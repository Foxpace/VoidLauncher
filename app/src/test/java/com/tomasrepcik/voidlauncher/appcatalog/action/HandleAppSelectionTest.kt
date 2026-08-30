package com.tomasrepcik.voidlauncher.appcatalog.action

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.launcher.LauncherRootAction
import com.tomasrepcik.voidlauncher.launcher.action.LauncherAction
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.homeAppsRepository
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HandleAppSelectionTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun givenInstalledApp_whenOpenedOrUninstalled_thenNativeActionsAreReturned() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val camera = installedApp("Camera")
            val repository = launcherRepository(installedApps = listOf(camera))
            advanceUntilIdle()
            val subject = HandleAppSelection(repository.homeAppsRepository())

            // WHEN
            val open = subject(AppSelectionAction.Open(camera))
            val uninstall = subject(AppSelectionAction.Uninstall(camera))

            // THEN
            assertThat(open).isEqualTo(
                LauncherRootAction.Open(LauncherAction.LaunchInstalledApp(camera)),
            )
            assertThat(uninstall).isEqualTo(
                LauncherRootAction.Open(LauncherAction.UninstallApp(camera)),
            )
        }

    @Test
    fun givenInstalledApp_whenAddedToHome_thenConfirmationAndUpdatedHomeAreReturned() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val camera = installedApp("Camera")
            val repository = launcherRepository(installedApps = listOf(camera))
            advanceUntilIdle()
            val subject = HandleAppSelection(repository.homeAppsRepository())

            // WHEN
            val result = subject(AppSelectionAction.AddToHome(camera))
            advanceUntilIdle()

            // THEN
            assertThat(result).isEqualTo(
                LauncherRootAction.ShowAppAddedConfirmation(camera.label),
            )
            assertThat(repository.readyState().launcher.pinnedAppKeys).containsExactly(camera.key)
        }

    @Test
    fun givenHomeApp_whenRemovedFromHome_thenHomeIsUpdatedWithoutAUiAction() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val camera = installedApp("Camera")
            val repository = launcherRepository(
                installedApps = listOf(camera),
                pinnedApps = listOf(camera),
            )
            advanceUntilIdle()
            val subject = HandleAppSelection(repository.homeAppsRepository())

            // WHEN
            val result = subject(AppSelectionAction.RemoveFromHome(camera))
            advanceUntilIdle()

            // THEN
            assertThat(result).isNull()
            assertThat(repository.readyState().launcher.pinnedAppKeys).isEmpty()
        }
}
