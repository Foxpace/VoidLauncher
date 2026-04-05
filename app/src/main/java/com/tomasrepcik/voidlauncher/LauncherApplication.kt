package com.tomasrepcik.voidlauncher

import android.app.Application
import androidx.room.Room
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
        ).fallbackToDestructiveMigration(dropAllTables = true).build()

        appContainer = AppContainer(
            launcherRepository = DefaultLauncherRepository(
                database = database,
                installedAppsDataSource = PackageManagerInstalledAppsDataSource(applicationContext)
            ),
            searchResolver = SearchResolver()
        )
    }
}

data class AppContainer(
    val launcherRepository: DefaultLauncherRepository,
    val searchResolver: SearchResolver,
)
