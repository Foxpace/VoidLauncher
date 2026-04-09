package com.tomasrepcik.voidlauncher.data.repository

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
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.data.source.InstalledAppsDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn

private const val HOME_SECTION = "HOME"
private const val TYPE_APP = "APP"
private const val TYPE_CONTACTS = "SYSTEM_CONTACTS"
private const val TYPE_CAMERA = "SYSTEM_CAMERA"

interface LauncherRepository {
    fun observeInstalledApps(): Flow<List<InstalledApp>>
    fun observePinnedHomeApps(): Flow<List<InstalledApp>>
    fun observePinnedAppKeys(): Flow<Set<AppKey>>
    fun observeBottomShortcuts(): Flow<List<ResolvedShortcut>>
    fun observePreferences(): Flow<LauncherPreferences>
    suspend fun ensureDefaults()
    suspend fun saveHomeApps(apps: List<AppKey>)
    suspend fun addHomeApp(appKey: AppKey)
    suspend fun removeHomeApp(appKey: AppKey)
    suspend fun reorderHomeApps(fromIndex: Int, toIndex: Int)
    suspend fun renameHomeApp(appKey: AppKey, newLabel: String?)
    suspend fun saveShortcut(slot: ShortcutSlot, selection: ShortcutSelection)
    suspend fun setHomeAppCount(count: Int)
}

class DefaultLauncherRepository(
    private val database: LauncherDatabase,
    private val installedAppsDataSource: InstalledAppsDataSource,
) : LauncherRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pinnedAppDao = database.pinnedAppDao()
    private val shortcutDao = database.shortcutDao()
    private val preferencesDao = database.preferencesDao()
    private val installedAppDao = database.installedAppDao()
    private val installedAppsFlow = installedAppDao.observeAll()
        .map { entities -> entities.map(InstalledAppEntity::toModel) }
        .distinctUntilChanged()
        .shareIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            replay = 1,
        )

    init {
        installedAppsDataSource.observeInstalledApps()
            .distinctUntilChanged()
            .onEach(::syncInstalledAppsCache)
            .launchIn(repositoryScope)
    }

    override fun observeInstalledApps(): Flow<List<InstalledApp>> = installedAppsFlow

    override fun observePinnedHomeApps(): Flow<List<InstalledApp>> =
        combine(
            pinnedAppDao.observeSection(HOME_SECTION),
            installedAppsFlow,
        ) { pinnedApps, installedApps ->
            pinnedApps to installedApps.associateBy(InstalledApp::key)
        }.map { (pinnedApps, appMap) ->
            pinnedApps.mapNotNull { entity ->
                val key = AppKey(entity.packageName, entity.activityName)
                resolveInstalledApp(key, appMap[key])?.let { app ->
                    if (entity.labelOverride != null) {
                        app.copy(
                            label = entity.labelOverride,
                            sortLabel = entity.labelOverride.lowercase(),
                        )
                    } else {
                        app
                    }
                }
            }
        }

    override fun observePinnedAppKeys(): Flow<Set<AppKey>> =
        pinnedAppDao.observeSection(HOME_SECTION).map { entities ->
            entities.map { AppKey(it.packageName, it.activityName) }.toSet()
        }

    override fun observeBottomShortcuts(): Flow<List<ResolvedShortcut>> =
        combine(
            shortcutDao.observeAll(),
            installedAppsFlow,
        ) { shortcuts, installedApps ->
            shortcuts to installedApps.associateBy(InstalledApp::key)
        }.map { (shortcuts, appMap) ->
            shortcuts.mapNotNull { entity ->
                val slot = entity.slot.toShortcutSlot() ?: return@mapNotNull null
                when (entity.shortcutType) {
                    TYPE_CONTACTS -> ResolvedShortcut(
                        slot = slot,
                        label = entity.customLabel ?: "Contacts",
                        selection = ShortcutSelection.SystemContacts,
                    )

                    TYPE_CAMERA -> ResolvedShortcut(
                        slot = slot,
                        label = entity.customLabel ?: "Camera",
                        selection = ShortcutSelection.SystemCamera,
                    )

                    TYPE_APP -> {
                        val packageName = entity.packageName ?: return@mapNotNull null
                        val activityName = entity.activityName ?: return@mapNotNull null
                        val appKey = AppKey(packageName, activityName)
                        val app = resolveInstalledApp(appKey, appMap[appKey])
                        ResolvedShortcut(
                            slot = slot,
                            label = entity.customLabel ?: app?.label ?: "Unavailable",
                            selection = ShortcutSelection.AppShortcut(appKey),
                            installedApp = app,
                            isAvailable = app != null,
                        )
                    }

                    else -> null
                }
            }
        }

    override fun observePreferences(): Flow<LauncherPreferences> =
        preferencesDao.observe().map { entity ->
            LauncherPreferences(homeAppCount = entity?.homeAppCount ?: DEFAULT_HOME_APP_COUNT)
        }

    override suspend fun ensureDefaults() {
        database.withTransaction {
            if (preferencesDao.get() == null) {
                preferencesDao.upsert(
                    LauncherPreferencesEntity(homeAppCount = DEFAULT_HOME_APP_COUNT)
                )
            }

            if (shortcutDao.count() == 0) {
                shortcutDao.upsert(
                    ShortcutEntity(
                        slot = ShortcutSlot.LEFT.name,
                        position = ShortcutSlot.LEFT.ordinal,
                        shortcutType = TYPE_CONTACTS,
                    )
                )
                shortcutDao.upsert(
                    ShortcutEntity(
                        slot = ShortcutSlot.RIGHT.name,
                        position = ShortcutSlot.RIGHT.ordinal,
                        shortcutType = TYPE_CAMERA,
                    )
                )
            }
        }
    }

    override suspend fun saveHomeApps(apps: List<AppKey>) {
        database.withTransaction {
            pinnedAppDao.deleteSection(HOME_SECTION)
            pinnedAppDao.insertAll(
                apps.distinct().mapIndexed { index, appKey ->
                    PinnedAppEntity(
                        section = HOME_SECTION,
                        position = index,
                        packageName = appKey.packageName,
                        activityName = appKey.activityName,
                    )
                }
            )
        }
    }

    override suspend fun addHomeApp(appKey: AppKey) {
        database.withTransaction {
            val current = pinnedAppDao.getSection(HOME_SECTION)
            if (current.any { it.packageName == appKey.packageName && it.activityName == appKey.activityName }) {
                return@withTransaction
            }
            pinnedAppDao.insertAll(
                listOf(
                    PinnedAppEntity(
                        section = HOME_SECTION,
                        position = current.size,
                        packageName = appKey.packageName,
                        activityName = appKey.activityName,
                    )
                )
            )
        }
    }

    override suspend fun removeHomeApp(appKey: AppKey) {
        database.withTransaction {
            val current = pinnedAppDao.getSection(HOME_SECTION)
            val filtered = current.filter {
                !(it.packageName == appKey.packageName && it.activityName == appKey.activityName)
            }
            pinnedAppDao.deleteSection(HOME_SECTION)
            pinnedAppDao.insertAll(
                filtered.mapIndexed { index, entity ->
                    entity.copy(position = index)
                }
            )
        }
    }

    override suspend fun reorderHomeApps(fromIndex: Int, toIndex: Int) {
        database.withTransaction {
            val current = pinnedAppDao.getSection(HOME_SECTION).toMutableList()
            if (fromIndex !in current.indices || toIndex !in current.indices) return@withTransaction
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            pinnedAppDao.deleteSection(HOME_SECTION)
            pinnedAppDao.insertAll(
                current.mapIndexed { index, entity -> entity.copy(position = index) }
            )
        }
    }

    override suspend fun renameHomeApp(appKey: AppKey, newLabel: String?) {
        pinnedAppDao.updateLabelOverride(
            HOME_SECTION, appKey.packageName, appKey.activityName, newLabel
        )
    }

    override suspend fun saveShortcut(slot: ShortcutSlot, selection: ShortcutSelection) {
        val entity = when (selection) {
            ShortcutSelection.SystemCamera -> ShortcutEntity(
                slot = slot.name,
                position = slot.ordinal,
                shortcutType = TYPE_CAMERA,
            )

            ShortcutSelection.SystemContacts -> ShortcutEntity(
                slot = slot.name,
                position = slot.ordinal,
                shortcutType = TYPE_CONTACTS,
            )

            is ShortcutSelection.AppShortcut -> ShortcutEntity(
                slot = slot.name,
                position = slot.ordinal,
                shortcutType = TYPE_APP,
                packageName = selection.key.packageName,
                activityName = selection.key.activityName,
            )
        }
        shortcutDao.upsert(entity)
    }

    override suspend fun setHomeAppCount(count: Int) {
        val boundedCount = count.coerceIn(MIN_HOME_APP_COUNT, MAX_HOME_APP_COUNT)
        val current = preferencesDao.get()
        preferencesDao.upsert(
            LauncherPreferencesEntity(
                id = current?.id ?: 0,
                homeAppCount = boundedCount,
            )
        )
    }

    private suspend fun syncInstalledAppsCache(apps: List<InstalledApp>) {
        database.withTransaction {
            installedAppDao.deleteAll()
            installedAppDao.insertAll(apps.map(InstalledApp::toEntity))
        }
    }

    private suspend fun resolveInstalledApp(
        appKey: AppKey,
        cachedApp: InstalledApp?,
    ): InstalledApp? = cachedApp ?: installedAppsDataSource.getInstalledApp(appKey)

    private fun String.toShortcutSlot(): ShortcutSlot? =
        ShortcutSlot.entries.firstOrNull { it.name == this }
}

private fun InstalledApp.toEntity(): InstalledAppEntity = InstalledAppEntity(
    packageName = key.packageName,
    activityName = key.activityName,
    label = label,
    sortLabel = sortLabel,
)

private fun InstalledAppEntity.toModel(): InstalledApp = InstalledApp(
    key = AppKey(
        packageName = packageName,
        activityName = activityName,
    ),
    label = label,
    sortLabel = sortLabel,
)
