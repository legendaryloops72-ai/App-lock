package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

        // The bypass is valid only while the authenticated app remains foreground.
        // Clear it before handling the new package, including Launcher/System UI and App Lock.
        if (unlockedPackage != null && packageName != unlockedPackage) {
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

    override fun onInterrupt() {}
}
