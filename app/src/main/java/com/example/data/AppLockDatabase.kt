package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ProtectedAppEntity::class, SecuritySettingsEntity::class, IntruderLogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppLockDatabase : RoomDatabase() {
    abstract fun appLockDao(): AppLockDao

    companion object {
        @Volatile
        private var INSTANCE: AppLockDatabase? = null

        fun getDatabase(context: Context): AppLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppLockDatabase::class.java,
                    "app_lock_database"
                )
                    .addCallback(AppLockDatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppLockDatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.appLockDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: AppLockDao) {
                // Insert default security settings
                dao.insertOrUpdateSettings(SecuritySettingsEntity(id = 1, pin = "1234", lockType = "PIN"))

                // Insert popular pre-populated apps
                val initialApps = listOf(
                    ProtectedAppEntity("com.whatsapp", "WhatsApp", true, "Social"),
                    ProtectedAppEntity("com.instagram.android", "Instagram", true, "Social"),
                    ProtectedAppEntity("com.google.android.youtube", "YouTube", false, "Media"),
                    ProtectedAppEntity("com.android.settings", "Settings", true, "System"),
                    ProtectedAppEntity("com.google.android.gm", "Gmail", true, "Finance"),
                    ProtectedAppEntity("com.google.android.apps.photos", "Google Photos", true, "Media"),
                    ProtectedAppEntity("com.sec.android.gallery3d", "Gallery", true, "Media"),
                    ProtectedAppEntity("com.bankofamerica.android", "Mobile Banking", true, "Finance"),
                    ProtectedAppEntity("com.facebook.katana", "Facebook", false, "Social"),
                    ProtectedAppEntity("com.twitter.android", "X (Twitter)", false, "Social"),
                    ProtectedAppEntity("com.google.android.apps.messaging", "Messages", false, "System"),
                    ProtectedAppEntity("com.spotify.music", "Spotify", false, "Media")
                )
                dao.insertApps(initialApps)
            }
        }
    }
}
