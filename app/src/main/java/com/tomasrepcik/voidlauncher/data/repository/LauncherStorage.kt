package com.tomasrepcik.voidlauncher.data.repository

import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions") // One aggregate contract is clearer than one-child storage interfaces.
internal interface LauncherStorage {
    val snapshots: Flow<LauncherStorageSnapshot>

    suspend fun initialize()
    suspend fun replaceInstalledApps(apps: List<InstalledApp>)
    suspend fun saveHomeApps(apps: List<AppKey>)
    suspend fun addHomeApp(appKey: AppKey)
    suspend fun removeHomeApp(appKey: AppKey)
    suspend fun reorderHomeApps(fromIndex: Int, toIndex: Int)
    suspend fun renameHomeApp(appKey: AppKey, newLabel: String?)
    suspend fun saveShortcut(slot: ShortcutSlot, selection: ShortcutSelection)
    suspend fun setHomeBackground(uri: String?)
    suspend fun setUseBackgroundColors(enabled: Boolean)
    suspend fun markNavigationTutorialSeen()
    suspend fun saveSchedule(schedule: AppSchedule)
    suspend fun deleteSchedule(id: String)
}
