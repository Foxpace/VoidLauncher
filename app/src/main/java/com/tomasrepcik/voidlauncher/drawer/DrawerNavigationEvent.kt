package com.tomasrepcik.voidlauncher.drawer

internal sealed interface DrawerNavigationEvent {
    data object Back : DrawerNavigationEvent
    data object OpenCustomization : DrawerNavigationEvent
}
