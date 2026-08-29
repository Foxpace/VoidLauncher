package com.tomasrepcik.voidlauncher.storage.database

import com.tomasrepcik.voidlauncher.launcher.AppKey
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.LauncherPreferences
import com.tomasrepcik.voidlauncher.launcher.ShortcutSelection
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot
import com.tomasrepcik.voidlauncher.schedule.data.AppSchedule

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
