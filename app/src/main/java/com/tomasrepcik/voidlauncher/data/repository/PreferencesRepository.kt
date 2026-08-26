package com.tomasrepcik.voidlauncher.data.repository

import com.tomasrepcik.voidlauncher.domain.error.AppOperation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PreferencesRepository internal constructor(
    launcher: LauncherRepository,
    private val storage: PreferencesStorage,
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
