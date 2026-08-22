package com.tomasrepcik.voidlauncher.data.repository

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.tomasrepcik.voidlauncher.data.local.InstalledAppEntity
import com.tomasrepcik.voidlauncher.data.local.LauncherDatabase
import com.tomasrepcik.voidlauncher.data.local.LauncherPreferencesEntity
import com.tomasrepcik.voidlauncher.data.local.PinnedAppEntity
import com.tomasrepcik.voidlauncher.data.local.ShortcutEntity
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.DEFAULT_HOME_APP_COUNT
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferences
import com.tomasrepcik.voidlauncher.data.model.MAX_HOME_APP_COUNT
import com.tomasrepcik.voidlauncher.data.model.MIN_HOME_APP_COUNT
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    suspend fun setHomeAppCount(count: Int)
}

internal class RoomLauncherStorage(
    private val database: LauncherDatabase,
) : LauncherStorage {
    private val pinnedAppDao = database.pinnedAppDao()
    private val shortcutDao = database.shortcutDao()
    private val preferencesDao = database.preferencesDao()
    private val installedAppDao = database.installedAppDao()

    override val snapshots: Flow<LauncherStorageSnapshot> = combine(
        installedAppDao.observeAll(),
        pinnedAppDao.observeSection(HOME_SECTION),
        shortcutDao.observeAll(),
        preferencesDao.observe(),
    ) { installed, pinned, shortcuts, preferences ->
        LauncherStorageSnapshot(
            installedApps = installed.map(InstalledAppEntity::toModel),
            pinnedApps = pinned.map(PinnedAppEntity::toStored),
            shortcuts = shortcuts.mapNotNull(ShortcutEntity::toStored),
            preferences = LauncherPreferences(
                homeAppCount = preferences?.homeAppCount ?: DEFAULT_HOME_APP_COUNT,
            ),
        )
    }.catch { cause ->
        if (cause is SQLiteException) throw StorageAccessException(cause)
        throw cause
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
        replaceHomeApps(apps.distinct().mapIndexed(::pinnedAppEntity))
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

    override suspend fun setHomeAppCount(count: Int) = storageCall {
        val current = preferencesDao.get()
        preferencesDao.upsert(
            LauncherPreferencesEntity(
                id = current?.id ?: 0,
                homeAppCount = count.coerceIn(MIN_HOME_APP_COUNT, MAX_HOME_APP_COUNT),
            ),
        )
    }

    private suspend fun replaceHomeApps(entities: List<PinnedAppEntity>) {
        database.withTransaction { writeHomeApps(entities) }
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
            throw StorageAccessException(SQLiteException("planned in-memory initialization failure"))
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

    override suspend fun setHomeAppCount(count: Int) = mutate { snapshot ->
        snapshot.copy(
            preferences = LauncherPreferences(count.coerceIn(MIN_HOME_APP_COUNT, MAX_HOME_APP_COUNT)),
        )
    }

    private fun mutate(transform: (LauncherStorageSnapshot) -> LauncherStorageSnapshot) {
        if (writeFailuresRemaining > 0) {
            writeFailuresRemaining--
            throw StorageAccessException(SQLiteException("planned in-memory write failure"))
        }
        state.update(transform)
    }
}

private suspend inline fun <T> storageCall(crossinline block: suspend () -> T): T = try {
    block()
} catch (cause: SQLiteException) {
    throw StorageAccessException(cause)
}

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
