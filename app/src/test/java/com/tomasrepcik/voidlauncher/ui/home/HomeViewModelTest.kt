package com.tomasrepcik.voidlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.action.LauncherAction
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.domain.search.SearchTarget
import com.tomasrepcik.voidlauncher.ui.LauncherRootAction
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.appSchedule
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.homeAppsRepository
import com.tomasrepcik.voidlauncher.testing.installedAppsRepository
import com.tomasrepcik.voidlauncher.testing.scheduleRepository
import com.tomasrepcik.voidlauncher.testing.shortcutRepository
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun givenHomeNavigationActions_whenSent_thenViewModelExposesRootDestinations() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val repository = launcherRepository()
            advanceUntilIdle()
            val subject = repository.homeViewModel()
            val navigation = async { subject.navigation.take(2).toList() }

            // WHEN
            subject.onAction(HomeAction.OpenDrawer)
            subject.onAction(HomeAction.OpenSchedules)
            advanceUntilIdle()

            // THEN
            assertThat(navigation.await()).containsExactly(
                HomeNavigationEvent.OpenDrawer,
                HomeNavigationEvent.OpenSchedules,
            ).inOrder()
        }

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
            val subject = repository.homeViewModel()
            startCollecting(subject.uiState)

            // WHEN
            subject.onAction(HomeAction.QueryChanged("spot"))
            advanceUntilIdle()
            val action = async { subject.rootActions.first() }
            subject.onAction(HomeAction.Search(SearchTarget.BestMatch))
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
                LauncherRootAction.Open(LauncherAction.LaunchInstalledApp(spotify))
            )
        }

    @Test
    fun givenBlankQuery_whenSearchRuns_thenFeedbackIsEmittedOutsideErrorState() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val repository = launcherRepository()
            advanceUntilIdle()
            val subject = repository.homeViewModel()
            val feedback = async { subject.rootActions.first() }

            // WHEN
            subject.onAction(HomeAction.Search(SearchTarget.BestMatch))
            advanceUntilIdle()

            // THEN
            assertThat(feedback.await())
                .isEqualTo(LauncherRootAction.ShowMessage("Type a query first."))
        }

    @Test
    fun givenHomeItems_whenClickedOrUninstalled_thenLauncherActionsAreEmitted() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val app = installedApp("Camera")
            val shortcut = resolvedShortcut(ShortcutSlot.LEFT)
            val repository = launcherRepository()
            advanceUntilIdle()
            val subject = repository.homeViewModel()
            val actions = async { subject.rootActions.take(3).toList() }

            // WHEN
            subject.onAction(HomeAction.OpenApp(app))
            subject.onAction(HomeAction.OpenShortcut(shortcut))
            subject.onAction(HomeAction.UninstallApp(app))
            advanceUntilIdle()

            // THEN
            assertThat(actions.await()).containsExactly(
                LauncherRootAction.Open(LauncherAction.LaunchInstalledApp(app)),
                LauncherRootAction.Open(LauncherAction.OpenShortcut(shortcut)),
                LauncherRootAction.Open(LauncherAction.UninstallApp(app)),
            ).inOrder()
        }

    @Test
    fun givenPinnedApps_whenRemovedRenamedAndReordered_thenRepositoryExposesUpdatedApps() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val camera = installedApp("Camera")
            val maps = installedApp("Maps")
            val spotify = installedApp("Spotify")
            val repository = launcherRepository(
                installedApps = listOf(camera, maps, spotify),
                pinnedApps = listOf(camera, maps, spotify),
            )
            advanceUntilIdle()
            val subject = repository.homeViewModel()

            // WHEN
            subject.onAction(HomeAction.RemoveApp(camera))
            subject.onAction(HomeAction.RenameApp(maps, "Lens"))
            subject.onAction(HomeAction.ReorderApps(1, 0))
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
        val schedule = appSchedule(apps = listOf(mail))
        val repository = launcherRepository(
            installedApps = listOf(mail, music),
            pinnedApps = listOf(music),
            schedules = listOf(schedule),
        )
        val currentTime = MutableStateFlow(LocalDateTime.of(2026, 8, 24, 10, 0))
        advanceUntilIdle()
        val subject = repository.homeViewModel(currentTime)

        // WHEN
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

private fun com.tomasrepcik.voidlauncher.data.repository.LauncherRepository.homeViewModel(
    currentTime: kotlinx.coroutines.flow.Flow<LocalDateTime> =
        kotlinx.coroutines.flow.flowOf(LocalDateTime.now()),
) = HomeViewModel(
    installedApps = installedAppsRepository(),
    homeApps = homeAppsRepository(),
    shortcuts = shortcutRepository(),
    schedules = scheduleRepository(),
    installedAppSearch = InstalledAppSearch(),
    currentTime = currentTime,
)
