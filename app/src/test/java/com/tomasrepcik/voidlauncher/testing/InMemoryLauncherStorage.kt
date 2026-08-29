package com.tomasrepcik.voidlauncher.testing

import android.database.sqlite.SQLiteException
import com.tomasrepcik.voidlauncher.launcher.AppKey
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.LauncherPreferences
import com.tomasrepcik.voidlauncher.launcher.ShortcutSelection
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot
import com.tomasrepcik.voidlauncher.launcher.withBackgroundColorsEnabled
import com.tomasrepcik.voidlauncher.launcher.withHomeBackground
import com.tomasrepcik.voidlauncher.launcher.withNavigationTutorialSeen
import com.tomasrepcik.voidlauncher.schedule.data.AppSchedule
import com.tomasrepcik.voidlauncher.storage.database.LauncherStorageSnapshot
import com.tomasrepcik.voidlauncher.storage.database.StoredPinnedApp
import com.tomasrepcik.voidlauncher.storage.database.StoredShortcut
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherStorage
import com.tomasrepcik.voidlauncher.storage.launcher.asStorageAccessException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Suppress("TooManyFunctions") // The fake mirrors the complete aggregate storage behavior.
internal class InMemoryLauncherStorage(
    initialSnapshot: LauncherStorageSnapshot = LauncherStorageSnapshot(),
    private var initializationFailuresRemaining: Int = 0,
    private var writeFailuresRemaining: Int = 0,
) : LauncherStorage {
    private val state = MutableStateFlow(initialSnapshot)
    override val snapshots: Flow<LauncherStorageSnapshot> = state

    override suspend fun initialize() {
        if (initializationFailuresRemaining > 0) {
            initializationFailuresRemaining--
            throw SQLiteException("planned in-memory initialization failure")
                .asStorageAccessException()
        }
        state.update { snapshot ->
            snapshot.copy(
                shortcuts = snapshot.shortcuts.ifEmpty {
                    listOf(
                        StoredShortcut(ShortcutSlot.LEFT, ShortcutSelection.SystemContacts),
                        StoredShortcut(ShortcutSlot.RIGHT, ShortcutSelection.SystemCamera),
                    )
                },
            )
        }
    }

    override suspend fun replaceInstalledApps(apps: List<InstalledApp>) {
        state.update { it.copy(installedApps = apps) }
    }

    override suspend fun saveHomeApps(apps: List<AppKey>) = write { snapshot ->
        snapshot.copy(pinnedApps = apps.distinct().map(::StoredPinnedApp))
    }

    override suspend fun addHomeApp(appKey: AppKey) = write { snapshot ->
        if (snapshot.pinnedApps.any { it.key == appKey }) {
            snapshot
        } else {
            snapshot.copy(pinnedApps = snapshot.pinnedApps + StoredPinnedApp(appKey))
        }
    }

    override suspend fun removeHomeApp(appKey: AppKey) = write { snapshot ->
        snapshot.copy(pinnedApps = snapshot.pinnedApps.filterNot { pinned -> pinned.key == appKey })
    }

    override suspend fun reorderHomeApps(fromIndex: Int, toIndex: Int) = write { snapshot ->
        val apps = snapshot.pinnedApps.toMutableList()
        if (fromIndex in apps.indices && toIndex in apps.indices) {
            apps.add(toIndex, apps.removeAt(fromIndex))
        }
        snapshot.copy(pinnedApps = apps)
    }

    override suspend fun renameHomeApp(appKey: AppKey, newLabel: String?) = write { snapshot ->
        snapshot.copy(
            pinnedApps = snapshot.pinnedApps.map { pinned ->
                if (pinned.key == appKey) pinned.copy(labelOverride = newLabel) else pinned
            },
        )
    }

    override suspend fun saveShortcut(slot: ShortcutSlot, selection: ShortcutSelection) =
        write { snapshot ->
            snapshot.copy(
                shortcuts = snapshot.shortcuts.filterNot { it.slot == slot } +
                    StoredShortcut(slot, selection),
            )
        }

    override suspend fun setHomeBackground(uri: String?) = updatePreferences { current ->
        current.withHomeBackground(uri)
    }

    override suspend fun setUseBackgroundColors(enabled: Boolean) = updatePreferences { current ->
        current.withBackgroundColorsEnabled(enabled)
    }

    override suspend fun markNavigationTutorialSeen() = updatePreferences { current ->
        current.withNavigationTutorialSeen()
    }

    override suspend fun saveSchedule(schedule: AppSchedule) = write { snapshot ->
        snapshot.copy(
            schedules = (snapshot.schedules.filterNot { it.id == schedule.id } + schedule)
                .sortedBy { it.name.lowercase() },
        )
    }

    override suspend fun deleteSchedule(id: String) = write { snapshot ->
        snapshot.copy(schedules = snapshot.schedules.filterNot { it.id == id })
    }

    private fun updatePreferences(update: (LauncherPreferences) -> LauncherPreferences) =
        write { snapshot -> snapshot.copy(preferences = update(snapshot.preferences)) }

    private fun write(transform: (LauncherStorageSnapshot) -> LauncherStorageSnapshot) {
        if (writeFailuresRemaining > 0) {
            writeFailuresRemaining--
            throw SQLiteException("planned in-memory write failure")
                .asStorageAccessException()
        }
        state.update(transform)
    }
}
