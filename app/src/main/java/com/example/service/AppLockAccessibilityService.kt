package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import com.example.data.AppLockDatabase
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Stack

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cacheJob: Job? = null

    @Volatile
    private var lockedApps: Map<String, String> = emptyMap()

    @Volatile
    private var isUninstallProtectionEnabled = false

    companion object {
        var isServiceRunning = false
        var unlockedPackage: String? = null
        @Volatile
        var interceptedPackage: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        startCacheObservers()
    }

    override fun onDestroy() {
        cacheJob?.cancel()
        serviceScope.cancel()
        lockedApps = emptyMap()
        isUninstallProtectionEnabled = false
        isServiceRunning = false
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceRunning = false
        return super.onUnbind(intent)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        if (cacheJob?.isActive != true) {
            startCacheObservers()
        }
    }

    private fun startCacheObservers() {
        if (cacheJob?.isActive == true) return

        cacheJob = serviceScope.launch {
            val db = AppLockDatabase.getDatabase(applicationContext)
            launch {
                db.appLockDao().getAllApps().collectLatest { apps ->
                    lockedApps = apps.asSequence()
                        .filter { it.isLocked }
                        .associate { it.packageName to it.appName }
                }
            }
            launch {
                db.appLockDao().getSecuritySettings().collectLatest { settings ->
                    isUninstallProtectionEnabled = settings?.uninstallProtectionEnabled ?: false
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        // LockScreen runs inside App Lock. Its own accessibility events must not
        // clear the temporary grant for the protected app being opened.
        if (packageName == applicationContext.packageName) return

        if (interceptedPackage != null && packageName != interceptedPackage) {
            interceptedPackage = null
        }

        // Accessibility can emit many events for one foreground transition.
        // Do not relaunch MainActivity while this package is already awaiting auth.
        if (packageName == interceptedPackage) return

        // The bypass is valid while the authenticated app remains foreground.
        // Transient window transitions (Launcher, SystemUI, IME keyboards) must not clear
        // unlockedPackage, preventing re-lock loops when returning to the protected app.
        if (unlockedPackage != null && packageName != unlockedPackage && !isTransientPackage(packageName)) {
            unlockedPackage = null
        }

        // Do not intercept or lock our own app. Opening App Lock and granting permissions
        // must never require the protected-app PIN/Pattern.
        // The authenticated app remains usable for this foreground session only.
        if (packageName == unlockedPackage) return

        // Use in-memory snapshots for the hot accessibility-event path.
        // Room is observed above and only updates these snapshots when data changes.
        if (isUninstallProtectionEnabled && isUninstallOrForceStopScreen(packageName)) {
            val rootNode = rootInActiveWindow
            if (isAppInfoOrUninstallOfOurApp(rootNode)) {
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("INTERCEPT_PACKAGE", packageName)
                    putExtra("INTERCEPT_NAME", "إعدادات الأمان (AppLock)")
                }
                startActivity(intent)
                return
            }
        }

        // Android Settings hosts the permission pages launched from App Lock.
        // It must never be treated as a user-protected app, otherwise tapping
        // Accessibility/Usage/Overlay permission opens LockScreen instead.
        if (packageName == "com.android.settings" ||
            packageName == "com.android.permissioncontroller") return

        val appName = lockedApps[packageName] ?: return
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("INTERCEPT_PACKAGE", packageName)
            putExtra("INTERCEPT_NAME", appName)
        }
        interceptedPackage = packageName
        startActivity(intent)
    }

    private fun isUninstallOrForceStopScreen(packageName: String): Boolean {
        return packageName == "com.android.settings" ||
            packageName.contains("packageinstaller") ||
            packageName.contains("package.installer") ||
            packageName.contains("permissioncontroller") ||
            packageName.contains("packageinstaller")
    }

    private fun isAppInfoOrUninstallOfOurApp(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val nodes = Stack<AccessibilityNodeInfo>()
        nodes.push(root)

        var containsOurAppRef = false
        var containsUninstallOrForceStop = false
        val ourPackage = applicationContext.packageName
        val ourLabels = setOf(
            "applock",
            "قفل التطبيقات",
            "حاسبة آمنة",
            "طقس اليوم",
            "متصفح الإنترنت",
            "my application"
        )
        val actionKeywords = setOf(
            "uninstall",
            "uninstall app",
            "force stop",
            "remove app",
            "إلغاء التثبيت",
            "إزالة التطبيق",
            "إيقاف إجباري",
            "إيقاف فرض",
            "فرض الإيقاف",
            "desinstalar",
            "forzar detención",
            "désinstaller",
            "arrêter de force",
            "deinstallieren",
            "beenden erzwingen"
        )

        while (nodes.isNotEmpty()) {
            val node = nodes.pop() ?: continue
            val text = (node.text?.toString() ?: "").trim().lowercase()
            val contentDescription = (node.contentDescription?.toString() ?: "").trim().lowercase()
            val combinedText = "$text $contentDescription"

            if (combinedText.contains(ourPackage.lowercase()) ||
                ourLabels.any { combinedText.contains(it) }) {
                containsOurAppRef = true
            }

            if (actionKeywords.any { combinedText.contains(it) }) {
                containsUninstallOrForceStop = true
            }

            if (containsOurAppRef && containsUninstallOrForceStop) {
                return true
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    nodes.push(child)
                }
            }
        }
        return false
    }

    private var cachedLauncherPackages: Set<String> = emptySet()
    private var lastLauncherCheckTime = 0L

    private var cachedImePackages: Set<String> = emptySet()
    private var lastImeCheckTime = 0L

    private fun isLauncherPackage(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastLauncherCheckTime > 10_000L || cachedLauncherPackages.isEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val homeList = mutableSetOf<String>()
                packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.let {
                    homeList.add(it)
                }
                val activities = packageManager.queryIntentActivities(intent, 0)
                for (resolveInfo in activities) {
                    resolveInfo.activityInfo?.packageName?.let { homeList.add(it) }
                }
                cachedLauncherPackages = homeList
                lastLauncherCheckTime = now
            } catch (_: Exception) {
            }
        }
        return cachedLauncherPackages.contains(packageName)
    }

    private fun isImePackage(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastImeCheckTime > 10_000L || cachedImePackages.isEmpty()) {
            try {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                cachedImePackages = imm?.enabledInputMethodList?.mapNotNull { it.packageName }?.toSet() ?: emptySet()
                lastImeCheckTime = now
            } catch (_: Exception) {
            }
        }
        return cachedImePackages.contains(packageName)
    }

    private fun isTransientPackage(packageName: String): Boolean {
        if (packageName == "android") return true
        if (packageName == "com.android.systemui") return true
        if (isLauncherPackage(packageName)) return true
        if (isImePackage(packageName)) return true
        return false
    }

    override fun onInterrupt() {}
}
