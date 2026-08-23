package com.tomasrepcik.voidlauncher.data.repository

import com.tomasrepcik.voidlauncher.data.local.LauncherDatabase
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferences
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferencesMutation
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.data.source.InstalledAppsDataSource
import com.tomasrepcik.voidlauncher.domain.error.AppError
import com.tomasrepcik.voidlauncher.domain.error.AppErrorKind
import com.tomasrepcik.voidlauncher.domain.error.AppOperation
import com.tomasrepcik.voidlauncher.domain.error.ErrorRecovery
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import com.tomasrepcik.voidlauncher.domain.schedule.ScheduleMutation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
    val schedules: List<AppSchedule>,
)

sealed interface LauncherRepositoryState {
    data object Loading : LauncherRepositoryState

    data class Ready(
        val launcher: LauncherState,
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
            .catch { cause ->
                cause.rethrowIfCancellation()
                mutableState.value = LauncherRepositoryState.InitializationError(
                    cause.toAppError(
                        kind = AppErrorKind.INSTALLED_APPS_LOAD_FAILED,
                        operation = AppOperation.LOAD_INSTALLED_APPS,
                    ),
                )
            }
            .onEach { apps ->
                installedApps.value = apps
                if (initialized) {
                    try {
                        storage.replaceInstalledApps(apps)
                    } catch (cause: StorageAccessException) {
                        mutableState.value = LauncherRepositoryState.InitializationError(
                            cause.toAppError(
                                kind = AppErrorKind.STORAGE_WRITE_FAILED,
                                operation = AppOperation.LOAD_INSTALLED_APPS,
                            ),
                        )
                    }
                }
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
                    cause.toAppError(
                        kind = AppErrorKind.STORAGE_INITIALIZATION_FAILED,
                        operation = AppOperation.INITIALIZE_STORAGE,
                    ),
                )
            } finally {
                initializationStarted = false
            }
        }
    }

    suspend fun saveHomeApps(apps: List<AppKey>) = performMutation(AppOperation.SAVE_HOME_APPS) {
        storage.saveHomeApps(apps)
    }

    suspend fun addHomeApp(appKey: AppKey) = performMutation(AppOperation.ADD_HOME_APP) {
        storage.addHomeApp(appKey)
    }

    suspend fun removeHomeApp(appKey: AppKey) = performMutation(AppOperation.REMOVE_HOME_APP) {
        storage.removeHomeApp(appKey)
    }

    suspend fun reorderHomeApps(fromIndex: Int, toIndex: Int) = performMutation(AppOperation.REORDER_HOME_APPS) {
        storage.reorderHomeApps(fromIndex, toIndex)
    }

    suspend fun renameHomeApp(appKey: AppKey, newLabel: String?) =
        performMutation(AppOperation.RENAME_HOME_APP) {
            storage.renameHomeApp(appKey, newLabel)
        }

    suspend fun saveShortcut(slot: ShortcutSlot, selection: ShortcutSelection) =
        performMutation(AppOperation.SAVE_SHORTCUT) { storage.saveShortcut(slot, selection) }

    suspend fun mutatePreferences(mutation: LauncherPreferencesMutation) =
        performMutation(AppOperation.UPDATE_PREFERENCES) {
            storage.mutatePreferences(mutation)
        }

    suspend fun mutateSchedule(mutation: ScheduleMutation) = performMutation(
        operation = when (mutation) {
            is ScheduleMutation.Save -> AppOperation.SAVE_SCHEDULE
            is ScheduleMutation.Delete -> AppOperation.DELETE_SCHEDULE
        },
    ) {
        storage.mutateSchedule(mutation)
    }

    private fun observeStorage() {
        combine(storage.snapshots, installedApps) { snapshot, currentInstalledApps ->
            toLauncherState(snapshot, currentInstalledApps.ifEmpty { snapshot.installedApps })
        }.catch { cause ->
            cause.rethrowIfCancellation()
            mutableState.value = LauncherRepositoryState.InitializationError(
                cause.toAppError(
                    kind = AppErrorKind.STORAGE_READ_FAILED,
                    operation = AppOperation.READ_STORAGE,
                ),
            )
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
            schedules = snapshot.schedules,
        )
    }
}

private suspend fun performMutation(
    operation: AppOperation,
    mutation: suspend () -> Unit,
): RepositoryMutationOutcome = try {
    mutation()
    RepositoryMutationOutcome.Completed
} catch (cause: StorageAccessException) {
    RepositoryMutationOutcome.Failed(
        cause.toAppError(
            kind = AppErrorKind.STORAGE_WRITE_FAILED,
            operation = operation,
        ),
    )
}

private fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}

private fun Throwable.toAppError(
    kind: AppErrorKind,
    operation: AppOperation,
): AppError {
    return AppError(
        kind = kind,
        operation = operation,
        recovery = ErrorRecovery.NONE,
        cause = (this as? StorageAccessException)?.cause ?: this,
    )
}

private val StoredShortcut.defaultLabel: String
    get() = when (selection) {
        ShortcutSelection.SystemCamera -> "Camera"
        ShortcutSelection.SystemContacts -> "Contacts"
        is ShortcutSelection.AppShortcut -> "Unavailable"
    }
