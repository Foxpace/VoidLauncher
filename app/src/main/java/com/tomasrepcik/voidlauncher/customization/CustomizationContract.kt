package com.tomasrepcik.voidlauncher.customization

import com.tomasrepcik.voidlauncher.launcher.ResolvedShortcut
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot

data class CustomizationUiState(
    val shortcuts: List<ResolvedShortcut> = emptyList(),
)

sealed interface CustomizationAction {
    data object Back : CustomizationAction
    data class EditShortcut(val slot: ShortcutSlot) : CustomizationAction
    data object OpenSchedules : CustomizationAction
    data object ShowNavigationTutorial : CustomizationAction
}
