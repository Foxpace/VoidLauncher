package com.tomasrepcik.voidlauncher.data.repository

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.testing.appSchedule
import com.tomasrepcik.voidlauncher.testing.homeAppsRepository
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.installedAppsRepository
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.preferencesRepository
import com.tomasrepcik.voidlauncher.testing.scheduleRepository
import com.tomasrepcik.voidlauncher.testing.shortcutRepository
import com.tomasrepcik.voidlauncher.testing.startCollecting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureRepositoryContractsTest {
    @Test
    fun givenReadyLauncher_whenFeatureRepositoriesAreRead_thenEachExposesItsOwnSlice() = runTest {
        // GIVEN
        val mail = installedApp("Mail")
        val schedule = appSchedule(apps = listOf(mail))
        val launcher = launcherRepository(
            installedApps = listOf(mail),
            pinnedApps = listOf(mail),
            schedules = listOf(schedule),
        )
        advanceUntilIdle()

        // WHEN
        val installedApps = launcher.installedAppsRepository().apps.first()
        val homeApps = launcher.homeAppsRepository().data.first()
        val shortcuts = launcher.shortcutRepository().shortcuts.first()
        val preferences = launcher.preferencesRepository().preferences.first()
        val schedules = launcher.scheduleRepository().schedules.first()

        // THEN
        assertThat(installedApps).containsExactly(mail)
        assertThat(homeApps?.apps).containsExactly(mail)
        assertThat(shortcuts?.map { it.slot })
            .containsExactly(ShortcutSlot.LEFT, ShortcutSlot.RIGHT).inOrder()
        assertThat(preferences).isNotNull()
        assertThat(schedules).containsExactly(schedule)
        assertThat(LauncherStatusRepository(launcher).state.value)
            .isInstanceOf(LauncherRepositoryState.Ready::class.java)
    }

    @Test
    fun givenFeatureRepositories_whenWritesRun_thenOnlyNamedFeatureStateChanges() = runTest {
        // GIVEN
        val launcher = launcherRepository()
        advanceUntilIdle()
        val shortcuts = launcher.shortcutRepository()
        val preferences = launcher.preferencesRepository()
        startCollecting(launcher.state)

        // WHEN
        val shortcutResult = shortcuts.save(ShortcutSlot.LEFT, ShortcutSelection.SystemCamera)
        val preferenceResult = preferences.setHomeBackground("content://background")
        advanceUntilIdle()

        // THEN
        assertThat(shortcutResult).isEqualTo(RepositoryWriteResult.Completed)
        assertThat(preferenceResult).isEqualTo(RepositoryWriteResult.Completed)
        val ready = launcher.state.value as LauncherRepositoryState.Ready
        val leftShortcut = ready.launcher.bottomShortcuts.single { it.slot == ShortcutSlot.LEFT }
        assertThat(leftShortcut.selection).isEqualTo(ShortcutSelection.SystemCamera)
        assertThat(ready.launcher.preferences.homeBackgroundUri).isEqualTo("content://background")
    }
}
