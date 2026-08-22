package com.tomasrepcik.voidlauncher.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.domain.action.LauncherAction
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
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
    private val actionChannel = Channel<LauncherAction>(capacity = Channel.BUFFERED)

    val actions = actionChannel.receiveAsFlow()

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
            actionChannel.send(LauncherAction.LaunchInstalledApp(app))
        }
    }

    fun addHomeApp(app: InstalledApp) {
        viewModelScope.launch {
            repository.addHomeApp(app.key)
        }
    }

    fun removeHomeApp(app: InstalledApp) {
        viewModelScope.launch {
            repository.removeHomeApp(app.key)
        }
    }

    fun uninstallApp(app: InstalledApp) {
        viewModelScope.launch {
            actionChannel.send(LauncherAction.UninstallApp(app))
        }
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
