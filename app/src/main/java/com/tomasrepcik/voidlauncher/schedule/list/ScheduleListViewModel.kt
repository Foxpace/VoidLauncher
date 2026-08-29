package com.tomasrepcik.voidlauncher.schedule.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.voidlauncher.schedule.data.ScheduleRepository
import com.tomasrepcik.voidlauncher.launcher.LauncherRootAction
import com.tomasrepcik.voidlauncher.launcher.sendWriteResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduleListViewModel(
    private val schedules: ScheduleRepository,
) : ViewModel() {
    private val rootActionChannel = Channel<LauncherRootAction>(Channel.BUFFERED)
    private val navigationChannel = Channel<ScheduleListNavigationEvent>(Channel.BUFFERED)
    internal val rootActions = rootActionChannel.receiveAsFlow()
    internal val navigation = navigationChannel.receiveAsFlow()

    val uiState: StateFlow<ScheduleListUiState> = schedules.schedules.map { currentSchedules ->
        ScheduleListUiState(
            schedules = currentSchedules.orEmpty(),
            isLoading = currentSchedules == null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScheduleListUiState(),
    )

    fun onAction(action: ScheduleListAction) {
        when (action) {
            ScheduleListAction.Back -> navigationChannel.trySend(ScheduleListNavigationEvent.Back)
            ScheduleListAction.AddSchedule ->
                navigationChannel.trySend(ScheduleListNavigationEvent.Add)
            is ScheduleListAction.EditSchedule ->
                navigationChannel.trySend(ScheduleListNavigationEvent.Edit(action.id))
            is ScheduleListAction.DeleteSchedule -> viewModelScope.launch {
                rootActionChannel.sendWriteResult(schedules.delete(action.id))
            }
            is ScheduleListAction.SetScheduleEnabled -> viewModelScope.launch {
                val updated = action.schedule.copy(enabled = action.enabled)
                rootActionChannel.sendWriteResult(schedules.save(updated))
            }
        }
    }
}
