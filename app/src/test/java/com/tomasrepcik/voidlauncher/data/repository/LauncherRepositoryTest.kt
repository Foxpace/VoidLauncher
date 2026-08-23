package com.tomasrepcik.voidlauncher.data.repository

import android.database.sqlite.SQLiteException
import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferencesMutation
import com.tomasrepcik.voidlauncher.domain.error.AppErrorKind
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import com.tomasrepcik.voidlauncher.domain.schedule.ScheduleMutation
import com.tomasrepcik.voidlauncher.testing.PlannedRepositoryFailures
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherRepositoryTest {
    @Test
    fun givenInitializationFailure_whenInitializationIsRetried_thenDefaultsAreCreated() = runTest {
        // GIVEN
        val repository = launcherRepository(
            failures = PlannedRepositoryFailures(initializationCount = 1),
        )

        // WHEN
        advanceUntilIdle()

        // THEN
        val failure = repository.state.value as LauncherRepositoryState.InitializationError
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.STORAGE_INITIALIZATION_FAILED)
        assertThat(failure.error.cause).isInstanceOf(SQLiteException::class.java)

        // WHEN
        repository.retryInitialization()
        advanceUntilIdle()

        // THEN
        val launcher = repository.readyState().launcher
        val slots = launcher.bottomShortcuts.map { it.slot }
        assertThat(slots)
            .containsExactly(ShortcutSlot.LEFT, ShortcutSlot.RIGHT).inOrder()
    }

    @Test
    fun givenRepositoryWriteFailure_whenHomeAppIsRemoved_thenLastValidStateAndErrorAreReturned() = runTest {
        // GIVEN
        val camera = installedApp("Camera")
        val repository = launcherRepository(
            installedApps = listOf(camera),
            pinnedApps = listOf(camera),
            failures = PlannedRepositoryFailures(writeCount = 1),
        )
        advanceUntilIdle()
        val before = repository.readyState().launcher

        // WHEN
        val outcome = repository.removeHomeApp(camera.key)
        advanceUntilIdle()

        // THEN
        val failure = outcome as RepositoryMutationOutcome.Failed
        val ready = repository.readyState()
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.STORAGE_WRITE_FAILED)
        assertThat(failure.error.cause).isInstanceOf(SQLiteException::class.java)
        assertThat(ready.launcher).isEqualTo(before)
    }

    @Test
    fun givenReadyRepository_whenNamedMutationsRun_thenObservableLauncherStateIsUpdated() = runTest {
        // GIVEN
        val camera = installedApp("Camera")
        val maps = installedApp("Maps")
        val repository = launcherRepository(installedApps = listOf(camera, maps))
        advanceUntilIdle()

        // WHEN
        repository.saveHomeApps(listOf(camera.key, maps.key, camera.key))
        repository.renameHomeApp(maps.key, "Navigation")
        repository.reorderHomeApps(1, 0)
        repository.mutatePreferences(LauncherPreferencesMutation.SetHomeAppCount(100))
        repository.mutatePreferences(
            LauncherPreferencesMutation.SetHomeBackground("content://images/background")
        )
        repository.mutatePreferences(LauncherPreferencesMutation.SetUseBackgroundColors(true))
        repository.mutatePreferences(LauncherPreferencesMutation.MarkNavigationTutorialSeen)
        advanceUntilIdle()

        // THEN
        val launcher = repository.readyState().launcher
        assertThat(launcher.pinnedHomeApps.map { it.label })
            .containsExactly("Navigation", "Camera").inOrder()
        assertThat(launcher.preferences.homeAppCount).isEqualTo(10)
        assertThat(launcher.preferences.homeBackgroundUri)
            .isEqualTo("content://images/background")
        assertThat(launcher.preferences.useBackgroundColors).isTrue()
        assertThat(launcher.preferences.hasSeenNavigationTutorial).isTrue()

        // WHEN
        repository.mutatePreferences(LauncherPreferencesMutation.SetHomeBackground(null))
        repository.mutatePreferences(LauncherPreferencesMutation.SetUseBackgroundColors(true))
        advanceUntilIdle()

        // THEN
        val clearedPreferences = repository.readyState().launcher.preferences
        assertThat(clearedPreferences.useBackgroundColors).isFalse()
    }

    @Test
    fun givenReadyRepository_whenScheduleIsSavedAndDeleted_thenObservableScheduleStateIsUpdated() = runTest {
        // GIVEN
        val mail = installedApp("Mail")
        val repository = launcherRepository(installedApps = listOf(mail))
        advanceUntilIdle()
        val schedule = AppSchedule(
            id = "work",
            name = "Work",
            days = setOf(DayOfWeek.MONDAY),
            startMinute = 9 * 60,
            endMinute = 17 * 60,
            appKeys = setOf(mail.key),
        )

        // WHEN
        repository.mutateSchedule(ScheduleMutation.Save(schedule))
        advanceUntilIdle()

        // THEN
        assertThat(repository.readyState().launcher.schedules).containsExactly(schedule)

        // WHEN
        repository.mutateSchedule(ScheduleMutation.Delete(schedule.id))
        advanceUntilIdle()

        // THEN
        assertThat(repository.readyState().launcher.schedules).isEmpty()
    }
}
