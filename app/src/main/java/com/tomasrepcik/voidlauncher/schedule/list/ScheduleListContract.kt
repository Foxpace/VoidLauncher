package com.tomasrepcik.voidlauncher.schedule.list

import com.tomasrepcik.voidlauncher.schedule.data.AppSchedule

data class ScheduleListUiState(
    val schedules: List<AppSchedule> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface ScheduleListAction {
    data object Back : ScheduleListAction
    data object AddSchedule : ScheduleListAction
    data class EditSchedule(val id: String) : ScheduleListAction
    data class DeleteSchedule(val id: String) : ScheduleListAction
    data class SetScheduleEnabled(
        val schedule: AppSchedule,
        val enabled: Boolean,
    ) : ScheduleListAction
}
