package com.tomasrepcik.voidlauncher.data.repository

import com.tomasrepcik.voidlauncher.data.local.LauncherDatabase
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferences
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.data.source.InstalledAppsDataSource
import com.tomasrepcik.voidlauncher.domain.error.AppError
import com.tomasrepcik.voidlauncher.domain.error.AppErrorKind
import com.tomasrepcik.voidlauncher.domain.error.AppOperation
import com.tomasrepcik.voidlauncher.domain.error.ErrorRecovery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class LauncherState(
    val installedApps: List<InstalledApp>,
    val pinnedHomeApps: List<InstalledApp>,
    val pinnedAppKeys: Set<AppKey>,
    val bottomShortcuts: List<ResolvedShortcut>,
    val preferences: LauncherPreferences,
)

sealed interface LauncherRepositoryState {
    data object Loading : LauncherRepositoryState

    data class Ready(
        val launcher: LauncherState,
        val mutationError: AppError? = null,
    ) : LauncherRepositoryState

    data class InitializationError(val error: AppError) : LauncherRepositoryState
}

sealed interface RepositoryMutationOutcome {
    data object Completed : RepositoryMutationOutcome
    data class Failed(val error: AppError) : RepositoryMutationOutcome
}

class LauncherRepository internal constructor(
    private val storage: LauncherStorage,
    private val installedAppsDataSource: InstalledAppsDataSource,
    private val scope: CoroutineScope,
) {
    constructor(
        database: LauncherDatabase,
        installedAppsDataSource: InstalledAppsDataSource,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : this(
        storage = RoomLauncherStorage(database),
        installedAppsDataSource = installedAppsDataSource,
        scope = CoroutineScope(SupervisorJob() + ioDispatcher),
    )

    private val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val mutableState = MutableStateFlow<LauncherRepositoryState>(LauncherRepositoryState.Loading)
    private var initialized = false
    private var initializationStarted = false

    val state: StateFlow<LauncherRepositoryState> = mutableState

    init {
        installedAppsDataSource.observeInstalledApps()
            .distinctUntilChanged()
            .onEach { apps ->
                installedApps.value = apps
                if (initialized) storage.replaceInstalledApps(apps)
            }
            .launchIn(scope)
        retryInitialization()
    }

    fun retryInitialization() {
        if (initialized || initializationStarted) return
        initializationStarted = true
        mutableState.value = LauncherRepositoryState.Loading
        scope.launch {
            try {
                storage.initialize()
                val initialSnapshot = storage.snapshots.first()
                installedApps.value = initialSnapshot.installedApps
                initialized = true
                observeStorage()
            } catch (cause: StorageAccessException) {
                mutableState.value = LauncherRepositoryState.InitializationError(
                    AppError(
                        kind = AppErrorKind.STORAGE_INITIALIZATION_FAILED,
                        operation = AppOperation.INITIALIZE_STORAGE,
                        recovery = ErrorRecovery.NONE,
                        cause = cause.cause ?: cause,
                    ),
                )
            } finally {
                initializationStarted = false
            }
        }
    }

    suspend fun saveHomeApps(apps: List<AppKey>) = mutate(AppOperation.SAVE_HOME_APPS) {
        storage.saveHomeApps(apps)
    }

    suspend fun addHomeApp(appKey: AppKey) = mutate(AppOperation.ADD_HOME_APP) {
        storage.addHomeApp(appKey)
    }

    suspend fun removeHomeApp(appKey: AppKey) = mutate(AppOperation.REMOVE_HOME_APP) {
        storage.removeHomeApp(appKey)
    }

    suspend fun reorderHomeApps(fromIndex: Int, toIndex: Int) = mutate(AppOperation.REORDER_HOME_APPS) {
        storage.reorderHomeApps(fromIndex, toIndex)
    }

    suspend fun renameHomeApp(appKey: AppKey, newLabel: String?) = mutate(AppOperation.RENAME_HOME_APP) {
        storage.renameHomeApp(appKey, newLabel)
    }

    suspend fun saveShortcut(slot: ShortcutSlot, selection: ShortcutSelection) =
        mutate(AppOperation.SAVE_SHORTCUT) { storage.saveShortcut(slot, selection) }

    suspend fun setHomeAppCount(count: Int) = mutate(AppOperation.UPDATE_PREFERENCES) {
        storage.setHomeAppCount(count)
    }

    private fun observeStorage() {
        combine(storage.snapshots, installedApps) { snapshot, currentInstalledApps ->
            toLauncherState(snapshot, currentInstalledApps.ifEmpty { snapshot.installedApps })
        }.onEach { launcherState ->
            mutableState.value = LauncherRepositoryState.Ready(launcherState)
        }.launchIn(scope)
    }

    private suspend fun toLauncherState(
        snapshot: LauncherStorageSnapshot,
        currentInstalledApps: List<InstalledApp>,
    ): LauncherState {
        val appMap = currentInstalledApps.associateBy(InstalledApp::key).toMutableMap()
        snapshot.pinnedApps.forEach { pinned ->
            if (pinned.key !in appMap) {
                installedAppsDataSource.getInstalledApp(pinned.key)?.let { appMap[pinned.key] = it }
            }
        }
        val pinnedApps = snapshot.pinnedApps.mapNotNull { pinned ->
            val app = appMap[pinned.key] ?: return@mapNotNull null
            pinned.labelOverride?.let { label ->
                app.copy(label = label, sortLabel = label.lowercase())
            } ?: app
        }
        val shortcuts = snapshot.shortcuts.map { shortcut ->
            val app = (shortcut.selection as? ShortcutSelection.AppShortcut)?.let { selection ->
                appMap[selection.key] ?: installedAppsDataSource.getInstalledApp(selection.key)
            }
            ResolvedShortcut(
                slot = shortcut.slot,
                label = shortcut.customLabel ?: app?.label ?: shortcut.defaultLabel,
                selection = shortcut.selection,
                installedApp = app,
                isAvailable = shortcut.selection !is ShortcutSelection.AppShortcut || app != null,
            )
        }
        return LauncherState(
            installedApps = currentInstalledApps,
            pinnedHomeApps = pinnedApps,
            pinnedAppKeys = snapshot.pinnedApps.map(StoredPinnedApp::key).toSet(),
            bottomShortcuts = shortcuts,
            preferences = snapshot.preferences,
        )
    }

    private suspend fun mutate(
        operation: AppOperation,
        mutation: suspend () -> Unit,
    ): RepositoryMutationOutcome = try {
        mutation()
        val ready = mutableState.value as? LauncherRepositoryState.Ready
        if (ready?.mutationError != null) mutableState.value = ready.copy(mutationError = null)
        RepositoryMutationOutcome.Completed
    } catch (cause: StorageAccessException) {
        val error = AppError(
            kind = AppErrorKind.STORAGE_WRITE_FAILED,
            operation = operation,
            recovery = ErrorRecovery.NONE,
            cause = cause.cause ?: cause,
        )
        val ready = mutableState.value as? LauncherRepositoryState.Ready
        if (ready != null) mutableState.value = ready.copy(mutationError = error)
        RepositoryMutationOutcome.Failed(error)
    }
}

private val StoredShortcut.defaultLabel: String
    get() = when (selection) {
        ShortcutSelection.SystemCamera -> "Camera"
        ShortcutSelection.SystemContacts -> "Contacts"
        is ShortcutSelection.AppShortcut -> "Unavailable"
    }
