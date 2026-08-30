package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppLockDatabase
import com.example.data.AppLockRepository
import com.example.data.IntruderLogEntity
import com.example.data.ProtectedAppEntity
import com.example.data.SecuritySettingsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

class AppLockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppLockRepository

    init {
        val dao = AppLockDatabase.getDatabase(application).appLockDao()
        repository = AppLockRepository(dao)
        loadInstalledAppsFromDevice()
    }

    fun refreshInstalledApps() {
        loadInstalledAppsFromDevice()
    }

    private fun loadInstalledAppsFromDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val pm = context.packageManager
                val ourPackageName = context.packageName

                // 1. Query all launcher apps
                val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(launcherIntent, 0)
                }

                // 2. Query all installed applications
                val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledApplications(0)
                }

                val existingApps = repository.getExistingApps().associateBy { it.packageName }
                val discoveredPackages = mutableMapOf<String, String>() // packageName -> appName

                // Process launcher activities
                for (info in resolveInfos) {
                    val pkg = info.activityInfo.packageName
                    if (pkg == ourPackageName) continue
                    val name = info.loadLabel(pm).toString()
                    if (name.isNotBlank()) {
                        discoveredPackages[pkg] = name
                    }
                }

                // Process installed applications (user apps, launchable apps, settings, system tools)
                for (appInfo in installedApps) {
                    val pkg = appInfo.packageName
                    if (pkg == ourPackageName) continue
                    if (discoveredPackages.containsKey(pkg)) continue

                    val isUserApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    val isLaunchable = pm.getLaunchIntentForPackage(pkg) != null
                    val isEssentialSystemApp = pkg == "com.android.settings" ||
                            pkg == "com.android.vending" ||
                            pkg.contains("camera") ||
                            pkg.contains("gallery") ||
                            pkg.contains("browser") ||
                            pkg.contains("chrome") ||
                            pkg.contains("calculator") ||
                            pkg.contains("contacts") ||
                            pkg.contains("dialer") ||
                            pkg.contains("mms") ||
                            pkg.contains("deskclock")

                    if (isUserApp || isLaunchable || isEssentialSystemApp) {
                        val name = try {
                            appInfo.loadLabel(pm).toString()
                        } catch (e: Exception) {
                            pkg
                        }
                        if (name.isNotBlank()) {
                            discoveredPackages[pkg] = name
                        }
                    }
                }

                val newAppsToInsert = mutableListOf<ProtectedAppEntity>()
                for ((pkg, name) in discoveredPackages) {
                    if (!existingApps.containsKey(pkg)) {
                        val category = when {
                            pkg.contains("whatsapp") || pkg.contains("instagram") || pkg.contains("facebook") || pkg.contains("telegram") || pkg.contains("twitter") || pkg.contains("snapchat") || pkg.contains("tiktok") || pkg.contains("social") || pkg.contains("messenger") -> "Social"
                            pkg.contains("bank") || pkg.contains("pay") || pkg.contains("wallet") || pkg.contains("finance") || pkg.contains("money") || pkg.contains("crypto") -> "Finance"
                            pkg.contains("youtube") || pkg.contains("photo") || pkg.contains("gallery") || pkg.contains("music") || pkg.contains("video") || pkg.contains("netflix") || pkg.contains("spotify") || pkg.contains("media") -> "Media"
                            else -> "System"
                        }
                        newAppsToInsert.add(
                            ProtectedAppEntity(
                                packageName = pkg,
                                appName = name,
                                isLocked = false,
                                category = category
                            )
                        )
                    }
                }

                if (newAppsToInsert.isNotEmpty()) {
                    repository.insertNewApps(newAppsToInsert)
                }
            } catch (e: Exception) {
                // Fallback gracefully
            }
        }
    }

    val apps: StateFlow<List<ProtectedAppEntity>> = repository.allApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<SecuritySettingsEntity?> = repository.securitySettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val intruderLogs: StateFlow<List<IntruderLogEntity>> = repository.intruderLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Navigation trigger from notifications or external intents
    private val _pendingNavigation = MutableStateFlow<String?>(null)
    val pendingNavigation: StateFlow<String?> = _pendingNavigation.asStateFlow()

    fun setPendingNavigation(route: String?) {
        _pendingNavigation.value = route
    }

    fun clearPendingNavigation() {
        _pendingNavigation.value = null
    }

    fun checkAndNotifyUnseenIntruders(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentLogs = intruderLogs.value
                if (currentLogs.isNotEmpty()) {
                    val prefs = context.getSharedPreferences("intruder_prefs", android.content.Context.MODE_PRIVATE)
                    val lastNotifiedId = prefs.getLong("last_notified_intruder_id", 0L)
                    val lastSeenId = prefs.getLong("last_seen_intruder_id", 0L)
                    val unnotifiedLogs = currentLogs.filter { it.id > lastNotifiedId && it.id > lastSeenId }
                    val latestLog = unnotifiedLogs.maxByOrNull { it.id }
                    if (latestLog != null) {
                        com.example.service.IntruderDetectionService.showIntruderNotification(
                            context = context,
                            appName = latestLog.appName,
                            hasPhoto = latestLog.photoPath != null,
                            logId = latestLog.id
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore gracefully
            }
        }
    }

    // Interception / Lock Screen Simulation State
    private val _interceptedPackageName = MutableStateFlow<String?>(null)
    val interceptedPackageName: StateFlow<String?> = _interceptedPackageName.asStateFlow()

    private val _interceptedAppName = MutableStateFlow<String?>(null)
    val interceptedAppName: StateFlow<String?> = _interceptedAppName.asStateFlow()

    private val _unlockSuccess = MutableStateFlow(false)
    val unlockSuccess: StateFlow<Boolean> = _unlockSuccess.asStateFlow()

    // Authentication result state for lock screen
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()
    private val _isSelfLocked = MutableStateFlow(true)
    val isSelfLocked: StateFlow<Boolean> = _isSelfLocked.asStateFlow()
    private var ignoreNextLock = false
    fun ignoreNextSelfLock() { ignoreNextLock = true }
    fun checkAndRequireSelfAuth(timeoutMs: Long, backgroundTime: Long) {
        if (ignoreNextLock) {
            ignoreNextLock = false
            return
        }
        if (backgroundTime > 0 && System.currentTimeMillis() - backgroundTime > timeoutMs) {
            _isSelfLocked.value = true
        }
    }
    fun unlockSelf() { _isSelfLocked.value = false }
    fun requireSelfAuth() { _isSelfLocked.value = true }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleAppLock(app: ProtectedAppEntity) {
        viewModelScope.launch {
            repository.updateApp(app.copy(isLocked = !app.isLocked))
        }
    }

    fun triggerIntercept(packageName: String, appName: String) {
        _interceptedPackageName.value = packageName
        _interceptedAppName.value = appName
        _unlockSuccess.value = false
        _authError.value = null
    }

    fun triggerAppLaunch(app: ProtectedAppEntity) {
        if (app.isLocked) {
            _interceptedPackageName.value = app.packageName
            _interceptedAppName.value = app.appName
            _unlockSuccess.value = false
            _authError.value = null
        } else {
            // Unlocked app opens immediately
            _interceptedPackageName.value = null
            _interceptedAppName.value = null
            _unlockSuccess.value = false
        }
    }

    fun dismissLockScreen() {
        _interceptedPackageName.value = null
        _interceptedAppName.value = null
        _unlockSuccess.value = false
        _authError.value = null
    }

    fun onBiometricSuccess() {
        _unlockSuccess.value = true
    }

    fun unlockSuccessful() {
        _interceptedPackageName.value?.let { pkg ->
            com.example.service.AppLockAccessibilityService.unlockedPackage = pkg
        }
        _interceptedPackageName.value = null
        _interceptedAppName.value = null
        _unlockSuccess.value = false
        _authError.value = null
    }

    fun setAuthError(error: String?) {
        _authError.value = error
    }

    private var failedAttemptsCount = 0

    fun verifyPin(enteredPin: String, currentSettings: SecuritySettingsEntity, appName: String) {
        viewModelScope.launch {
            if (enteredPin == currentSettings.pin) {
                // Success!
                failedAttemptsCount = 0
                _authError.value = null
                _unlockSuccess.value = true
            } else {
                failedAttemptsCount++
                _authError.value = "رمز خاطئ. المحاولة $failedAttemptsCount من 3"
                val shouldCapture = failedAttemptsCount >= 1 // Capture photo on failed attempt
                val details = if (failedAttemptsCount >= 3) {
                    "3 محاولات PIN خاطئة متتالية. تم التقاط سيلفي المتطفل."
                } else {
                    "محاولة PIN خاطئة ($enteredPin) - محاولة $failedAttemptsCount"
                }

                com.example.service.IntruderDetectionService.recordFailedAttempt(
                    context = getApplication(),
                    appName = appName,
                    details = details,
                    capturePhoto = shouldCapture
                )

                if (failedAttemptsCount >= 3) {
                    failedAttemptsCount = 0
                }
            }
        }
    }

    fun verifyPattern(enteredPattern: String, currentSettings: SecuritySettingsEntity, appName: String) {
        viewModelScope.launch {
            if (enteredPattern == currentSettings.patternSequence) {
                failedAttemptsCount = 0
                _authError.value = null
                _unlockSuccess.value = true
            } else {
                failedAttemptsCount++
                _authError.value = "نمط خاطئ. المحاولة $failedAttemptsCount من 3"
                val shouldCapture = failedAttemptsCount >= 1
                val details = if (failedAttemptsCount >= 3) {
                    "3 محاولات نمط خاطئة متتالية. تم التقاط سيلفي المتطفل."
                } else {
                    "محاولة رسم نمط خاطئة - محاولة $failedAttemptsCount"
                }

                com.example.service.IntruderDetectionService.recordFailedAttempt(
                    context = getApplication(),
                    appName = appName,
                    details = details,
                    capturePhoto = shouldCapture
                )

                if (failedAttemptsCount >= 3) {
                    failedAttemptsCount = 0
                }
            }
        }
    }

    fun testCaptureIntruderSelfie(appName: String = "تجربة الأمان") {
        viewModelScope.launch {
            com.example.service.IntruderDetectionService.recordFailedAttempt(
                context = getApplication(),
                appName = appName,
                details = "تجربة التقاط سيلفي المتطفل من المعرض 📸",
                capturePhoto = true
            )
        }
    }

    fun deleteIntruderLog(id: Long) {
        viewModelScope.launch {
            repository.deleteLog(id)
        }
    }

    fun updateSettings(newSettings: SecuritySettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
        }
    }

    fun clearIntruderLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun addNewApp(packageName: String, appName: String, category: String) {
        viewModelScope.launch {
            repository.insertApp(
                ProtectedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    isLocked = true,
                    category = category
                )
            )
        }
    }
}
