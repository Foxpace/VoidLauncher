package com.tomasrepcik.voidlauncher.shortcuts.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.voidlauncher.launcher.ShortcutSelection
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot
import com.tomasrepcik.voidlauncher.appcatalog.data.InstalledAppsRepository
import com.tomasrepcik.voidlauncher.storage.launcher.RepositoryWriteResult
import com.tomasrepcik.voidlauncher.shortcuts.data.ShortcutRepository
import com.tomasrepcik.voidlauncher.appcatalog.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.launcher.LauncherRootAction
import com.tomasrepcik.voidlauncher.launcher.sendWriteResult
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
    private val saving = MutableStateFlow(false)
    private val rootActionChannel = Channel<LauncherRootAction>(Channel.BUFFERED)
    internal val rootActions = rootActionChannel.receiveAsFlow()

    val uiState: StateFlow<ShortcutPickerUiState> = combine(
        installedApps.apps,
        query,
        saving,
    ) { currentApps, currentQuery, isSaving ->
        val installedApps = currentApps
            ?: return@combine ShortcutPickerUiState(
                query = currentQuery,
                isLoading = true,
                isSaving = isSaving,
            )
        val filtered = installedAppSearch.filter(currentQuery, installedApps)
        ShortcutPickerUiState(
            query = currentQuery,
            apps = filtered,
            isLoading = false,
            isSaving = isSaving,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShortcutPickerUiState(isLoading = true),
    )

    fun onAction(action: ShortcutPickerAction) {
        when (action) {
            ShortcutPickerAction.Back ->
                rootActionChannel.trySend(LauncherRootAction.CloseScreen)
            is ShortcutPickerAction.QueryChanged -> query.value = action.value
            is ShortcutPickerAction.SelectApp -> select(ShortcutSelection.AppShortcut(action.app.key))
            ShortcutPickerAction.SelectContacts -> select(ShortcutSelection.SystemContacts)
            ShortcutPickerAction.SelectCamera -> select(ShortcutSelection.SystemCamera)
        }
    }

    private fun select(selection: ShortcutSelection) {
        if (saving.value) return
        saving.value = true
        viewModelScope.launch {
            val result = shortcuts.save(slot, selection)
            rootActionChannel.sendWriteResult(
                result,
                sendCompletion = true,
            )
            if (result is RepositoryWriteResult.Failed) saving.value = false
        }
    }
}
