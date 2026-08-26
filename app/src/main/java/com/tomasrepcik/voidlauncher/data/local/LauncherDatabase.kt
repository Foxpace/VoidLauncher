package com.tomasrepcik.voidlauncher.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PinnedAppEntity::class,
        ShortcutEntity::class,
        LauncherPreferencesEntity::class,
        InstalledAppEntity::class,
        AppScheduleEntity::class,
    ],
    version = LAUNCHER_DATABASE_VERSION,
    exportSchema = false,
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun pinnedAppDao(): PinnedAppDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun installedAppDao(): InstalledAppDao
    abstract fun appScheduleDao(): AppScheduleDao
}

internal fun openLauncherDatabase(context: Context): LauncherDatabase =
    Room.databaseBuilder(
        context.applicationContext,
        LauncherDatabase::class.java,
        LAUNCHER_DATABASE_NAME,
    )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

internal const val LAUNCHER_DATABASE_VERSION = 7
internal const val LAUNCHER_DATABASE_NAME = "void-launcher.db"
