package com.tomasrepcik.voidlauncher.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.domain.search.matchesSearchQuery
import com.tomasrepcik.voidlauncher.ui.navigation.LauncherCommand
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
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val commandChannel = Channel<LauncherCommand>(capacity = Channel.BUFFERED)

    val commands = commandChannel.receiveAsFlow()

    val uiState: StateFlow<DrawerUiState> = combine(
        repository.observeInstalledApps(),
        repository.observePinnedAppKeys(),
        query,
    ) { apps, pinnedKeys, currentQuery ->
        val filteredApps = if (currentQuery.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                matchesSearchQuery(app.label, currentQuery)
            }
        }
        DrawerUiState(
            query = currentQuery,
            apps = filteredApps,
            pinnedAppKeys = pinnedKeys,
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
            commandChannel.send(LauncherCommand.LaunchInstalledApp(app))
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
            commandChannel.send(LauncherCommand.UninstallApp(app))
        }
    }

    companion object {
        fun provideFactory(repository: LauncherRepository) = viewModelFactory {
            initializer {
                DrawerViewModel(repository)
            }
        }
    }
}
