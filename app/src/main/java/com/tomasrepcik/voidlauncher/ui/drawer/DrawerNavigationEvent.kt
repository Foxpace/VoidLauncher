package com.tomasrepcik.voidlauncher.ui.drawer

internal sealed interface DrawerNavigationEvent {
    data object Back : DrawerNavigationEvent
    data object OpenCustomization : DrawerNavigationEvent
}
