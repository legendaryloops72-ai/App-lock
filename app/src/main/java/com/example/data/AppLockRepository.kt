package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class AppLockRepository(private val dao: AppLockDao) {
    val allApps: Flow<List<ProtectedAppEntity>> = dao.getAllApps()
    val securitySettings: Flow<SecuritySettingsEntity?> = dao.getSecuritySettings()
    val intruderLogs: Flow<List<IntruderLogEntity>> = dao.getAllIntruderLogs()

    suspend fun updateApp(app: ProtectedAppEntity) {
        dao.updateApp(app)
    }

    suspend fun insertApp(app: ProtectedAppEntity) {
        dao.insertApp(app)
    }

    suspend fun saveSettings(settings: SecuritySettingsEntity) {
        dao.insertOrUpdateSettings(settings)
    }

    suspend fun logIntruder(log: IntruderLogEntity) {
        dao.insertIntruderLog(log)
    }

    suspend fun clearLogs() {
        dao.clearIntruderLogs()
    }
}
