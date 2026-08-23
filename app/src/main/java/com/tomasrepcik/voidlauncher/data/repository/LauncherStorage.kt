package com.tomasrepcik.voidlauncher.data.repository

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.tomasrepcik.voidlauncher.data.local.AppScheduleEntity
import com.tomasrepcik.voidlauncher.data.local.InstalledAppEntity
import com.tomasrepcik.voidlauncher.data.local.LauncherDatabase
import com.tomasrepcik.voidlauncher.data.local.LauncherPreferencesEntity
import com.tomasrepcik.voidlauncher.data.local.PinnedAppEntity
import com.tomasrepcik.voidlauncher.data.local.ShortcutEntity
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.DEFAULT_HOME_APP_COUNT
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferences
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferencesMutation
import com.tomasrepcik.voidlauncher.data.model.MAX_HOME_APP_COUNT
import com.tomasrepcik.voidlauncher.data.model.MIN_HOME_APP_COUNT
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import com.tomasrepcik.voidlauncher.domain.schedule.MINUTES_PER_DAY
import com.tomasrepcik.voidlauncher.domain.schedule.ScheduleMutation
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

private const val HOME_SECTION = "HOME"
private const val TYPE_APP = "APP"
private const val TYPE_CONTACTS = "SYSTEM_CONTACTS"
private const val TYPE_CAMERA = "SYSTEM_CAMERA"

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

internal class StorageAccessException(cause: Throwable) : RuntimeException(cause)

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
    suspend fun mutatePreferences(mutation: LauncherPreferencesMutation)
    suspend fun mutateSchedule(mutation: ScheduleMutation)
}

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
            preferences = LauncherPreferences(
                homeAppCount = preferences?.homeAppCount ?: DEFAULT_HOME_APP_COUNT,
                hasSeenNavigationTutorial = preferences?.hasSeenNavigationTutorial ?: false,
            ),
            schedules = schedules.map(AppScheduleEntity::toModel),
        )
    }.catch { cause ->
        throw cause.asStorageAccessException()
    }

    override suspend fun initialize() = storageCall {
        database.withTransaction {
            if (preferencesDao.get() == null) {
                preferencesDao.upsert(LauncherPreferencesEntity(homeAppCount = DEFAULT_HOME_APP_COUNT))
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

    override suspend fun mutatePreferences(mutation: LauncherPreferencesMutation) = storageCall {
        val current = preferencesDao.get()
            ?: LauncherPreferencesEntity(homeAppCount = DEFAULT_HOME_APP_COUNT)
        val updated = when (mutation) {
            is LauncherPreferencesMutation.SetHomeAppCount -> current.copy(
                homeAppCount = mutation.count.coerceIn(MIN_HOME_APP_COUNT, MAX_HOME_APP_COUNT),
            )
            LauncherPreferencesMutation.MarkNavigationTutorialSeen -> current.copy(
                hasSeenNavigationTutorial = true,
            )
        }
        preferencesDao.upsert(updated)
    }

    override suspend fun mutateSchedule(mutation: ScheduleMutation) = storageCall {
        when (mutation) {
            is ScheduleMutation.Save -> scheduleDao.upsert(mutation.schedule.toEntity())
            is ScheduleMutation.Delete -> scheduleDao.delete(mutation.id)
        }
    }

    private suspend fun writeHomeApps(entities: List<PinnedAppEntity>) {
        pinnedAppDao.deleteSection(HOME_SECTION)
        pinnedAppDao.insertAll(entities)
    }
}

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

    override suspend fun saveHomeApps(apps: List<AppKey>) = mutate {
        it.copy(pinnedApps = apps.distinct().map(::StoredPinnedApp))
    }

    override suspend fun addHomeApp(appKey: AppKey) = mutate { snapshot ->
        if (snapshot.pinnedApps.any { it.key == appKey }) snapshot else {
            snapshot.copy(pinnedApps = snapshot.pinnedApps + StoredPinnedApp(appKey))
        }
    }

    override suspend fun removeHomeApp(appKey: AppKey) = mutate {
        it.copy(pinnedApps = it.pinnedApps.filterNot { pinned -> pinned.key == appKey })
    }

    override suspend fun reorderHomeApps(fromIndex: Int, toIndex: Int) = mutate { snapshot ->
        val apps = snapshot.pinnedApps.toMutableList()
        if (fromIndex in apps.indices && toIndex in apps.indices) {
            apps.add(toIndex, apps.removeAt(fromIndex))
        }
        snapshot.copy(pinnedApps = apps)
    }

    override suspend fun renameHomeApp(appKey: AppKey, newLabel: String?) = mutate { snapshot ->
        snapshot.copy(
            pinnedApps = snapshot.pinnedApps.map { pinned ->
                if (pinned.key == appKey) pinned.copy(labelOverride = newLabel) else pinned
            },
        )
    }

    override suspend fun saveShortcut(slot: ShortcutSlot, selection: ShortcutSelection) = mutate { snapshot ->
        snapshot.copy(
            shortcuts = snapshot.shortcuts.filterNot { it.slot == slot } + StoredShortcut(slot, selection),
        )
    }

    override suspend fun mutatePreferences(mutation: LauncherPreferencesMutation) = mutate { snapshot ->
        val updated = when (mutation) {
            is LauncherPreferencesMutation.SetHomeAppCount -> snapshot.preferences.copy(
                homeAppCount = mutation.count.coerceIn(MIN_HOME_APP_COUNT, MAX_HOME_APP_COUNT),
            )
            LauncherPreferencesMutation.MarkNavigationTutorialSeen -> snapshot.preferences.copy(
                hasSeenNavigationTutorial = true,
            )
        }
        snapshot.copy(
            preferences = updated,
        )
    }

    override suspend fun mutateSchedule(mutation: ScheduleMutation) = mutate { snapshot ->
        when (mutation) {
            is ScheduleMutation.Save -> snapshot.copy(
                schedules = (snapshot.schedules.filterNot { it.id == mutation.schedule.id } +
                    mutation.schedule).sortedBy { it.name.lowercase() },
            )
            is ScheduleMutation.Delete -> snapshot.copy(
                schedules = snapshot.schedules.filterNot { it.id == mutation.id },
            )
        }
    }

    private fun mutate(transform: (LauncherStorageSnapshot) -> LauncherStorageSnapshot) {
        if (writeFailuresRemaining > 0) {
            writeFailuresRemaining--
            throw SQLiteException("planned in-memory write failure")
                .asStorageAccessException()
        }
        state.update(transform)
    }
}

private suspend inline fun <T> storageCall(crossinline block: suspend () -> T): T = flow {
    emit(block())
}.catch { cause ->
    throw cause.asStorageAccessException()
}.first()

private fun Throwable.asStorageAccessException(): StorageAccessException =
    this as? StorageAccessException ?: StorageAccessException(this)

private val PinnedAppEntity.key: AppKey
    get() = AppKey(packageName, activityName)

private fun PinnedAppEntity.toStored() = StoredPinnedApp(key, labelOverride)

private fun ShortcutEntity.toStored(): StoredShortcut? {
    val shortcutSlot = ShortcutSlot.entries.firstOrNull { it.name == slot } ?: return null
    val selection = when (shortcutType) {
        TYPE_CONTACTS -> ShortcutSelection.SystemContacts
        TYPE_CAMERA -> ShortcutSelection.SystemCamera
        TYPE_APP -> packageName?.let { packageName ->
            activityName?.let { activityName ->
                ShortcutSelection.AppShortcut(AppKey(packageName, activityName))
            }
        }
        else -> null
    } ?: return null
    return StoredShortcut(shortcutSlot, selection, customLabel)
}

private fun defaultShortcut(slot: ShortcutSlot, type: String) = ShortcutEntity(
    slot = slot.name,
    position = slot.ordinal,
    shortcutType = type,
)

private fun pinnedAppEntity(position: Int, appKey: AppKey) = PinnedAppEntity(
    section = HOME_SECTION,
    position = position,
    packageName = appKey.packageName,
    activityName = appKey.activityName,
)

private fun ShortcutSelection.toEntity(slot: ShortcutSlot): ShortcutEntity = when (this) {
    ShortcutSelection.SystemCamera -> defaultShortcut(slot, TYPE_CAMERA)
    ShortcutSelection.SystemContacts -> defaultShortcut(slot, TYPE_CONTACTS)
    is ShortcutSelection.AppShortcut -> ShortcutEntity(
        slot = slot.name,
        position = slot.ordinal,
        shortcutType = TYPE_APP,
        packageName = key.packageName,
        activityName = key.activityName,
    )
}

private fun InstalledApp.toEntity() = InstalledAppEntity(
    packageName = key.packageName,
    activityName = key.activityName,
    label = label,
    sortLabel = sortLabel,
)

private fun InstalledAppEntity.toModel() = InstalledApp(
    key = AppKey(packageName, activityName),
    label = label,
    sortLabel = sortLabel,
)

private fun AppSchedule.toEntity() = AppScheduleEntity(
    id = id,
    name = name,
    days = days.joinToString(",", transform = DayOfWeek::name),
    startMinute = startMinute.coerceIn(0, MINUTES_PER_DAY - 1),
    endMinute = endMinute.coerceIn(0, MINUTES_PER_DAY - 1),
    appKeys = appKeys.joinToString("\n") { key ->
        "${key.packageName}\t${key.activityName}"
    },
    enabled = enabled,
)

private fun AppScheduleEntity.toModel(): AppSchedule {
    val storedDays = days.split(',')
        .mapNotNull { value -> DayOfWeek.entries.firstOrNull { it.name == value } }
        .toSet()
    val storedAppKeys = appKeys.lineSequence().mapNotNull { value ->
        val parts = value.split('\t', limit = 2)
        if (parts.size == 2) AppKey(parts[0], parts[1]) else null
    }.toSet()
    return AppSchedule(
        id = id,
        name = name,
        days = storedDays,
        startMinute = startMinute.coerceIn(0, MINUTES_PER_DAY - 1),
        endMinute = endMinute.coerceIn(0, MINUTES_PER_DAY - 1),
        appKeys = storedAppKeys,
        enabled = enabled,
    )
}
