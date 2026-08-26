package com.tomasrepcik.voidlauncher.ui.customization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.voidlauncher.data.repository.ShortcutRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

class CustomizationViewModel(
    shortcuts: ShortcutRepository,
) : ViewModel() {
    private val navigationChannel = Channel<CustomizationNavigationEvent>(Channel.BUFFERED)
    internal val navigation = navigationChannel.receiveAsFlow()

    val uiState: StateFlow<CustomizationUiState> =
        shortcuts.shortcuts.map { currentShortcuts ->
            CustomizationUiState(
                shortcuts = currentShortcuts.orEmpty().sortedBy { it.slot.ordinal },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CustomizationUiState(),
        )

    fun onAction(action: CustomizationAction) {
        val event = when (action) {
            CustomizationAction.Back -> CustomizationNavigationEvent.Back
            is CustomizationAction.EditShortcut -> CustomizationNavigationEvent.EditShortcut(action.slot)
            CustomizationAction.OpenSchedules -> CustomizationNavigationEvent.OpenSchedules
            CustomizationAction.ShowNavigationTutorial ->
                CustomizationNavigationEvent.ShowNavigationTutorial
        }
        navigationChannel.trySend(event)
    }
}
