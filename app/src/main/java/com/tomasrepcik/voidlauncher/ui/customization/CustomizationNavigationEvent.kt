package com.tomasrepcik.voidlauncher.ui.customization

import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot

internal sealed interface CustomizationNavigationEvent {
    data object Back : CustomizationNavigationEvent
    data class EditShortcut(val slot: ShortcutSlot) : CustomizationNavigationEvent
    data object OpenSchedules : CustomizationNavigationEvent
    data object ShowNavigationTutorial : CustomizationNavigationEvent
}
