package com.tomasrepcik.voidlauncher

import android.app.Application
import com.tomasrepcik.voidlauncher.data.local.openLauncherDatabase
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.data.source.PackageManagerInstalledAppsDataSource
import com.tomasrepcik.voidlauncher.data.source.observeInstalledAppChanges
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch

class LauncherApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = openLauncherDatabase(applicationContext)

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

data class AppContainer(
    val launcherRepository: LauncherRepository,
    val installedAppSearch: InstalledAppSearch,
)
