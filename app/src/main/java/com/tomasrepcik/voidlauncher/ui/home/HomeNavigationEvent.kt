package com.tomasrepcik.voidlauncher.ui.home

internal sealed interface HomeNavigationEvent {
    data object OpenDrawer : HomeNavigationEvent
    data object OpenSchedules : HomeNavigationEvent
}
