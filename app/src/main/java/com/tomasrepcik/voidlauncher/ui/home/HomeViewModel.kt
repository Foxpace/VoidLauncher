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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val installedAppSearch: InstalledAppSearch,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val hintMessage = MutableStateFlow<String?>(null)
    private val actionChannel = Channel<LauncherAction>(capacity = Channel.BUFFERED)
    private val feedbackChannel = Channel<String>(capacity = Channel.BUFFERED)

    val actions = actionChannel.receiveAsFlow()
    val feedback = feedbackChannel.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        query,
        hintMessage,
        repository.state,
    ) { currentQuery, currentHint, repositoryState ->
        val launcher = (repositoryState as? LauncherRepositoryState.Ready)?.launcher
            ?: return@combine HomeUiState(query = currentQuery, isLoading = true)
        HomeUiState(
            query = currentQuery,
            homeApps = launcher.pinnedHomeApps,
            shortcuts = launcher.bottomShortcuts.sortedBy { it.slot.ordinal },
            hintMessage = currentHint,
            searchSuggestions = installedAppSearch.suggestions(currentQuery, launcher.installedApps),
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
            viewModelScope.launch { feedbackChannel.send("Type a query first.") }
        } else {
            sendAction(action)
        }
    }

    fun onAppHint() {
        val app = installedAppSearch.hint(query.value, currentInstalledApps())
        if (app != null) {
            hintMessage.value = "Try ${app.label}"
            viewModelScope.launch { feedbackChannel.send("Best app hint: ${app.label}") }
        } else {
            hintMessage.value = "No local app hint"
            viewModelScope.launch { feedbackChannel.send("No local app hint for that query.") }
        }
    }

    fun onAppClicked(app: InstalledApp) {
        sendAction(LauncherAction.LaunchInstalledApp(app))
    }

    fun onShortcutClicked(shortcut: ResolvedShortcut) {
        sendAction(LauncherAction.OpenShortcut(shortcut))
    }

    suspend fun removeHomeApp(app: InstalledApp) = repository.removeHomeApp(app.key)

    suspend fun renameHomeApp(app: InstalledApp, newLabel: String?) =
        repository.renameHomeApp(app.key, newLabel)

    suspend fun reorderHomeApps(fromIndex: Int, toIndex: Int) =
        repository.reorderHomeApps(fromIndex, toIndex)

    fun uninstallApp(app: InstalledApp) {
        sendAction(LauncherAction.UninstallApp(app))
    }

    private fun sendAction(action: LauncherAction) {
        viewModelScope.launch {
            actionChannel.send(action)
        }
    }

    private fun currentInstalledApps(): List<InstalledApp> =
        (repository.state.value as? LauncherRepositoryState.Ready)?.launcher?.installedApps.orEmpty()

    companion object {
        fun provideFactory(
            repository: LauncherRepository,
            installedAppSearch: InstalledAppSearch,
        ) = viewModelFactory {
            initializer {
                HomeViewModel(repository, installedAppSearch)
            }
        }
    }
}
