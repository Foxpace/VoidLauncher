package com.tomasrepcik.voidlauncher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.voidlauncher.appcatalog.action.AppSelectionAction
import com.tomasrepcik.voidlauncher.appcatalog.action.HandleAppSelection
import com.tomasrepcik.voidlauncher.home.data.HomeAppsRepository
import com.tomasrepcik.voidlauncher.appcatalog.data.InstalledAppsRepository
import com.tomasrepcik.voidlauncher.storage.launcher.RepositoryWriteResult
import com.tomasrepcik.voidlauncher.schedule.data.ScheduleRepository
import com.tomasrepcik.voidlauncher.shortcuts.data.ShortcutRepository
import com.tomasrepcik.voidlauncher.launcher.action.LauncherAction
import com.tomasrepcik.voidlauncher.schedule.data.AppScheduleResolver
import com.tomasrepcik.voidlauncher.appcatalog.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.appcatalog.search.SearchTarget
import com.tomasrepcik.voidlauncher.launcher.LauncherRootAction
import com.tomasrepcik.voidlauncher.launcher.sendWriteResult
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Suppress("LongParameterList") // Explicit feature dependencies are clearer than a synthetic wrapper.
class HomeViewModel(
    installedApps: InstalledAppsRepository,
    private val homeApps: HomeAppsRepository,
    shortcuts: ShortcutRepository,
    schedules: ScheduleRepository,
    private val installedAppSearch: InstalledAppSearch,
    private val handleAppSelection: HandleAppSelection,
    scheduleResolver: AppScheduleResolver,
    currentTime: Flow<LocalDateTime>,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val rootActionChannel = Channel<LauncherRootAction>(capacity = Channel.BUFFERED)
    private val navigationChannel = Channel<HomeNavigationEvent>(capacity = Channel.BUFFERED)

    private val currentInstalledApps = installedApps.apps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    private val repositoryData = combine(
        currentInstalledApps,
        homeApps.data,
        shortcuts.shortcuts,
        schedules.schedules,
    ) { currentApps, currentHomeApps, currentShortcuts, currentSchedules ->
        HomeRepositoryData(currentApps, currentHomeApps, currentShortcuts, currentSchedules)
    }

    internal val rootActions = rootActionChannel.receiveAsFlow()
    internal val navigation = navigationChannel.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        query,
        repositoryData,
        currentTime,
    ) { currentQuery, data, now ->
        val installed = data.installedApps
        val pinned = data.homeApps
        val currentShortcuts = data.shortcuts
        val currentSchedules = data.schedules
        if (installed == null || pinned == null || currentShortcuts == null || currentSchedules == null) {
            return@combine HomeUiState(query = currentQuery, isLoading = true)
        }
        val scheduledApps = scheduleResolver.visibleApps(
            defaultApps = pinned.apps,
            installedApps = installed,
            schedules = currentSchedules,
            at = now,
        )
        HomeUiState(
            query = currentQuery,
            homeApps = scheduledApps.apps,
            homeAppKeys = pinned.keys,
            shortcuts = currentShortcuts.sortedBy { it.slot.ordinal },
            searchSuggestions = installedAppSearch.suggestions(currentQuery, installed),
            isScheduleActive = scheduledApps.isScheduleActive,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true),
    )

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OpenDrawer -> navigationChannel.trySend(HomeNavigationEvent.OpenDrawer)
            HomeAction.OpenSchedules -> navigationChannel.trySend(HomeNavigationEvent.OpenSchedules)
            is HomeAction.QueryChanged -> updateQuery(action.value)
            is HomeAction.Search -> search(action.target)
            is HomeAction.OpenApp -> {
                updateQuery("")
                runAppSelection(AppSelectionAction.Open(action.app))
            }
            is HomeAction.AddApp -> runAppSelection(AppSelectionAction.AddToHome(action.app))
            is HomeAction.OpenShortcut -> emitNative(LauncherAction.OpenShortcut(action.shortcut))
            is HomeAction.RemoveApp ->
                runAppSelection(AppSelectionAction.RemoveFromHome(action.app))
            is HomeAction.RenameApp -> runHomeAppWrite {
                homeApps.rename(action.app.key, action.label)
            }
            is HomeAction.ReorderApps -> runHomeAppWrite {
                homeApps.reorder(action.fromIndex, action.toIndex)
            }
            is HomeAction.UninstallApp -> runAppSelection(AppSelectionAction.Uninstall(action.app))
        }
    }

    private fun updateQuery(value: String) {
        query.value = value
    }

    private fun search(target: SearchTarget) {
        val currentQuery = query.value
        if (currentQuery.isBlank()) {
            rootActionChannel.trySend(LauncherRootAction.ShowMessage("Type a query first."))
            return
        }
        viewModelScope.launch {
            val action = installedAppSearch.resolve(
                target,
                currentQuery,
                currentInstalledApps.first { it != null }.orEmpty(),
            )
            if (action != null) rootActionChannel.send(LauncherRootAction.Open(action))
        }
    }

    private fun emitNative(action: LauncherAction) {
        rootActionChannel.trySend(LauncherRootAction.Open(action))
    }

    private fun runAppSelection(action: AppSelectionAction) {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            handleAppSelection(action)?.let { rootActionChannel.send(it) }
        }
    }

    private fun runHomeAppWrite(write: suspend () -> RepositoryWriteResult) {
        viewModelScope.launch { rootActionChannel.sendWriteResult(write()) }
    }

}

internal fun minuteTicks(clock: Clock): Flow<LocalDateTime> = flow {
    while (currentCoroutineContext().isActive) {
        val now = LocalDateTime.now(clock)
        emit(now)
        val millisUntilNextMinute = (60 - now.second) * 1_000L - now.nano / 1_000_000L
        delay(millisUntilNextMinute.coerceAtLeast(1L).milliseconds)
    }
}
