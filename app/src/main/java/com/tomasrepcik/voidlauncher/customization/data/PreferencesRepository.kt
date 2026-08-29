package com.tomasrepcik.voidlauncher.customization.data

import com.tomasrepcik.voidlauncher.launcher.error.AppOperation
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherRepository
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherStorage
import com.tomasrepcik.voidlauncher.storage.launcher.readyLauncherState
import com.tomasrepcik.voidlauncher.storage.launcher.writeToStorage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PreferencesRepository internal constructor(
    launcher: LauncherRepository,
    private val storage: LauncherStorage,
) {
    val preferences = launcher.readyLauncherState()
        .map { state -> state?.preferences }
        .distinctUntilChanged()

    suspend fun setHomeBackground(uri: String?) = writeToStorage(AppOperation.UPDATE_PREFERENCES) {
        storage.setHomeBackground(uri)
    }

    suspend fun setUseBackgroundColors(enabled: Boolean) =
        writeToStorage(AppOperation.UPDATE_PREFERENCES) {
            storage.setUseBackgroundColors(enabled)
        }

    suspend fun markNavigationTutorialSeen() = writeToStorage(AppOperation.UPDATE_PREFERENCES) {
        storage.markNavigationTutorialSeen()
    }
}
