package com.tomasrepcik.voidlauncher.ui.customization.shortcutpicker

import com.tomasrepcik.voidlauncher.data.model.InstalledApp

data class ShortcutPickerUiState(
    val query: String = "",
    val apps: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface ShortcutPickerAction {
    data object Back : ShortcutPickerAction
    data class QueryChanged(val value: String) : ShortcutPickerAction
    data class SelectApp(val app: InstalledApp) : ShortcutPickerAction
    data object SelectContacts : ShortcutPickerAction
    data object SelectCamera : ShortcutPickerAction
}
