package com.tomasrepcik.voidlauncher.data.repository

import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.domain.error.AppOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class HomeAppsData(
    val apps: List<InstalledApp>,
    val keys: Set<AppKey>,
)

class HomeAppsRepository internal constructor(
    launcher: LauncherRepository,
    private val storage: HomeAppsStorage = launcher.storage,
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
