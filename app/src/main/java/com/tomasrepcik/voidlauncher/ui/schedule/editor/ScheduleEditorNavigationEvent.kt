package com.tomasrepcik.voidlauncher.ui.schedule.editor

internal sealed interface ScheduleEditorNavigationEvent {
    data object Back : ScheduleEditorNavigationEvent
}
