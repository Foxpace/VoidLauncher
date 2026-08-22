package com.tomasrepcik.voidlauncher.ui.drawer

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.domain.action.LauncherAction
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
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
    fun stateUsesNormalizedSubstringFiltering() = runTest(mainDispatcherRule.dispatcher) {
        val minuta = installedApp("Minúta")
        val camera = installedApp("Camera")
        val repository = launcherRepository(
            installedApps = listOf(minuta, camera),
            pinnedApps = listOf(camera),
        )
        advanceUntilIdle()
        val subject = DrawerViewModel(repository, InstalledAppSearch())
        startCollecting(subject.uiState)

        subject.onQueryChange("INU!!")
        advanceUntilIdle()

        assertThat(subject.uiState.value.apps).containsExactly(minuta)
        assertThat(subject.uiState.value.pinnedAppKeys).containsExactly(camera.key)
    }

    @Test
    fun actionsAndMutationsUseDeepModuleInterfaces() = runTest(mainDispatcherRule.dispatcher) {
        val camera = installedApp("Camera")
        val repository = launcherRepository(installedApps = listOf(camera))
        advanceUntilIdle()
        val subject = DrawerViewModel(repository, InstalledAppSearch())
        val action = async { subject.actions.first() }

        subject.onAppClicked(camera)
        subject.addHomeApp(camera)
        advanceUntilIdle()

        assertThat(action.await()).isEqualTo(LauncherAction.LaunchInstalledApp(camera))
        assertThat(repository.readyState().launcher.pinnedAppKeys).containsExactly(camera.key)

        subject.removeHomeApp(camera)
        advanceUntilIdle()
        assertThat(repository.readyState().launcher.pinnedAppKeys).isEmpty()
    }
}
