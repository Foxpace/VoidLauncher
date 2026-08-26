package com.tomasrepcik.voidlauncher.data.repository

import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import kotlinx.coroutines.flow.Flow

internal interface InstalledAppsStorage {
    suspend fun replaceInstalledApps(apps: List<InstalledApp>)
}

internal interface HomeAppsStorage {
    suspend fun saveHomeApps(apps: List<AppKey>)
    suspend fun addHomeApp(appKey: AppKey)
    suspend fun removeHomeApp(appKey: AppKey)
    suspend fun reorderHomeApps(fromIndex: Int, toIndex: Int)
    suspend fun renameHomeApp(appKey: AppKey, newLabel: String?)
}

internal interface ShortcutStorage {
    suspend fun saveShortcut(slot: ShortcutSlot, selection: ShortcutSelection)
}

internal interface PreferencesStorage {
    suspend fun setHomeBackground(uri: String?)
    suspend fun setUseBackgroundColors(enabled: Boolean)
    suspend fun markNavigationTutorialSeen()
}

internal interface ScheduleStorage {
    suspend fun saveSchedule(schedule: AppSchedule)
    suspend fun deleteSchedule(id: String)
}

internal interface LauncherStorage :
    InstalledAppsStorage,
    HomeAppsStorage,
    ShortcutStorage,
    PreferencesStorage,
    ScheduleStorage {
    val snapshots: Flow<LauncherStorageSnapshot>

    suspend fun initialize()
}
