package com.tomasrepcik.voidlauncher

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tomasrepcik.voidlauncher.data.local.LauncherDatabase
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.data.source.PackageManagerInstalledAppsDataSource
import com.tomasrepcik.voidlauncher.data.source.observeInstalledAppChanges
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch

class LauncherApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            applicationContext,
            LauncherDatabase::class.java,
            "void-launcher.db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

        appContainer = AppContainer(
            launcherRepository = LauncherRepository(
                database = database,
                installedAppsDataSource = PackageManagerInstalledAppsDataSource(
                    packageManager = applicationContext.packageManager,
                    launcherPackageName = packageName,
                    packageChanges = applicationContext.observeInstalledAppChanges(),
                ),
            ),
            installedAppSearch = InstalledAppSearch()
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `installed_apps` (
                `packageName` TEXT NOT NULL,
                `activityName` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `sortLabel` TEXT NOT NULL,
                PRIMARY KEY(`packageName`, `activityName`)
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `app_schedules` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `days` TEXT NOT NULL,
                `startMinute` INTEGER NOT NULL,
                `endMinute` INTEGER NOT NULL,
                `appKeys` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `app_schedules` ADD COLUMN `enabled` INTEGER NOT NULL DEFAULT 1"
        )
    }
}

data class AppContainer(
    val launcherRepository: LauncherRepository,
    val installedAppSearch: InstalledAppSearch,
)
