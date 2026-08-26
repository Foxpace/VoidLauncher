package com.tomasrepcik.voidlauncher.domain.schedule

import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

const val MINUTES_PER_DAY = 24 * 60

data class AppSchedule(
    val id: String,
    val name: String,
    val days: Set<DayOfWeek>,
    val startMinute: Int,
    val endMinute: Int,
    val appKeys: Set<AppKey>,
    val enabled: Boolean = true,
)

/**
 * Owns the rules for overlapping, all-day, and overnight schedules.
 * Callers only choose the pinned apps, saved schedules, and point in time.
 */
class AppScheduleResolver {
    fun visibleApps(
        defaultApps: List<InstalledApp>,
        installedApps: List<InstalledApp>,
        schedules: List<AppSchedule>,
        at: LocalDateTime,
    ): ScheduledApps {
        val activeSchedules = schedules.filter { schedule -> schedule.enabled && schedule.isActiveAt(at) }
        if (activeSchedules.isEmpty()) return ScheduledApps(defaultApps, isScheduleActive = false)

        val visibleKeys = activeSchedules.flatMapTo(mutableSetOf(), AppSchedule::appKeys)
        return ScheduledApps(
            apps = installedApps.filter { app -> app.key in visibleKeys },
            isScheduleActive = true,
        )
    }
}

data class ScheduledApps(
    val apps: List<InstalledApp>,
    val isScheduleActive: Boolean,
)

fun AppSchedule.isActiveAt(dateTime: LocalDateTime): Boolean {
    if (days.isEmpty()) return false
    val minute = dateTime.toLocalTime().minuteOfDay
    val day = dateTime.dayOfWeek
    val safeStart = startMinute.coerceIn(0, MINUTES_PER_DAY - 1)
    val safeEnd = endMinute.coerceIn(0, MINUTES_PER_DAY - 1)

    return when {
        safeStart == safeEnd -> day in days
        safeStart < safeEnd -> day in days && minute in safeStart until safeEnd
        minute >= safeStart -> day in days
        else -> dateTime.minusDays(1).dayOfWeek in days && minute < safeEnd
    }
}

val LocalTime.minuteOfDay: Int
    get() = hour * 60 + minute
