package com.tomasrepcik.voidlauncher

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tomasrepcik.voidlauncher.data.local.LauncherDatabase
import com.tomasrepcik.voidlauncher.data.repository.DefaultLauncherRepository
import com.tomasrepcik.voidlauncher.data.source.PackageManagerInstalledAppsDataSource
import com.tomasrepcik.voidlauncher.domain.search.SearchResolver

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
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

        appContainer = AppContainer(
            launcherRepository = DefaultLauncherRepository(
                database = database,
                installedAppsDataSource = PackageManagerInstalledAppsDataSource(applicationContext)
            ),
            searchResolver = SearchResolver()
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

data class AppContainer(
    val launcherRepository: DefaultLauncherRepository,
    val searchResolver: SearchResolver,
)
