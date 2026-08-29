package com.tomasrepcik.voidlauncher.storage.launcher

import com.tomasrepcik.voidlauncher.launcher.AppKey
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.LauncherPreferences
import com.tomasrepcik.voidlauncher.launcher.ResolvedShortcut
import com.tomasrepcik.voidlauncher.launcher.ShortcutSelection
import com.tomasrepcik.voidlauncher.launcher.error.AppError
import com.tomasrepcik.voidlauncher.launcher.error.AppErrorKind
import com.tomasrepcik.voidlauncher.launcher.error.AppOperation
import com.tomasrepcik.voidlauncher.launcher.error.ErrorRecovery
import com.tomasrepcik.voidlauncher.schedule.data.AppSchedule
import com.tomasrepcik.voidlauncher.storage.database.LauncherStorageSnapshot
import com.tomasrepcik.voidlauncher.storage.database.StoredPinnedApp
import com.tomasrepcik.voidlauncher.storage.database.StoredShortcut
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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

sealed interface RepositoryWriteResult {
    data object Completed : RepositoryWriteResult
    data class Failed(val error: AppError) : RepositoryWriteResult
}

class LauncherRepository internal constructor(
    internal val storage: LauncherStorage,
    installedAppUpdates: Flow<List<InstalledApp>>,
    private val findInstalledApp: suspend (AppKey) -> InstalledApp?,
    private val scope: CoroutineScope,
) {
    private val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val mutableState = MutableStateFlow<LauncherRepositoryState>(LauncherRepositoryState.Loading)
    private var initialized = false
    private var initializationStarted = false

    val state: StateFlow<LauncherRepositoryState> = mutableState

    init {
        installedAppUpdates
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
                initializeRepository()
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

    private suspend fun initializeRepository() {
        storage.initialize()
        val initialSnapshot = storage.snapshots.first()
        installedApps.value = initialSnapshot.installedApps
        initialized = true
        observeStorage()
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
                findInstalledApp(pinned.key)?.let { appMap[pinned.key] = it }
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
                appMap[selection.key] ?: findInstalledApp(selection.key)
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

internal suspend fun writeToStorage(
    operation: AppOperation,
    write: suspend () -> Unit,
): RepositoryWriteResult = try {
    completeStorageWrite(write)
} catch (cause: StorageAccessException) {
    RepositoryWriteResult.Failed(
        cause.toAppError(
            kind = AppErrorKind.STORAGE_WRITE_FAILED,
            operation = operation,
        ),
    )
}

private suspend fun completeStorageWrite(write: suspend () -> Unit): RepositoryWriteResult {
    write()
    return RepositoryWriteResult.Completed
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
