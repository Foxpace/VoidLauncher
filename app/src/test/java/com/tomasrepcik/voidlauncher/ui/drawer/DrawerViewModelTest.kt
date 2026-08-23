package com.tomasrepcik.voidlauncher.ui.drawer

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.domain.action.LauncherAction
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.ui.LauncherUiEffect
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import com.tomasrepcik.voidlauncher.testing.startCollecting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DrawerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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
            val subject = DrawerViewModel(repository, InstalledAppSearch())
            startCollecting(subject.uiState)

            // WHEN
            subject.onQueryChange("INU!!")
            advanceUntilIdle()

            // THEN
            assertThat(subject.uiState.value.apps).containsExactly(minuta)
            assertThat(subject.uiState.value.pinnedAppKeys).containsExactly(camera.key)
        }

    @Test
    fun givenDrawer_whenAppActionsRun_thenEffectsAndPinnedStateAreUpdated() = runTest(mainDispatcherRule.dispatcher) {
        // GIVEN
        val camera = installedApp("Camera")
        val repository = launcherRepository(installedApps = listOf(camera))
        advanceUntilIdle()
        val subject = DrawerViewModel(repository, InstalledAppSearch())
        val action = async { subject.effects.first() }

        // WHEN
        subject.onAppClicked(camera)
        subject.addHomeApp(camera)
        advanceUntilIdle()

        // THEN
        assertThat(action.await()).isEqualTo(
            LauncherUiEffect.Action(LauncherAction.LaunchInstalledApp(camera))
        )
        assertThat(repository.readyState().launcher.pinnedAppKeys).containsExactly(camera.key)

        // WHEN
        subject.removeHomeApp(camera)
        advanceUntilIdle()

        // THEN
        assertThat(repository.readyState().launcher.pinnedAppKeys).isEmpty()
    }
}
