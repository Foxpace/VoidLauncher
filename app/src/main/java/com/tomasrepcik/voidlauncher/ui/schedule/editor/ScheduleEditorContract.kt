package com.tomasrepcik.voidlauncher.ui.schedule.editor

import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import com.tomasrepcik.voidlauncher.ui.schedule.WEEKDAYS
import java.time.DayOfWeek

data class ScheduleEditorArgs(val scheduleId: String?)

data class ScheduleEditorUiState(
    val id: String? = null,
    val name: String = DEFAULT_SCHEDULE_NAME,
    val days: Set<DayOfWeek> = WEEKDAYS,
    val startMinute: Int = DEFAULT_START_MINUTE,
    val endMinute: Int = DEFAULT_END_MINUTE,
    val selectedAppKeys: Set<AppKey> = emptySet(),
    val enabled: Boolean = true,
    val appQuery: String = "",
    val isAppPickerOpen: Boolean = false,
    val installedApps: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
) {
    val canSave: Boolean
        get() = days.isNotEmpty() && selectedAppKeys.isNotEmpty() && !isSaving

    internal fun reduce(action: ScheduleEditorAction.Update): ScheduleEditorUiState = when (action) {
        is ScheduleEditorAction.ChangeName -> copy(name = action.value)
        is ScheduleEditorAction.ToggleDay -> copy(days = days.toggle(action.day))
        is ScheduleEditorAction.ChangeDays -> copy(days = action.days)
        is ScheduleEditorAction.ChangeStartTime -> copy(startMinute = action.minute)
        is ScheduleEditorAction.ChangeEndTime -> copy(endMinute = action.minute)
        is ScheduleEditorAction.ToggleApp -> copy(selectedAppKeys = selectedAppKeys.toggle(action.key))
        is ScheduleEditorAction.ChangeAppQuery -> copy(appQuery = action.value)
        is ScheduleEditorAction.SetEnabled -> copy(enabled = action.enabled)
        ScheduleEditorAction.OpenAppPicker -> copy(isAppPickerOpen = true)
        ScheduleEditorAction.CloseAppPicker -> copy(isAppPickerOpen = false, appQuery = "")
    }
}

sealed interface ScheduleEditorAction {
    data object Back : ScheduleEditorAction
    data object SaveSchedule : ScheduleEditorAction

    sealed interface Update : ScheduleEditorAction

    data class ChangeName(val value: String) : Update
    data class ToggleDay(val day: DayOfWeek) : Update
    data class ChangeDays(val days: Set<DayOfWeek>) : Update
    data class ChangeStartTime(val minute: Int) : Update
    data class ChangeEndTime(val minute: Int) : Update
    data class ToggleApp(val key: AppKey) : Update
    data class ChangeAppQuery(val value: String) : Update
    data class SetEnabled(val enabled: Boolean) : Update
    data object OpenAppPicker : Update
    data object CloseAppPicker : Update
}

internal fun AppSchedule.toEditorState() = ScheduleEditorUiState(
    id = id,
    name = name,
    days = days,
    startMinute = startMinute,
    endMinute = endMinute,
    selectedAppKeys = appKeys,
    enabled = enabled,
    isLoading = false,
)

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value

internal const val DEFAULT_SCHEDULE_NAME = "My schedule"
private const val DEFAULT_START_MINUTE = 9 * 60
private const val DEFAULT_END_MINUTE = 17 * 60
