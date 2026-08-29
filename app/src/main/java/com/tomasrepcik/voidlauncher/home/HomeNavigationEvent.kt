package com.tomasrepcik.voidlauncher.home

internal sealed interface HomeNavigationEvent {
    data object OpenDrawer : HomeNavigationEvent
    data object OpenSchedules : HomeNavigationEvent
}
