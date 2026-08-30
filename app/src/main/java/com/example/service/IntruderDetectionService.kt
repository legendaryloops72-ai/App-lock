package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppLockDatabase
import com.example.data.Camera2CaptureHelper
import com.example.data.IntruderLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class IntruderDetectionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val action = intent.action
        if (action == ACTION_RECORD_FAILED_ATTEMPT) {
            val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Protected App"
            val details = intent.getStringExtra(EXTRA_DETAILS) ?: "Failed unlock attempt"
            val shouldCapturePhoto = intent.getBooleanExtra(EXTRA_CAPTURE_PHOTO, true)

            performIntruderCapture(applicationContext, appName, details, shouldCapturePhoto) {
                stopSelf(startId)
            }
        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private fun performIntruderCapture(
        context: Context,
        appName: String,
        details: String,
        shouldCapturePhoto: Boolean,
        onComplete: () -> Unit
    ) {
        serviceScope.launch {
            try {
                if (shouldCapturePhoto) {
                    Camera2CaptureHelper.captureIntruderPhoto(context) { photoPath ->
                        serviceScope.launch {
                            val db = AppLockDatabase.getDatabase(context)
                            val log = IntruderLogEntity(
                                appName = appName,
                                timestamp = System.currentTimeMillis(),
                                details = details,
                                photoPath = photoPath
                            )
                            db.appLockDao().insertIntruderLog(log)
                            Log.d(TAG, "Intruder log recorded with photo: $photoPath")
                            showIntruderNotification(context, appName, photoPath != null)
                            onComplete()
                        }
                    }
                } else {
                    val db = AppLockDatabase.getDatabase(context)
                    val log = IntruderLogEntity(
                        appName = appName,
                        timestamp = System.currentTimeMillis(),
                        details = details,
                        photoPath = null
                    )
                    db.appLockDao().insertIntruderLog(log)
                    Log.d(TAG, "Intruder log recorded without photo: $details")
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error performing intruder capture", e)
                onComplete()
            }
        }
    }

    private fun showIntruderNotification(context: Context, appName: String, hasPhoto: Boolean) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channelId = "intruder_alerts_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "تنبيهات المتطفلين (Intruder Alerts)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيه عند محاولات الفتح الفاشلة والتقاط سيلفي للمتطفل"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "intruders")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val photoText = if (hasPhoto) "تم التقاط صورة للمتطفل بالكاميرا الأمامية 📸" else "تم تسجيل محاولة الدخول"
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("⚠️ تنبيه أمان: محاولة فتح فاشلة!")
                .setContentText("محاولة غير مصرح بها لفتح ($appName). $photoText")
                .setStyle(NotificationCompat.BigTextStyle().bigText("تم رصد محاولات فتح غير مصرح بها لتطبيق ($appName).\n$photoText\nاضغط هنا لعرض سجل المتطفلين ومعاينة الصور الملتقطة."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send intruder notification", e)
        }
    }

    companion object {
        private const val TAG = "IntruderDetectionService"
        const val NOTIFICATION_ID = 9921

        const val ACTION_RECORD_FAILED_ATTEMPT = "com.example.action.RECORD_FAILED_ATTEMPT"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_DETAILS = "extra_details"
        const val EXTRA_CAPTURE_PHOTO = "extra_capture_photo"

        /**
         * Trigger background capture of an intruder attempt.
         */
        fun recordFailedAttempt(
            context: Context,
            appName: String,
            details: String,
            capturePhoto: Boolean = true
        ) {
            try {
                val intent = Intent(context, IntruderDetectionService::class.java).apply {
                    action = ACTION_RECORD_FAILED_ATTEMPT
                    putExtra(EXTRA_APP_NAME, appName)
                    putExtra(EXTRA_DETAILS, details)
                    putExtra(EXTRA_CAPTURE_PHOTO, capturePhoto)
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start IntruderDetectionService, fallback to direct capture", e)
                // Fallback direct execution in IO scope
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (capturePhoto) {
                            Camera2CaptureHelper.captureIntruderPhoto(context) { path ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    val db = AppLockDatabase.getDatabase(context)
                                    db.appLockDao().insertIntruderLog(
                                        IntruderLogEntity(
                                            appName = appName,
                                            timestamp = System.currentTimeMillis(),
                                            details = details,
                                            photoPath = path
                                        )
                                    )
                                }
                            }
                        } else {
                            val db = AppLockDatabase.getDatabase(context)
                            db.appLockDao().insertIntruderLog(
                                IntruderLogEntity(
                                    appName = appName,
                                    timestamp = System.currentTimeMillis(),
                                    details = details,
                                    photoPath = null
                                )
                            )
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, "Fallback capture failed", ex)
                    }
                }
            }
        }
    }
}
