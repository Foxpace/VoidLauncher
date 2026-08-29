package com.tomasrepcik.voidlauncher.shortcuts.picker

import com.tomasrepcik.voidlauncher.launcher.InstalledApp

data class ShortcutPickerUiState(
    val query: String = "",
    val apps: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
)

sealed interface ShortcutPickerAction {
    data object Back : ShortcutPickerAction
    data class QueryChanged(val value: String) : ShortcutPickerAction
    data class SelectApp(val app: InstalledApp) : ShortcutPickerAction
    data object SelectContacts : ShortcutPickerAction
    data object SelectCamera : ShortcutPickerAction
}
