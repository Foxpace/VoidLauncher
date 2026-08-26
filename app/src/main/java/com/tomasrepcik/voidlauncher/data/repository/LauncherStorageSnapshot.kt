package com.tomasrepcik.voidlauncher.data.repository

import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferences
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule

internal data class StoredPinnedApp(
    val key: AppKey,
    val labelOverride: String? = null,
)

internal data class StoredShortcut(
    val slot: ShortcutSlot,
    val selection: ShortcutSelection,
    val customLabel: String? = null,
)

internal data class LauncherStorageSnapshot(
    val installedApps: List<InstalledApp> = emptyList(),
    val pinnedApps: List<StoredPinnedApp> = emptyList(),
    val shortcuts: List<StoredShortcut> = emptyList(),
    val preferences: LauncherPreferences = LauncherPreferences(),
    val schedules: List<AppSchedule> = emptyList(),
)
