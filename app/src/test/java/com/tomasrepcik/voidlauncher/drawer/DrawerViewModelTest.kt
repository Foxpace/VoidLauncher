package com.tomasrepcik.voidlauncher.drawer

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.launcher.action.LauncherAction
import com.tomasrepcik.voidlauncher.appcatalog.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.launcher.LauncherRootAction
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.homeAppsRepository
import com.tomasrepcik.voidlauncher.testing.installedAppsRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import com.tomasrepcik.voidlauncher.testing.startCollecting
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
class DrawerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun givenDrawerNavigationActions_whenSent_thenViewModelExposesRootDestinations() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val repository = launcherRepository()
            advanceUntilIdle()
            val subject = DrawerViewModel(
                installedApps = repository.installedAppsRepository(),
                homeApps = repository.homeAppsRepository(),
                installedAppSearch = InstalledAppSearch(),
            )
            val navigation = async { subject.navigation.take(2).toList() }

            // WHEN
            subject.onAction(DrawerAction.Back)
            subject.onAction(DrawerAction.OpenCustomization)
            advanceUntilIdle()

            // THEN
            assertThat(navigation.await()).containsExactly(
                DrawerNavigationEvent.Back,
                DrawerNavigationEvent.OpenCustomization,
            ).inOrder()
        }

    @Test
    fun givenApps_whenNormalizedQueryChanges_thenDrawerUsesSubstringFiltering() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val minuta = installedApp("Minúta")
            val camera = installedApp("Camera")
            val repository = launcherRepository(
                installedApps = listOf(minuta, camera),
                pinnedApps = listOf(camera),
            )
            advanceUntilIdle()
            val subject = DrawerViewModel(
                installedApps = repository.installedAppsRepository(),
                homeApps = repository.homeAppsRepository(),
                installedAppSearch = InstalledAppSearch(),
            )
            startCollecting(subject.uiState)

            // WHEN
            subject.onAction(DrawerAction.QueryChanged("INU!!"))
            advanceUntilIdle()

            // THEN
            assertThat(subject.uiState.value.apps).containsExactly(minuta)
            assertThat(subject.uiState.value.pinnedAppKeys).containsExactly(camera.key)
            assertThat(subject.uiState.value.sectionLetters).containsExactly(minuta.key, 'M')
            assertThat(subject.uiState.value.alphabetIndex).containsExactly('M', 0)
        }

    @Test
    fun givenAppList_whenAppIsAddedToHome_thenStateAndConfirmationAreUpdated() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val camera = installedApp("Camera")
            val repository = launcherRepository(installedApps = listOf(camera))
            advanceUntilIdle()
            val subject = DrawerViewModel(
                installedApps = repository.installedAppsRepository(),
                homeApps = repository.homeAppsRepository(),
                installedAppSearch = InstalledAppSearch(),
            )
            val confirmation = async { subject.rootActions.first() }

            // WHEN
            subject.onAction(DrawerAction.AddHomeApp(camera))
            advanceUntilIdle()

            // THEN
            assertThat(confirmation.await()).isEqualTo(
                LauncherRootAction.ShowAppAddedConfirmation(camera.label),
            )
            assertThat(repository.readyState().launcher.pinnedAppKeys).containsExactly(camera.key)
        }

    @Test
    fun givenPinnedApp_whenOpenedAndRemoved_thenLaunchAndStateAreUpdated() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val camera = installedApp("Camera")
            val repository = launcherRepository(
                installedApps = listOf(camera),
                pinnedApps = listOf(camera),
            )
            advanceUntilIdle()
            val subject = DrawerViewModel(
                installedApps = repository.installedAppsRepository(),
                homeApps = repository.homeAppsRepository(),
                installedAppSearch = InstalledAppSearch(),
            )
            val action = async { subject.rootActions.first() }

            // WHEN
            subject.onAction(DrawerAction.OpenApp(camera))
            subject.onAction(DrawerAction.RemoveHomeApp(camera))
            advanceUntilIdle()

            // THEN
            assertThat(action.await()).isEqualTo(
                LauncherRootAction.Open(LauncherAction.LaunchInstalledApp(camera))
            )
            assertThat(repository.readyState().launcher.pinnedAppKeys).isEmpty()
        }

    @Test
    fun givenUninstallRequest_whenSent_thenLauncherActionIsEmitted() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val camera = installedApp("Camera")
            val repository = launcherRepository(installedApps = listOf(camera))
            advanceUntilIdle()
            val subject = DrawerViewModel(
                installedApps = repository.installedAppsRepository(),
                homeApps = repository.homeAppsRepository(),
                installedAppSearch = InstalledAppSearch(),
            )
            val action = async { subject.rootActions.first() }

            // WHEN
            subject.onAction(DrawerAction.UninstallApp(camera))
            advanceUntilIdle()

            // THEN
            assertThat(action.await()).isEqualTo(
                LauncherRootAction.Open(LauncherAction.UninstallApp(camera)),
            )
        }
}
