package com.tomasrepcik.voidlauncher.data.repository

import androidx.room.withTransaction
import com.tomasrepcik.voidlauncher.data.local.InstalledAppEntity
import com.tomasrepcik.voidlauncher.data.local.LauncherDatabase
import com.tomasrepcik.voidlauncher.data.local.LauncherPreferencesEntity
import com.tomasrepcik.voidlauncher.data.local.PinnedAppEntity
import com.tomasrepcik.voidlauncher.data.local.ShortcutEntity
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferences
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.data.model.withBackgroundColorsEnabled
import com.tomasrepcik.voidlauncher.data.model.withHomeBackground
import com.tomasrepcik.voidlauncher.data.model.withNavigationTutorialSeen
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

@Suppress("TooManyFunctions") // One adapter implements the narrow storage ports as atomic Room operations.
internal class RoomLauncherStorage(
    private val database: LauncherDatabase,
) : LauncherStorage {
    private val pinnedAppDao = database.pinnedAppDao()
    private val shortcutDao = database.shortcutDao()
    private val preferencesDao = database.preferencesDao()
    private val installedAppDao = database.installedAppDao()
    private val scheduleDao = database.appScheduleDao()

    override val snapshots: Flow<LauncherStorageSnapshot> = combine(
        installedAppDao.observeAll(),
        pinnedAppDao.observeSection(HOME_SECTION),
        shortcutDao.observeAll(),
        preferencesDao.observe(),
        scheduleDao.observeAll(),
    ) { installed, pinned, shortcuts, preferences, schedules ->
        LauncherStorageSnapshot(
            installedApps = installed.map(InstalledAppEntity::toModel),
            pinnedApps = pinned.map(PinnedAppEntity::toStored),
            shortcuts = shortcuts.mapNotNull(ShortcutEntity::toStored),
            preferences = preferences?.model ?: LauncherPreferences(),
            schedules = schedules.map { it.toModel() },
        )
    }.catch { cause ->
        throw cause.asStorageAccessException()
    }

    override suspend fun initialize() = storageCall {
        database.withTransaction {
            if (preferencesDao.get() == null) {
                preferencesDao.upsert(LauncherPreferencesEntity())
            }
            if (shortcutDao.count() == 0) {
                shortcutDao.upsert(defaultShortcut(ShortcutSlot.LEFT, TYPE_CONTACTS))
                shortcutDao.upsert(defaultShortcut(ShortcutSlot.RIGHT, TYPE_CAMERA))
            }
        }
    }

    override suspend fun replaceInstalledApps(apps: List<InstalledApp>) = storageCall {
        database.withTransaction {
            installedAppDao.deleteAll()
            installedAppDao.insertAll(apps.map(InstalledApp::toEntity))
        }
    }

    override suspend fun saveHomeApps(apps: List<AppKey>) = storageCall {
        database.withTransaction {
            writeHomeApps(apps.distinct().mapIndexed(::pinnedAppEntity))
        }
    }

    override suspend fun addHomeApp(appKey: AppKey) = storageCall {
        database.withTransaction {
            val current = pinnedAppDao.getSection(HOME_SECTION)
            if (current.none { entity -> entity.key == appKey }) {
                pinnedAppDao.insertAll(listOf(pinnedAppEntity(current.size, appKey)))
            }
        }
    }

    override suspend fun removeHomeApp(appKey: AppKey) = storageCall {
        database.withTransaction {
            val remaining = pinnedAppDao.getSection(HOME_SECTION)
                .filterNot { entity -> entity.key == appKey }
                .mapIndexed { index, entity -> entity.copy(position = index) }
            writeHomeApps(remaining)
        }
    }

    override suspend fun reorderHomeApps(fromIndex: Int, toIndex: Int) = storageCall {
        database.withTransaction {
            val current = pinnedAppDao.getSection(HOME_SECTION).toMutableList()
            if (fromIndex in current.indices && toIndex in current.indices) {
                current.add(toIndex, current.removeAt(fromIndex))
                writeHomeApps(current.mapIndexed { index, entity -> entity.copy(position = index) })
            }
        }
    }

    override suspend fun renameHomeApp(appKey: AppKey, newLabel: String?) = storageCall {
        pinnedAppDao.updateLabelOverride(
            HOME_SECTION,
            appKey.packageName,
            appKey.activityName,
            newLabel,
        )
    }

    override suspend fun saveShortcut(slot: ShortcutSlot, selection: ShortcutSelection) = storageCall {
        shortcutDao.upsert(selection.toEntity(slot))
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

    override suspend fun saveSchedule(schedule: AppSchedule) = storageCall {
        scheduleDao.upsert(schedule.toEntity())
    }

    override suspend fun deleteSchedule(id: String) = storageCall {
        scheduleDao.delete(id)
    }

    private suspend fun writeHomeApps(entities: List<PinnedAppEntity>) {
        pinnedAppDao.deleteSection(HOME_SECTION)
        pinnedAppDao.insertAll(entities)
    }

    private suspend fun updatePreferences(
        update: (LauncherPreferences) -> LauncherPreferences,
    ) = storageCall {
        val current = preferencesDao.get()?.model ?: LauncherPreferences()
        preferencesDao.upsert(update(current).entity)
    }
}
