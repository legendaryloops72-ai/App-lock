package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "protected_apps")
data class ProtectedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isLocked: Boolean,
    val category: String, // "Social", "Finance", "Media", "System"
    val iconResName: String = "ic_launcher_foreground"
)

@Entity(tableName = "security_settings")
data class SecuritySettingsEntity(
    @PrimaryKey val id: Int = 1,
    val pin: String = "1234",
    val lockType: String = "PIN", // "PIN" or "PATTERN"
    val patternSequence: String = "0,1,2,4,6", // comma-separated pattern indices 0-8
    val isBiometricEnabled: Boolean = true,
    val lockTimeout: String = "IMMEDIATELY", // "IMMEDIATELY", "1_MIN", "SCREEN_OFF"
    val fakeCrashEnabled: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val isDarkMode: Boolean = false,
    val wallpaperType: String = "DEFAULT", // "DEFAULT", "GRADIENT", "CUSTOM"
    val wallpaperValue: String = "",
    val uninstallProtectionEnabled: Boolean = false
)

@Entity(tableName = "intruder_logs")
data class IntruderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String,
    val photoPath: String? = null
)
