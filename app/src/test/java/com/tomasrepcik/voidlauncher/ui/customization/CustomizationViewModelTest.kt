package com.tomasrepcik.voidlauncher.ui.customization

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import com.tomasrepcik.voidlauncher.testing.startCollecting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomizationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun repositoryInitializationCreatesDefaultShortcutsBeforeViewModelReadsState() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = launcherRepository()
            advanceUntilIdle()
            val subject = CustomizationViewModel(repository)
            startCollecting(subject.uiState)
            advanceUntilIdle()

            val state = subject.uiState.value
            val slots = state.shortcuts.map { it.slot }
            assertThat(slots)
                .containsExactly(ShortcutSlot.LEFT, ShortcutSlot.RIGHT).inOrder()
        }

    @Test
    fun shortcutPickerUsesSearchAndRepositoryOutcomes() = runTest(mainDispatcherRule.dispatcher) {
        val minuta = installedApp("Minúta")
        val repository = launcherRepository(installedApps = listOf(minuta, installedApp("Maps")))
        advanceUntilIdle()
        val subject = ShortcutPickerViewModel(
            ShortcutSlot.RIGHT,
            repository,
            InstalledAppSearch(),
        )
        startCollecting(subject.uiState)

        subject.onQueryChange("minuta")
        subject.onAppSelected(minuta)
        advanceUntilIdle()

        assertThat(subject.uiState.value.apps).containsExactly(minuta)
        val right = repository.readyState().launcher.bottomShortcuts
            .single { it.slot == ShortcutSlot.RIGHT }
        assertThat(right.selection).isEqualTo(ShortcutSelection.AppShortcut(minuta.key))
        assertThat(right.installedApp).isEqualTo(minuta)
    }
}
