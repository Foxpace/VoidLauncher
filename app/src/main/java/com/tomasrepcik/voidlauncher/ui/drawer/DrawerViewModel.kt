package com.tomasrepcik.voidlauncher.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.data.repository.RepositoryMutationOutcome
import com.tomasrepcik.voidlauncher.domain.action.LauncherAction
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.ui.LauncherUiEffect
import com.tomasrepcik.voidlauncher.ui.sendOutcome
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DrawerUiState(
    val query: String = "",
    val apps: List<InstalledApp> = emptyList(),
    val pinnedAppKeys: Set<AppKey> = emptySet(),
    val isLoading: Boolean = true,
)

class DrawerViewModel(
    private val repository: LauncherRepository,
    installedAppSearch: InstalledAppSearch,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val effectChannel = Channel<LauncherUiEffect>(capacity = Channel.BUFFERED)

    internal val effects = effectChannel.receiveAsFlow()

    val uiState: StateFlow<DrawerUiState> = combine(
        repository.state,
        query,
    ) { repositoryState, currentQuery ->
        val launcher = (repositoryState as? LauncherRepositoryState.Ready)?.launcher
            ?: return@combine DrawerUiState(query = currentQuery, isLoading = true)
        val filteredApps = installedAppSearch.filter(currentQuery, launcher.installedApps)
        DrawerUiState(
            query = currentQuery,
            apps = filteredApps,
            pinnedAppKeys = launcher.pinnedAppKeys,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DrawerUiState(isLoading = true),
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onAppClicked(app: InstalledApp) {
        viewModelScope.launch {
            effectChannel.send(LauncherUiEffect.Action(LauncherAction.LaunchInstalledApp(app)))
        }
    }

    fun addHomeApp(app: InstalledApp) = mutate { repository.addHomeApp(app.key) }

    fun removeHomeApp(app: InstalledApp) = mutate { repository.removeHomeApp(app.key) }

    fun uninstallApp(app: InstalledApp) {
        viewModelScope.launch {
            effectChannel.send(LauncherUiEffect.Action(LauncherAction.UninstallApp(app)))
        }
    }

    private fun mutate(mutation: suspend () -> RepositoryMutationOutcome) {
        viewModelScope.launch { effectChannel.sendOutcome(mutation()) }
    }

    companion object {
        fun provideFactory(
            repository: LauncherRepository,
            installedAppSearch: InstalledAppSearch,
        ) = viewModelFactory {
            initializer {
                DrawerViewModel(repository, installedAppSearch)
            }
        }
    }
}
