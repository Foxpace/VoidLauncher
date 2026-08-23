package com.tomasrepcik.voidlauncher.domain.schedule

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.testing.installedApp
import java.time.DayOfWeek
import java.time.LocalDateTime
import org.junit.Test

class AppScheduleResolverTest {
    private val resolver = AppScheduleResolver()
    private val mail = installedApp("Mail")
    private val music = installedApp("Music")
    private val games = installedApp("Games")
    private val installedApps = listOf(games, mail, music)

    @Test
    fun noActiveScheduleReturnsDefaultHomeApps() {
        val schedule = schedule(
            days = setOf(DayOfWeek.MONDAY),
            startMinute = 9 * 60,
            endMinute = 17 * 60,
            apps = setOf(mail),
        )

        val result = resolver.visibleApps(
            defaultApps = listOf(music),
            installedApps = installedApps,
            schedules = listOf(schedule),
            at = dateTime(DayOfWeek.TUESDAY, 10),
        )

        assertThat(result.apps).containsExactly(music)
        assertThat(result.isScheduleActive).isFalse()
    }

    @Test
    fun overlappingSchedulesCombineTheirSelectedApps() {
        val work = schedule(apps = setOf(mail))
        val lunch = schedule(startMinute = 12 * 60, endMinute = 13 * 60, apps = setOf(music))

        val result = resolver.visibleApps(
            defaultApps = listOf(games),
            installedApps = installedApps,
            schedules = listOf(work, lunch),
            at = dateTime(DayOfWeek.MONDAY, 12, 30),
        )

        assertThat(result.apps).containsExactly(mail, music).inOrder()
        assertThat(result.isScheduleActive).isTrue()
    }

    @Test
    fun overnightScheduleUsesPreviousSelectedDayAfterMidnight() {
        val evening = schedule(
            days = setOf(DayOfWeek.FRIDAY),
            startMinute = 22 * 60,
            endMinute = 2 * 60,
            apps = setOf(music),
        )

        val result = resolver.visibleApps(
            defaultApps = listOf(games),
            installedApps = installedApps,
            schedules = listOf(evening),
            at = dateTime(DayOfWeek.SATURDAY, 1),
        )

        assertThat(result.apps).containsExactly(music)
        assertThat(result.isScheduleActive).isTrue()
    }

    @Test
    fun equalTimesCreateAnAllDaySchedule() {
        val allDay = schedule(startMinute = 0, endMinute = 0, apps = setOf(mail))

        assertThat(allDay.isActiveAt(dateTime(DayOfWeek.MONDAY, 23, 59))).isTrue()
        assertThat(allDay.isActiveAt(dateTime(DayOfWeek.TUESDAY, 0))).isFalse()
    }

    @Test
    fun disabledScheduleDoesNotReplaceDefaultApps() {
        val disabled = schedule(apps = setOf(mail)).copy(enabled = false)

        val result = resolver.visibleApps(
            defaultApps = listOf(games),
            installedApps = installedApps,
            schedules = listOf(disabled),
            at = dateTime(DayOfWeek.MONDAY, 10),
        )

        assertThat(result.apps).containsExactly(games)
        assertThat(result.isScheduleActive).isFalse()
    }

    private fun schedule(
        days: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
        startMinute: Int = 9 * 60,
        endMinute: Int = 17 * 60,
        apps: Set<com.tomasrepcik.voidlauncher.data.model.InstalledApp>,
    ) = AppSchedule(
        id = "schedule-$startMinute-$endMinute",
        name = "Schedule",
        days = days,
        startMinute = startMinute,
        endMinute = endMinute,
        appKeys = apps.mapTo(mutableSetOf()) { it.key },
    )

    private fun dateTime(day: DayOfWeek, hour: Int, minute: Int = 0): LocalDateTime =
        LocalDateTime.of(2026, 8, 24, hour, minute).plusDays((day.value - DayOfWeek.MONDAY.value).toLong())
}
