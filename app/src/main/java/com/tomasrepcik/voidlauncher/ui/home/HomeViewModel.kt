package com.tomasrepcik.voidlauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.domain.action.LauncherAction
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.domain.search.SearchTarget
import com.tomasrepcik.voidlauncher.domain.schedule.AppScheduleResolver
import com.tomasrepcik.voidlauncher.ui.LauncherUiEffect
import com.tomasrepcik.voidlauncher.ui.sendOutcome
import java.time.LocalDateTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class HomeUiState(
    val query: String = "",
    val homeApps: List<InstalledApp> = emptyList(),
    val shortcuts: List<ResolvedShortcut> = emptyList(),
    val hintMessage: String? = null,
    val searchSuggestions: List<InstalledApp> = emptyList(),
    val isScheduleActive: Boolean = false,
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val repository: LauncherRepository,
    private val installedAppSearch: InstalledAppSearch,
    scheduleResolver: AppScheduleResolver = AppScheduleResolver(),
    currentTime: Flow<LocalDateTime> = flowOf(LocalDateTime.now()),
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val hintMessage = MutableStateFlow<String?>(null)
    private val effectChannel = Channel<LauncherUiEffect>(capacity = Channel.BUFFERED)

    internal val effects = effectChannel.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        query,
        hintMessage,
        repository.state,
        currentTime,
    ) { currentQuery, currentHint, repositoryState, now ->
        val launcher = (repositoryState as? LauncherRepositoryState.Ready)?.launcher
            ?: return@combine HomeUiState(query = currentQuery, isLoading = true)
        val scheduledApps = scheduleResolver.visibleApps(
            defaultApps = launcher.pinnedHomeApps,
            installedApps = launcher.installedApps,
            schedules = launcher.schedules,
            at = now,
        )
        HomeUiState(
            query = currentQuery,
            homeApps = scheduledApps.apps,
            shortcuts = launcher.bottomShortcuts.sortedBy { it.slot.ordinal },
            hintMessage = currentHint,
            searchSuggestions = installedAppSearch.suggestions(currentQuery, launcher.installedApps),
            isScheduleActive = scheduledApps.isScheduleActive,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true),
    )

    fun onQueryChange(value: String) {
        query.value = value
        hintMessage.value = null
    }

    fun onSearch(target: SearchTarget) {
        val action = installedAppSearch.resolve(target, query.value, currentInstalledApps())
        if (action == null) {
            effectChannel.trySend(LauncherUiEffect.Feedback("Type a query first."))
        } else {
            effectChannel.trySend(LauncherUiEffect.Action(action))
        }
    }

    fun onAppHint() {
        val app = installedAppSearch.hint(query.value, currentInstalledApps())
        if (app != null) {
            hintMessage.value = "Try ${app.label}"
            effectChannel.trySend(LauncherUiEffect.Feedback("Best app hint: ${app.label}"))
        } else {
            hintMessage.value = "No local app hint"
            effectChannel.trySend(LauncherUiEffect.Feedback("No local app hint for that query."))
        }
    }

    fun onAppClicked(app: InstalledApp) {
        effectChannel.trySend(LauncherUiEffect.Action(LauncherAction.LaunchInstalledApp(app)))
    }

    fun onShortcutClicked(shortcut: ResolvedShortcut) {
        effectChannel.trySend(LauncherUiEffect.Action(LauncherAction.OpenShortcut(shortcut)))
    }

    fun removeHomeApp(app: InstalledApp) {
        viewModelScope.launch { effectChannel.sendOutcome(repository.removeHomeApp(app.key)) }
    }

    fun renameHomeApp(app: InstalledApp, newLabel: String?) {
        viewModelScope.launch {
            effectChannel.sendOutcome(repository.renameHomeApp(app.key, newLabel))
        }
    }

    fun reorderHomeApps(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            effectChannel.sendOutcome(repository.reorderHomeApps(fromIndex, toIndex))
        }
    }

    fun uninstallApp(app: InstalledApp) {
        effectChannel.trySend(LauncherUiEffect.Action(LauncherAction.UninstallApp(app)))
    }

    private fun currentInstalledApps(): List<InstalledApp> =
        (repository.state.value as? LauncherRepositoryState.Ready)?.launcher?.installedApps.orEmpty()

    companion object {
        fun provideFactory(
            repository: LauncherRepository,
            installedAppSearch: InstalledAppSearch,
        ) = viewModelFactory {
            initializer {
                HomeViewModel(
                    repository = repository,
                    installedAppSearch = installedAppSearch,
                    currentTime = minuteTicks(),
                )
            }
        }
    }
}

private fun minuteTicks(): Flow<LocalDateTime> = flow {
    while (currentCoroutineContext().isActive) {
        val now = LocalDateTime.now()
        emit(now)
        val millisUntilNextMinute = (60 - now.second) * 1_000L - now.nano / 1_000_000L
        delay(millisUntilNextMinute.coerceAtLeast(1L).milliseconds)
    }
}
