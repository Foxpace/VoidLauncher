package com.tomasrepcik.voidlauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.domain.search.SearchResolution
import com.tomasrepcik.voidlauncher.domain.search.SearchResolver
import com.tomasrepcik.voidlauncher.domain.search.matchesSearchQuery
import com.tomasrepcik.voidlauncher.domain.search.startsWithSearchQuery
import com.tomasrepcik.voidlauncher.ui.navigation.LauncherCommand
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val query: String = "",
    val homeApps: List<InstalledApp> = emptyList(),
    val shortcuts: List<ResolvedShortcut> = emptyList(),
    val hintMessage: String? = null,
    val searchSuggestions: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val repository: LauncherRepository,
    private val searchResolver: SearchResolver,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val hintMessage = MutableStateFlow<String?>(null)
    private val commandChannel = Channel<LauncherCommand>(capacity = Channel.BUFFERED)

    val commands = commandChannel.receiveAsFlow()

    private val installedApps = repository.observeInstalledApps().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
    private val areInstalledAppsLoaded = repository.observeInstalledApps()
        .map { true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            query,
            hintMessage,
            installedApps,
            areInstalledAppsLoaded,
        ) { q, h, apps, isLoaded ->
            HomeInputs(
                query = q,
                hintMessage = h,
                apps = apps,
                isLoaded = isLoaded,
            )
        },
        repository.observePinnedHomeApps(),
        repository.observeBottomShortcuts(),
    ) { inputs, homeApps, shortcuts ->
        val suggestions = if (inputs.query.isNotBlank()) {
            inputs.apps.filter { app ->
                matchesSearchQuery(app.label, inputs.query)
            }.sortedBy { app ->
                if (startsWithSearchQuery(app.label, inputs.query)) 0 else 1
            }.take(5)
        } else {
            emptyList()
        }
        HomeUiState(
            query = inputs.query,
            homeApps = homeApps,
            shortcuts = shortcuts.sortedBy { it.slot.ordinal },
            hintMessage = inputs.hintMessage,
            searchSuggestions = suggestions,
            isLoading = !inputs.isLoaded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true),
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaults()
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
        hintMessage.value = null
    }

    fun onPrimarySearch() {
        dispatch(searchResolver.resolvePrimary(query.value, installedApps.value))
    }

    fun onBrowserSearch() {
        dispatch(searchResolver.resolveBrowser(query.value))
    }

    fun onPlayStoreSearch() {
        dispatch(searchResolver.resolvePlayStore(query.value))
    }

    fun onMapsSearch() {
        dispatch(searchResolver.resolveMaps(query.value))
    }

    fun onAppHint() {
        when (val resolution = searchResolver.resolveHint(query.value, installedApps.value)) {
            is SearchResolution.AppHint -> {
                hintMessage.value = "Try ${resolution.app.label}"
                sendCommand(LauncherCommand.ShowMessage("Best app hint: ${resolution.app.label}"))
            }

            is SearchResolution.NoMatch -> {
                hintMessage.value = "No local app hint"
                sendCommand(LauncherCommand.ShowMessage("No local app hint for that query."))
            }

            else -> Unit
        }
    }

    fun onAppClicked(app: InstalledApp) {
        sendCommand(LauncherCommand.LaunchInstalledApp(app))
    }

    fun onShortcutClicked(shortcut: ResolvedShortcut) {
        sendCommand(LauncherCommand.OpenShortcut(shortcut))
    }

    fun removeHomeApp(app: InstalledApp) {
        viewModelScope.launch {
            repository.removeHomeApp(app.key)
        }
    }

    fun renameHomeApp(app: InstalledApp, newLabel: String?) {
        viewModelScope.launch {
            repository.renameHomeApp(app.key, newLabel)
        }
    }

    fun reorderHomeApps(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.reorderHomeApps(fromIndex, toIndex)
        }
    }

    fun uninstallApp(app: InstalledApp) {
        viewModelScope.launch {
            commandChannel.send(LauncherCommand.UninstallApp(app))
        }
    }

    private fun dispatch(resolution: SearchResolution) {
        when (resolution) {
            is SearchResolution.LaunchInstalledApp -> {
                sendCommand(LauncherCommand.LaunchInstalledApp(resolution.app))
            }

            is SearchResolution.PlayStoreSearch -> {
                sendCommand(LauncherCommand.OpenPlayStoreSearch(resolution.query))
            }

            is SearchResolution.MapsSearch -> {
                sendCommand(LauncherCommand.OpenMapsSearch(resolution.query))
            }

            is SearchResolution.WebSearch -> {
                sendCommand(LauncherCommand.OpenWebSearch(resolution.query))
            }

            is SearchResolution.NoMatch -> {
                sendCommand(LauncherCommand.ShowMessage("Type a query first."))
            }

            is SearchResolution.AppHint -> {
                hintMessage.value = "Try ${resolution.app.label}"
            }
        }
    }

    private fun sendCommand(command: LauncherCommand) {
        viewModelScope.launch {
            commandChannel.send(command)
        }
    }

    companion object {
        fun provideFactory(
            repository: LauncherRepository,
            searchResolver: SearchResolver,
        ) = viewModelFactory {
            initializer {
                HomeViewModel(repository, searchResolver)
            }
        }
    }
}

private data class HomeInputs(
    val query: String,
    val hintMessage: String?,
    val apps: List<InstalledApp>,
    val isLoaded: Boolean,
)
