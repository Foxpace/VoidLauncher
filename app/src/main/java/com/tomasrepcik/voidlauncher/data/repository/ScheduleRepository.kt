package com.tomasrepcik.voidlauncher.data.repository

import com.tomasrepcik.voidlauncher.domain.error.AppOperation
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ScheduleRepository internal constructor(
    launcher: LauncherRepository,
    private val storage: ScheduleStorage = launcher.storage,
) {
    val schedules: Flow<List<AppSchedule>?> = launcher.readyLauncherState()
        .map { state -> state?.schedules }
        .distinctUntilChanged()

    suspend fun save(schedule: AppSchedule) = writeToStorage(AppOperation.SAVE_SCHEDULE) {
        storage.saveSchedule(schedule)
    }

    suspend fun delete(id: String) = writeToStorage(AppOperation.DELETE_SCHEDULE) {
        storage.deleteSchedule(id)
    }
}
