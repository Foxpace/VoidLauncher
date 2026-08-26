package com.tomasrepcik.voidlauncher.ui.customization

import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot

data class CustomizationUiState(
    val shortcuts: List<ResolvedShortcut> = emptyList(),
)

sealed interface CustomizationAction {
    data object Back : CustomizationAction
    data class EditShortcut(val slot: ShortcutSlot) : CustomizationAction
    data object OpenSchedules : CustomizationAction
    data object ShowNavigationTutorial : CustomizationAction
}
