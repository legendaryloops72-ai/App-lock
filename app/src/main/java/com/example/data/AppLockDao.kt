package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLockDao {
    @Query("SELECT * FROM protected_apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<ProtectedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: ProtectedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<ProtectedAppEntity>)

    @Update
    suspend fun updateApp(app: ProtectedAppEntity)

    @Query("SELECT * FROM security_settings WHERE id = 1")
    fun getSecuritySettings(): Flow<SecuritySettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SecuritySettingsEntity)

    @Query("SELECT * FROM intruder_logs ORDER BY timestamp DESC")
    fun getAllIntruderLogs(): Flow<List<IntruderLogEntity>>

    @Query("SELECT COUNT(*) FROM intruder_logs")
    fun getIntruderLogsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntruderLog(log: IntruderLogEntity)

    @Query("DELETE FROM intruder_logs WHERE id = :id")
    suspend fun deleteIntruderLog(id: Long)

    @Query("DELETE FROM intruder_logs")
    suspend fun clearIntruderLogs()
}
