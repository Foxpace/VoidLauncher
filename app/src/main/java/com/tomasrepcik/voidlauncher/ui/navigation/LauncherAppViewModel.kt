package com.tomasrepcik.voidlauncher.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.voidlauncher.data.repository.LauncherStatusRepository
import com.tomasrepcik.voidlauncher.data.repository.PreferencesRepository
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.ui.LauncherRootAction
import com.tomasrepcik.voidlauncher.ui.sendWriteResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class LauncherAppViewModel(
    private val status: LauncherStatusRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<LauncherAppUiState> = combine(
        status.state,
        preferences.preferences,
    ) { repositoryState, currentPreferences ->
        LauncherAppUiState(
            isLoading = repositoryState is LauncherRepositoryState.Loading,
            initializationError = (repositoryState as? LauncherRepositoryState.InitializationError)
                ?.error,
            hasSeenNavigationTutorial = currentPreferences?.hasSeenNavigationTutorial,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherAppUiState(),
    )

    private val rootActionChannel = Channel<LauncherRootAction>(Channel.BUFFERED)
    val rootActions = rootActionChannel.receiveAsFlow()

    fun onAction(action: LauncherAppAction) {
        when (action) {
            LauncherAppAction.MarkNavigationTutorialSeen -> viewModelScope.launch {
                rootActionChannel.sendWriteResult(preferences.markNavigationTutorialSeen())
            }
            LauncherAppAction.RetryInitialization -> status.retryInitialization()
        }
    }
}
