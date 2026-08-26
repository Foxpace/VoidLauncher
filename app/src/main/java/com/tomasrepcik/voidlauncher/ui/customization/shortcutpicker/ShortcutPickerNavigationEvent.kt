package com.tomasrepcik.voidlauncher.ui.customization.shortcutpicker

internal sealed interface ShortcutPickerNavigationEvent {
    data object Back : ShortcutPickerNavigationEvent
}
