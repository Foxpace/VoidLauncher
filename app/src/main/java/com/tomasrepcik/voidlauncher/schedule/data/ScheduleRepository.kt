package com.tomasrepcik.voidlauncher.schedule.data

import com.tomasrepcik.voidlauncher.launcher.error.AppOperation
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherRepository
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherStorage
import com.tomasrepcik.voidlauncher.storage.launcher.readyLauncherState
import com.tomasrepcik.voidlauncher.storage.launcher.writeToStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ScheduleRepository internal constructor(
    launcher: LauncherRepository,
    private val storage: LauncherStorage,
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
