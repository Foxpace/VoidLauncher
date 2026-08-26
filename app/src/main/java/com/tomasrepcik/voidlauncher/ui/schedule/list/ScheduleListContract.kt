package com.tomasrepcik.voidlauncher.ui.schedule.list

import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule

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
