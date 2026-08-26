package com.tomasrepcik.voidlauncher.ui.customization.shortcutpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.data.repository.InstalledAppsRepository
import com.tomasrepcik.voidlauncher.data.repository.ShortcutRepository
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.ui.LauncherRootAction
import com.tomasrepcik.voidlauncher.ui.sendWriteResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShortcutPickerViewModel(
    private val slot: ShortcutSlot,
    installedApps: InstalledAppsRepository,
    private val shortcuts: ShortcutRepository,
    installedAppSearch: InstalledAppSearch,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val rootActionChannel = Channel<LauncherRootAction>(Channel.BUFFERED)
    private val navigationChannel = Channel<ShortcutPickerNavigationEvent>(Channel.BUFFERED)
    internal val rootActions = rootActionChannel.receiveAsFlow()
    internal val navigation = navigationChannel.receiveAsFlow()

    val uiState: StateFlow<ShortcutPickerUiState> = combine(
        installedApps.apps,
        query,
    ) { currentApps, currentQuery ->
        val installedApps = currentApps
            ?: return@combine ShortcutPickerUiState(query = currentQuery, isLoading = true)
        val filtered = installedAppSearch.filter(currentQuery, installedApps)
        ShortcutPickerUiState(
            query = currentQuery,
            apps = filtered,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShortcutPickerUiState(isLoading = true),
    )

    fun onAction(action: ShortcutPickerAction) {
        when (action) {
            ShortcutPickerAction.Back ->
                navigationChannel.trySend(ShortcutPickerNavigationEvent.Back)
            is ShortcutPickerAction.QueryChanged -> query.value = action.value
            is ShortcutPickerAction.SelectApp -> select(ShortcutSelection.AppShortcut(action.app.key))
            ShortcutPickerAction.SelectContacts -> select(ShortcutSelection.SystemContacts)
            ShortcutPickerAction.SelectCamera -> select(ShortcutSelection.SystemCamera)
        }
    }

    private fun select(selection: ShortcutSelection) {
        viewModelScope.launch {
            rootActionChannel.sendWriteResult(
                shortcuts.save(slot, selection),
                sendCompletion = true,
            )
        }
    }
}
