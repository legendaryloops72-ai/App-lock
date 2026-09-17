package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.data.AppLockDatabase
import com.example.data.ProtectedAppEntity
import com.example.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class AppLockUsageService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile
    private var lockedApps: List<ProtectedAppEntity> = emptyList()

    companion object {
        private const val CHANNEL_ID = "app_lock_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 500L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // Keep a lightweight in-memory snapshot instead of querying Room on every poll.
        serviceScope.launch {
            AppLockDatabase.getDatabase(applicationContext)
                .appLockDao()
                .getAllApps()
                .collectLatest { apps ->
                    lockedApps = apps.filter { it.isLocked }
                }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (job == null || job?.isActive != true) {
            job = serviceScope.launch {
                while (isActive) {
                    if (AppLockAccessibilityService.isServiceRunning) {
                        // AccessibilityService is the primary monitor. UsageStats is only a fallback.
                        delay(POLL_INTERVAL_MS * 6)
                        continue
                    }
                    checkForegroundApp()
                    delay(POLL_INTERVAL_MS)
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
                val recentPackage = stats.maxByOrNull { it.lastTimeUsed }?.packageName
                if (recentPackage != null && recentPackage != packageName) {
                    // An unlock is a foreground-session grant, not a permanent bypass.
                    // Clear it as soon as Home, System UI, or another app becomes current.
                    if (recentPackage != AppLockAccessibilityService.unlockedPackage) {
                        AppLockAccessibilityService.unlockedPackage = null
                    } else {
                        return
                    }

                    val app = lockedApps.firstOrNull {
                        it.packageName == recentPackage &&
                            it.packageName != "com.android.settings" &&
                            it.packageName != "com.android.permissioncontroller"
                    }
                    if (app != null && recentPackage != AppLockAccessibilityService.interceptedPackage) {
                        val intent = Intent(applicationContext, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra("INTERCEPT_PACKAGE", app.packageName)
                            putExtra("INTERCEPT_NAME", app.appName)
                        }
                        AppLockAccessibilityService.interceptedPackage = recentPackage
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Lock protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps App Lock protection available after device restart"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("App Lock protection")
            .setContentText("App protection is active")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        job?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
