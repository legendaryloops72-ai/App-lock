package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                val serviceIntent = Intent(context, AppLockUsageService::class.java)
                context.startService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
