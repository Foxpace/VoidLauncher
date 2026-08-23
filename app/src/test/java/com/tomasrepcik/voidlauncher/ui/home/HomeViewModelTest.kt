package com.tomasrepcik.voidlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.action.LauncherAction
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.domain.search.SearchTarget
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import com.tomasrepcik.voidlauncher.ui.LauncherUiEffect
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import com.tomasrepcik.voidlauncher.testing.resolvedShortcut
import com.tomasrepcik.voidlauncher.testing.startCollecting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun givenReadyRepository_whenHomeSearchRuns_thenContentSuggestionsAndActionsAreExposed() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val spotify = installedApp("Spotify")
            val maps = installedApp("Maps")
            val shortcuts = listOf(
                resolvedShortcut(ShortcutSlot.RIGHT),
                resolvedShortcut(ShortcutSlot.LEFT),
            )
            val repository = launcherRepository(
                installedApps = listOf(spotify, maps),
                pinnedApps = listOf(spotify),
                shortcuts = shortcuts,
            )
            advanceUntilIdle()
            val subject = HomeViewModel(repository, InstalledAppSearch())
            startCollecting(subject.uiState)

            // WHEN
            subject.onQueryChange("spot")
            advanceUntilIdle()
            val action = async { subject.effects.first() }
            subject.onSearch(SearchTarget.Primary)
            advanceUntilIdle()

            // THEN
            assertThat(subject.uiState.value.homeApps).containsExactly(spotify)
            val state = subject.uiState.value
            val slots = state.shortcuts.map { it.slot }
            assertThat(slots)
                .containsExactly(ShortcutSlot.LEFT, ShortcutSlot.RIGHT).inOrder()
            val suggestions = state.searchSuggestions
            val firstSuggestion = suggestions.first()
            assertThat(firstSuggestion).isEqualTo(spotify)
            assertThat(subject.uiState.value.isLoading).isFalse()
            assertThat(action.await()).isEqualTo(
                LauncherUiEffect.Action(LauncherAction.LaunchInstalledApp(spotify))
            )
        }

    @Test
    fun givenBlankAndNearMatchQueries_whenSearchAndHintRun_thenFeedbackIsEmittedOutsideErrorState() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val telegram = installedApp("Telegram")
            val repository = launcherRepository(installedApps = listOf(telegram))
            advanceUntilIdle()
            val subject = HomeViewModel(repository, InstalledAppSearch())
            startCollecting(subject.uiState)
            val feedback = async { subject.effects.take(2).toList() }

            // WHEN
            subject.onSearch(SearchTarget.Primary)
            subject.onQueryChange("telegrm")
            subject.onAppHint()
            advanceUntilIdle()

            // THEN
            assertThat(feedback.await()).containsExactly(
                LauncherUiEffect.Feedback("Type a query first."),
                LauncherUiEffect.Feedback("Best app hint: Telegram"),
            ).inOrder()
            assertThat(subject.uiState.value.hintMessage).isEqualTo("Try Telegram")
        }

    @Test
    fun givenHomeItems_whenClickedOrUninstalled_thenLauncherActionsAreEmitted() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val app = installedApp("Camera")
            val shortcut = resolvedShortcut(ShortcutSlot.LEFT)
            val repository = launcherRepository()
            advanceUntilIdle()
            val subject = HomeViewModel(repository, InstalledAppSearch())
            val actions = async { subject.effects.take(3).toList() }

            // WHEN
            subject.onAppClicked(app)
            subject.onShortcutClicked(shortcut)
            subject.uninstallApp(app)
            advanceUntilIdle()

            // THEN
            assertThat(actions.await()).containsExactly(
                LauncherUiEffect.Action(LauncherAction.LaunchInstalledApp(app)),
                LauncherUiEffect.Action(LauncherAction.OpenShortcut(shortcut)),
                LauncherUiEffect.Action(LauncherAction.UninstallApp(app)),
            ).inOrder()
        }

    @Test
    fun givenPinnedApps_whenMutated_thenRepositoryExposesUpdatedApps() = runTest(mainDispatcherRule.dispatcher) {
        // GIVEN
        val camera = installedApp("Camera")
        val maps = installedApp("Maps")
        val spotify = installedApp("Spotify")
        val repository = launcherRepository(
            installedApps = listOf(camera, maps, spotify),
            pinnedApps = listOf(camera, maps, spotify),
        )
        advanceUntilIdle()
        val subject = HomeViewModel(repository, InstalledAppSearch())

        // WHEN
        subject.removeHomeApp(camera)
        subject.renameHomeApp(maps, "Lens")
        subject.reorderHomeApps(1, 0)
        advanceUntilIdle()

        // THEN
        val launcher = repository.readyState().launcher
        val labels = launcher.pinnedHomeApps.map { it.label }
        assertThat(labels)
            .containsExactly("Spotify", "Lens").inOrder()
    }

    @Test
    fun givenSchedule_whenTimeChanges_thenHomeShowsActiveOrDefaultApps() = runTest(mainDispatcherRule.dispatcher) {
        // GIVEN
        val mail = installedApp("Mail")
        val music = installedApp("Music")
        val schedule = AppSchedule(
            id = "work",
            name = "Work",
            days = setOf(DayOfWeek.MONDAY),
            startMinute = 9 * 60,
            endMinute = 17 * 60,
            appKeys = setOf(mail.key),
        )
        val repository = launcherRepository(
            installedApps = listOf(mail, music),
            pinnedApps = listOf(music),
            schedules = listOf(schedule),
        )
        val currentTime = MutableStateFlow(LocalDateTime.of(2026, 8, 24, 10, 0))
        advanceUntilIdle()
        val subject = HomeViewModel(
            repository = repository,
            installedAppSearch = InstalledAppSearch(),
            currentTime = currentTime,
        )
        startCollecting(subject.uiState)
        advanceUntilIdle()

        // THEN
        assertThat(subject.uiState.value.homeApps).containsExactly(mail)
        assertThat(subject.uiState.value.isScheduleActive).isTrue()

        // WHEN
        currentTime.value = LocalDateTime.of(2026, 8, 24, 18, 0)
        advanceUntilIdle()

        // THEN
        assertThat(subject.uiState.value.homeApps).containsExactly(music)
        assertThat(subject.uiState.value.isScheduleActive).isFalse()
    }
}
