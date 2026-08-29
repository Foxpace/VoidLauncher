package com.tomasrepcik.voidlauncher.home.data

import com.tomasrepcik.voidlauncher.launcher.AppKey
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.error.AppOperation
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherRepository
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherStorage
import com.tomasrepcik.voidlauncher.storage.launcher.readyLauncherState
import com.tomasrepcik.voidlauncher.storage.launcher.writeToStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class HomeAppsData(
    val apps: List<InstalledApp>,
    val keys: Set<AppKey>,
)

class HomeAppsRepository internal constructor(
    launcher: LauncherRepository,
    private val storage: LauncherStorage,
) {
    val data: Flow<HomeAppsData?> = launcher.readyLauncherState()
        .map { state ->
            state?.let { launcher ->
                HomeAppsData(
                    apps = launcher.pinnedHomeApps,
                    keys = launcher.pinnedAppKeys,
                )
            }
        }
        .distinctUntilChanged()

    suspend fun save(apps: List<AppKey>) = writeToStorage(AppOperation.SAVE_HOME_APPS) {
        storage.saveHomeApps(apps)
    }

    suspend fun add(app: AppKey) = writeToStorage(AppOperation.ADD_HOME_APP) {
        storage.addHomeApp(app)
    }

    suspend fun remove(app: AppKey) = writeToStorage(AppOperation.REMOVE_HOME_APP) {
        storage.removeHomeApp(app)
    }

    suspend fun reorder(fromIndex: Int, toIndex: Int) =
        writeToStorage(AppOperation.REORDER_HOME_APPS) {
            storage.reorderHomeApps(fromIndex, toIndex)
        }

    suspend fun rename(app: AppKey, label: String?) =
        writeToStorage(AppOperation.RENAME_HOME_APP) {
            storage.renameHomeApp(app, label)
        }
}
