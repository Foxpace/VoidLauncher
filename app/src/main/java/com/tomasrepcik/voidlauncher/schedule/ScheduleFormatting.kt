package com.tomasrepcik.voidlauncher.schedule

import com.tomasrepcik.voidlauncher.schedule.data.AppSchedule
import com.tomasrepcik.voidlauncher.schedule.data.MINUTES_PER_DAY
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

internal const val MINUTES_PER_HOUR = 60
internal val EVERY_DAY = DayOfWeek.entries.toSet()
internal val WEEKDAYS = setOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
)
internal val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

internal fun AppSchedule.summary(): String =
    days.sortedBy(DayOfWeek::getValue).joinToString(", ") { it.shortName() }

internal fun DayOfWeek.shortName(): String =
    getDisplayName(TextStyle.SHORT, Locale.getDefault())

internal fun Int.asTime(): String {
    val minute = coerceIn(0, MINUTES_PER_DAY - 1)
    return LocalTime.of(minute / MINUTES_PER_HOUR, minute % MINUTES_PER_HOUR)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}
