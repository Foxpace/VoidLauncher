package com.tomasrepcik.voidlauncher.ui.schedule.list

internal sealed interface ScheduleListNavigationEvent {
    data object Back : ScheduleListNavigationEvent
    data object Add : ScheduleListNavigationEvent
    data class Edit(val id: String) : ScheduleListNavigationEvent
}
