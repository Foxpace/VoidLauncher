package com.tomasrepcik.voidlauncher.ui.customization

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.installedAppsRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import com.tomasrepcik.voidlauncher.testing.shortcutRepository
import com.tomasrepcik.voidlauncher.testing.startCollecting
import com.tomasrepcik.voidlauncher.ui.LauncherRootAction
import com.tomasrepcik.voidlauncher.ui.customization.shortcutpicker.ShortcutPickerViewModel
import com.tomasrepcik.voidlauncher.ui.customization.shortcutpicker.ShortcutPickerAction
import com.tomasrepcik.voidlauncher.ui.customization.shortcutpicker.ShortcutPickerNavigationEvent
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
class CustomizationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun givenShortcutPicker_whenBackIsSent_thenRootNavigationIsExposed() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val repository = launcherRepository()
            advanceUntilIdle()
            val subject = ShortcutPickerViewModel(
                slot = ShortcutSlot.LEFT,
                installedApps = repository.installedAppsRepository(),
                shortcuts = repository.shortcutRepository(),
                installedAppSearch = InstalledAppSearch(),
            )
            val navigation = async { subject.navigation.first() }

            // WHEN
            subject.onAction(ShortcutPickerAction.Back)
            advanceUntilIdle()

            // THEN
            assertThat(navigation.await()).isEqualTo(ShortcutPickerNavigationEvent.Back)
        }

    @Test
    fun givenCustomizationNavigationActions_whenSent_thenRootDecisionsAreExposedInOrder() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val repository = launcherRepository()
            advanceUntilIdle()
            val subject = CustomizationViewModel(repository.shortcutRepository())
            val navigation = async { subject.navigation.take(4).toList() }

            // WHEN
            subject.onAction(CustomizationAction.Back)
            subject.onAction(CustomizationAction.EditShortcut(ShortcutSlot.LEFT))
            subject.onAction(CustomizationAction.OpenSchedules)
            subject.onAction(CustomizationAction.ShowNavigationTutorial)
            advanceUntilIdle()

            // THEN
            assertThat(navigation.await()).containsExactly(
                CustomizationNavigationEvent.Back,
                CustomizationNavigationEvent.EditShortcut(ShortcutSlot.LEFT),
                CustomizationNavigationEvent.OpenSchedules,
                CustomizationNavigationEvent.ShowNavigationTutorial,
            ).inOrder()
        }

    @Test
    fun givenInitializedRepository_whenCustomizationStateIsRead_thenDefaultShortcutsAreAvailable() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val repository = launcherRepository()
            advanceUntilIdle()
            val subject = CustomizationViewModel(repository.shortcutRepository())
            startCollecting(subject.uiState)

            // WHEN
            advanceUntilIdle()

            // THEN
            val state = subject.uiState.value
            val slots = state.shortcuts.map { it.slot }
            assertThat(slots)
                .containsExactly(ShortcutSlot.LEFT, ShortcutSlot.RIGHT).inOrder()
        }

    @Test
    fun givenShortcutPicker_whenAppIsSelected_thenSearchAndSavedShortcutAreExposed() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val minuta = installedApp("Minúta")
            val repository = launcherRepository(installedApps = listOf(minuta, installedApp("Maps")))
            advanceUntilIdle()
            val subject = ShortcutPickerViewModel(
                slot = ShortcutSlot.RIGHT,
                installedApps = repository.installedAppsRepository(),
                shortcuts = repository.shortcutRepository(),
                installedAppSearch = InstalledAppSearch(),
            )
            startCollecting(subject.uiState)
            val completion = async { subject.rootActions.first() }

            // WHEN
            subject.onAction(ShortcutPickerAction.QueryChanged("minuta"))
            subject.onAction(ShortcutPickerAction.SelectApp(minuta))
            advanceUntilIdle()

            // THEN
            assertThat(subject.uiState.value.apps).containsExactly(minuta)
            val right = repository.readyState().launcher.bottomShortcuts
                .single { it.slot == ShortcutSlot.RIGHT }
            assertThat(right.selection).isEqualTo(ShortcutSelection.AppShortcut(minuta.key))
            assertThat(right.installedApp).isEqualTo(minuta)
            assertThat(completion.await()).isEqualTo(LauncherRootAction.CloseScreen)
        }
}
