package com.tomasrepcik.voidlauncher.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferencesMutation
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.ui.LauncherUiEffect
import com.tomasrepcik.voidlauncher.ui.sendOutcome
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal class LauncherAppViewModel(
    private val repository: LauncherRepository,
) : ViewModel() {
    val repositoryState: StateFlow<LauncherRepositoryState> = repository.state

    private val effectChannel = Channel<LauncherUiEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun finishNavigationTutorial() {
        viewModelScope.launch {
            effectChannel.sendOutcome(
                repository.mutatePreferences(
                    LauncherPreferencesMutation.MarkNavigationTutorialSeen,
                ),
            )
        }
    }

    fun retryInitialization() = repository.retryInitialization()

    companion object {
        fun provideFactory(repository: LauncherRepository) = viewModelFactory {
            initializer { LauncherAppViewModel(repository) }
        }
    }
}
