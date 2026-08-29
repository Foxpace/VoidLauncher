package com.tomasrepcik.voidlauncher.schedule.editor

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.appSchedule
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.homeAppsRepository
import com.tomasrepcik.voidlauncher.testing.installedAppsRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import com.tomasrepcik.voidlauncher.testing.scheduleRepository
import com.tomasrepcik.voidlauncher.testing.startCollecting
import com.tomasrepcik.voidlauncher.appcatalog.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.launcher.LauncherRootAction
import com.tomasrepcik.voidlauncher.schedule.editor.ScheduleEditorAction
import com.tomasrepcik.voidlauncher.schedule.editor.ScheduleEditorViewModel
import com.tomasrepcik.voidlauncher.schedule.list.ScheduleListAction
import com.tomasrepcik.voidlauncher.schedule.list.ScheduleListViewModel
import com.tomasrepcik.voidlauncher.schedule.list.ScheduleListNavigationEvent
import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun givenScheduleRoots_whenNavigationActionsAreSent_thenDestinationsAreExposed() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val repository = launcherRepository()
            advanceUntilIdle()
            val list = ScheduleListViewModel(repository.scheduleRepository())
            val editor = ScheduleEditorViewModel(
                schedules = repository.scheduleRepository(),
                installedApps = repository.installedAppsRepository(),
                homeApps = repository.homeAppsRepository(),
                scheduleId = null,
                installedAppSearch = InstalledAppSearch(),
                scheduleIdFactory = { "schedule" },
            )
            val listNavigation = async { list.navigation.take(3).toList() }
            val editorRootAction = async { editor.rootActions.first() }

            // WHEN
            list.onAction(ScheduleListAction.Back)
            list.onAction(ScheduleListAction.AddSchedule)
            list.onAction(ScheduleListAction.EditSchedule("work"))
            editor.onAction(ScheduleEditorAction.Back)
            advanceUntilIdle()

            // THEN
            assertThat(listNavigation.await()).containsExactly(
                ScheduleListNavigationEvent.Back,
                ScheduleListNavigationEvent.Add,
                ScheduleListNavigationEvent.Edit("work"),
            ).inOrder()
            assertThat(editorRootAction.await()).isEqualTo(LauncherRootAction.CloseScreen)
        }

    @Test
    fun givenNewScheduleEditor_whenCompleteScheduleIntentsAreSent_thenScheduleIsPersisted() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val mail = installedApp("Mail")
            val repository = launcherRepository(installedApps = listOf(mail))
            advanceUntilIdle()
            val subject = ScheduleEditorViewModel(
                schedules = repository.scheduleRepository(),
                installedApps = repository.installedAppsRepository(),
                homeApps = repository.homeAppsRepository(),
                scheduleId = null,
                installedAppSearch = InstalledAppSearch(),
                scheduleIdFactory = { "work" },
            )
            startCollecting(subject.uiState)
            advanceUntilIdle()

            // WHEN
            subject.onAction(ScheduleEditorAction.ChangeName("  Work  "))
            subject.onAction(ScheduleEditorAction.ChangeDays(setOf(DayOfWeek.MONDAY)))
            subject.onAction(ScheduleEditorAction.ChangeStartTime(8 * 60))
            subject.onAction(ScheduleEditorAction.ChangeEndTime(16 * 60))
            subject.onAction(ScheduleEditorAction.ToggleApp(mail.key))
            val effect = async { subject.rootActions.first() }
            subject.onAction(ScheduleEditorAction.SaveSchedule)
            advanceUntilIdle()

            // THEN
            assertThat(effect.await()).isEqualTo(LauncherRootAction.CloseScreen)
            val launcher = repository.readyState().launcher
            val schedule = launcher.schedules.single()
            assertThat(schedule.id).isEqualTo("work")
            assertThat(schedule.name).isEqualTo("Work")
            assertThat(schedule.days).containsExactly(DayOfWeek.MONDAY)
            assertThat(schedule.appKeys).containsExactly(mail.key)
        }

    @Test
    fun givenNewSchedule_whenEditorStateIsLoaded_thenWeekdaysCurrentAppsAndPickerStateAreExposed() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val mail = installedApp("Mail")
            val music = installedApp("Music")
            val repository = launcherRepository(
                installedApps = listOf(mail, music),
                pinnedApps = listOf(mail),
            )
            advanceUntilIdle()
            val subject = ScheduleEditorViewModel(
                schedules = repository.scheduleRepository(),
                installedApps = repository.installedAppsRepository(),
                homeApps = repository.homeAppsRepository(),
                scheduleId = null,
                installedAppSearch = InstalledAppSearch(),
                scheduleIdFactory = { "schedule" },
            )
            startCollecting(subject.uiState)

            // WHEN
            advanceUntilIdle()

            // THEN
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

            // WHEN
            subject.onAction(ScheduleEditorAction.OpenAppPicker)
            subject.onAction(ScheduleEditorAction.ChangeAppQuery("mail"))
            advanceUntilIdle()

            // THEN
            assertThat(subject.uiState.value.isAppPickerOpen).isTrue()

            // WHEN
            subject.onAction(ScheduleEditorAction.CloseAppPicker)
            advanceUntilIdle()

            // THEN
            assertThat(subject.uiState.value.isAppPickerOpen).isFalse()
            assertThat(subject.uiState.value.appQuery).isEmpty()
        }

    @Test
    fun givenSavedSchedule_whenDisableIntentIsSent_thenScheduleIsDisabledWithoutDeletion() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val mail = installedApp("Mail")
            val schedule = appSchedule(apps = listOf(mail))
            val repository = launcherRepository(
                installedApps = listOf(mail),
                schedules = listOf(schedule),
            )
            advanceUntilIdle()
            val subject = ScheduleListViewModel(repository.scheduleRepository())
            startCollecting(subject.uiState)

            // WHEN
            subject.onAction(
                ScheduleListAction.SetScheduleEnabled(schedule, enabled = false),
            )
            advanceUntilIdle()

            // THEN
            val launcher = repository.readyState().launcher
            val saved = launcher.schedules.single()
            assertThat(saved.enabled).isFalse()
        }
}
