package com.tomasrepcik.voidlauncher.storage.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tomasrepcik.voidlauncher.appcatalog.data.InstalledAppDao
import com.tomasrepcik.voidlauncher.appcatalog.data.InstalledAppEntity
import com.tomasrepcik.voidlauncher.customization.data.LauncherPreferencesEntity
import com.tomasrepcik.voidlauncher.customization.data.PreferencesDao
import com.tomasrepcik.voidlauncher.home.data.PinnedAppDao
import com.tomasrepcik.voidlauncher.home.data.PinnedAppEntity
import com.tomasrepcik.voidlauncher.schedule.data.AppScheduleDao
import com.tomasrepcik.voidlauncher.schedule.data.AppScheduleEntity
import com.tomasrepcik.voidlauncher.shortcuts.data.ShortcutDao
import com.tomasrepcik.voidlauncher.shortcuts.data.ShortcutEntity

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
