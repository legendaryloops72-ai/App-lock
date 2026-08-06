package com.example.service

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.data.AppLockDatabase
import com.example.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppLockUsageService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (job == null || job?.isActive != true) {
            job = serviceScope.launch {
                while (isActive) {
                    if (AppLockAccessibilityService.isServiceRunning) {
                        delay(3000)
                        continue
                    }
                    checkForegroundApp()
                    delay(150)
                }
            }
        }
        return START_STICKY
    }

    private suspend fun checkForegroundApp() {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
            val currentTime = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 10000,
                currentTime
            )
            if (!stats.isNullOrEmpty()) {
                val sortedStats = stats.sortedBy { it.lastTimeUsed }
                val recentPackage = sortedStats.lastOrNull()?.packageName
                if (recentPackage != null && recentPackage != packageName && recentPackage != AppLockAccessibilityService.unlockedPackage) {
                    val db = AppLockDatabase.getDatabase(applicationContext)
                    val app = db.appLockDao().getAllApps().first().find { it.packageName == recentPackage }
                    if (app != null && app.isLocked) {
                        val intent = Intent(applicationContext, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra("INTERCEPT_PACKAGE", app.packageName)
                            putExtra("INTERCEPT_NAME", app.appName)
                        }
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }
}
