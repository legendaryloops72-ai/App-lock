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

class AppLockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppLockRepository

    init {
        val dao = AppLockDatabase.getDatabase(application).appLockDao()
        repository = AppLockRepository(dao)
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

    // Interception / Lock Screen Simulation State
    private val _interceptedAppName = MutableStateFlow<String?>(null)
    val interceptedAppName: StateFlow<String?> = _interceptedAppName.asStateFlow()

    // Authentication result state for lock screen
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()
    private val _isSelfLocked = MutableStateFlow(true)
    val isSelfLocked: StateFlow<Boolean> = _isSelfLocked.asStateFlow()
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

    fun triggerAppLaunch(app: ProtectedAppEntity) {
        if (app.isLocked) {
            _interceptedAppName.value = app.appName
            _authError.value = null
        } else {
            // Unlocked app opens immediately
            _interceptedAppName.value = null
        }
    }

    fun dismissLockScreen() {
        _interceptedAppName.value = null
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
                _interceptedAppName.value = null
            } else {
                failedAttemptsCount++
                _authError.value = "Incorrect PIN. Attempt $failedAttemptsCount of 3."
                if (failedAttemptsCount >= 3) {
                    failedAttemptsCount = 0
                    com.example.data.Camera2CaptureHelper.captureIntruderPhoto(getApplication()) { photoPath ->
                        viewModelScope.launch {
                            repository.logIntruder(
                                IntruderLogEntity(
                                    appName = appName,
                                    timestamp = System.currentTimeMillis(),
                                    details = "3 failed PIN attempts. Intruder selfie captured.",
                                    photoPath = photoPath
                                )
                            )
                        }
                    }
                } else {
                    repository.logIntruder(
                        IntruderLogEntity(
                            appName = appName,
                            timestamp = System.currentTimeMillis(),
                            details = "Failed PIN attempt: $enteredPin"
                        )
                    )
                }
            }
        }
    }

    fun verifyPattern(enteredPattern: String, currentSettings: SecuritySettingsEntity, appName: String) {
        viewModelScope.launch {
            if (enteredPattern == currentSettings.patternSequence) {
                failedAttemptsCount = 0
                _authError.value = null
                _interceptedAppName.value = null
            } else {
                failedAttemptsCount++
                _authError.value = "Incorrect Pattern. Attempt $failedAttemptsCount of 3."
                if (failedAttemptsCount >= 3) {
                    failedAttemptsCount = 0
                    com.example.data.Camera2CaptureHelper.captureIntruderPhoto(getApplication()) { photoPath ->
                        viewModelScope.launch {
                            repository.logIntruder(
                                IntruderLogEntity(
                                    appName = appName,
                                    timestamp = System.currentTimeMillis(),
                                    details = "3 failed Pattern attempts. Intruder selfie captured.",
                                    photoPath = photoPath
                                )
                            )
                        }
                    }
                } else {
                    repository.logIntruder(
                        IntruderLogEntity(
                            appName = appName,
                            timestamp = System.currentTimeMillis(),
                            details = "Failed Pattern attempt"
                        )
                    )
                }
            }
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
