package com.tomasrepcik.voidlauncher.ui.schedule

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import com.tomasrepcik.voidlauncher.testing.startCollecting
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun editorIntentsProduceAndPersistOneCompleteSchedule() =
        runTest(mainDispatcherRule.dispatcher) {
            val mail = installedApp("Mail")
            val repository = launcherRepository(installedApps = listOf(mail))
            advanceUntilIdle()
            val subject = ScheduleEditorViewModel(
                repository = repository,
                scheduleId = null,
                newId = { "work" },
            )
            startCollecting(subject.uiState)
            advanceUntilIdle()

            subject.onIntent(ScheduleEditorIntent.NameChanged("  Work  "))
            subject.onIntent(ScheduleEditorIntent.DaysChanged(setOf(DayOfWeek.MONDAY)))
            subject.onIntent(ScheduleEditorIntent.StartTimeChanged(8 * 60))
            subject.onIntent(ScheduleEditorIntent.EndTimeChanged(16 * 60))
            subject.onIntent(ScheduleEditorIntent.AppToggled(mail.key))
            val effect = async { subject.effects.first() }
            subject.onIntent(ScheduleEditorIntent.Save)
            advanceUntilIdle()

            assertThat(effect.await()).isEqualTo(ScheduleEffect.Saved)
            val launcher = repository.readyState().launcher
            val schedule = launcher.schedules.single()
            assertThat(schedule.id).isEqualTo("work")
            assertThat(schedule.name).isEqualTo("Work")
            assertThat(schedule.days).containsExactly(DayOfWeek.MONDAY)
            assertThat(schedule.appKeys).containsExactly(mail.key)
        }

    @Test
    fun newScheduleStartsWithWeekdaysAndCurrentHomeApps() =
        runTest(mainDispatcherRule.dispatcher) {
            val mail = installedApp("Mail")
            val music = installedApp("Music")
            val repository = launcherRepository(
                installedApps = listOf(mail, music),
                pinnedApps = listOf(mail),
            )
            advanceUntilIdle()
            val subject = ScheduleEditorViewModel(repository, scheduleId = null)
            startCollecting(subject.uiState)
            advanceUntilIdle()

            val state = subject.uiState.value
            assertThat(state.name).isEqualTo("My schedule")
            assertThat(state.days).containsExactly(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            )
            assertThat(state.selectedAppKeys).containsExactly(mail.key)
            assertThat(state.installedApps.first()).isEqualTo(mail)

            subject.onIntent(ScheduleEditorIntent.OpenAppPicker)
            subject.onIntent(ScheduleEditorIntent.AppQueryChanged("mail"))
            advanceUntilIdle()
            assertThat(subject.uiState.value.isAppPickerOpen).isTrue()

            subject.onIntent(ScheduleEditorIntent.CloseAppPicker)
            advanceUntilIdle()
            assertThat(subject.uiState.value.isAppPickerOpen).isFalse()
            assertThat(subject.uiState.value.appQuery).isEmpty()
        }

    @Test
    fun listIntentCanDisableScheduleWithoutDeletingIt() =
        runTest(mainDispatcherRule.dispatcher) {
            val mail = installedApp("Mail")
            val schedule = AppSchedule(
                id = "work",
                name = "Work",
                days = setOf(DayOfWeek.MONDAY),
                startMinute = 9 * 60,
                endMinute = 17 * 60,
                appKeys = setOf(mail.key),
            )
            val repository = launcherRepository(
                installedApps = listOf(mail),
                schedules = listOf(schedule),
            )
            advanceUntilIdle()
            val subject = ScheduleListViewModel(repository)
            startCollecting(subject.uiState)

            subject.onIntent(ScheduleListIntent.SetEnabled(schedule, enabled = false))
            advanceUntilIdle()

            val launcher = repository.readyState().launcher
            val saved = launcher.schedules.single()
            assertThat(saved.enabled).isFalse()
        }
}
