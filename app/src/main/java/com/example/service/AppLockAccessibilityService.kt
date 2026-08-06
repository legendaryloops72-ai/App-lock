package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.AppLockDatabase
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Stack

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    companion object {
        var isServiceRunning = false
        var unlockedPackage: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceRunning = false
        return super.onUnbind(intent)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        // Clear unlocked package bypass immediately if leaving Settings or Package Installers
        if (packageName != "com.android.settings" && 
            !packageName.contains("packageinstaller") && 
            unlockedPackage != null) {
            unlockedPackage = null
        }

        // Do not intercept or lock our own app
        if (packageName == applicationContext.packageName) return

        if (packageName == unlockedPackage) return

        serviceScope.launch {
            try {
                val db = AppLockDatabase.getDatabase(applicationContext)
                val settings = db.appLockDao().getSecuritySettings().first()
                val isUninstallProtectionEnabled = settings?.uninstallProtectionEnabled ?: false

                if (isUninstallProtectionEnabled && 
                    (packageName == "com.android.settings" || packageName.contains("packageinstaller"))) {
                    
                    val rootNode = rootInActiveWindow
                    if (isAppInfoOrUninstallOfOurApp(rootNode)) {
                        val intent = Intent(applicationContext, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra("INTERCEPT_PACKAGE", packageName)
                            putExtra("INTERCEPT_NAME", "إعدادات الأمان (AppLock)")
                        }
                        startActivity(intent)
                        return@launch
                    }
                }

                // Standard App Locking
                val app = db.appLockDao().getAllApps().first().find { it.packageName == packageName }
                if (app != null && app.isLocked) {
                    val intent = Intent(applicationContext, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("INTERCEPT_PACKAGE", app.packageName)
                        putExtra("INTERCEPT_NAME", app.appName)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isAppInfoOrUninstallOfOurApp(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val nodes = Stack<AccessibilityNodeInfo>()
        nodes.push(root)
        
        var containsOurAppRef = false
        var containsUninstallOrForceStop = false
        val ourPackage = applicationContext.packageName ?: "com.example"
        
        while (nodes.isNotEmpty()) {
            val node = nodes.pop() ?: continue
            val text = node.text?.toString()?.lowercase() ?: ""
            
            // Check if node contains our package or app name references
            if (text.contains(ourPackage) || 
                text.contains("applock") ||
                text.contains("قفل التطبيقات") || 
                text.contains("حاسبة آمنة") || 
                text.contains("طقس اليوم") ||
                text.contains("متصفح الإنترنت") ||
                text.contains("my application")) {
                containsOurAppRef = true
            }
            
            // Check for uninstall or force-stop keywords across multiple languages
            if (text.contains("uninstall") || 
                text.contains("force stop") || 
                text.contains("إلغاء التثبيت") || 
                text.contains("إيقاف إجباري") || 
                text.contains("إيقاف فرض") || 
                text.contains("فرض الإيقاف") ||
                text.contains("desinstalar") ||
                text.contains("forzar detención")) {
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
