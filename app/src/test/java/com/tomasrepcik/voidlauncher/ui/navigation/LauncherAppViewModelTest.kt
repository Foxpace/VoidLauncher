package com.tomasrepcik.voidlauncher.ui.navigation

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.repository.LauncherStatusRepository
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.PlannedRepositoryFailures
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.preferencesRepository
import com.tomasrepcik.voidlauncher.testing.startCollecting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherAppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun givenReadyApp_whenTutorialIsCompleted_thenAppStateOwnsThePreferenceResult() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val repository = launcherRepository()
            advanceUntilIdle()
            val subject = LauncherAppViewModel(
                status = LauncherStatusRepository(repository),
                preferences = repository.preferencesRepository(),
            )
            startCollecting(subject.uiState)
            advanceUntilIdle()
            assertThat(subject.uiState.value.hasSeenNavigationTutorial).isFalse()

            // WHEN
            subject.onAction(LauncherAppAction.MarkNavigationTutorialSeen)
            advanceUntilIdle()

            // THEN
            assertThat(subject.uiState.value.hasSeenNavigationTutorial).isTrue()
        }

    @Test
    fun givenFailedStartup_whenRetryIsRequested_thenAppStateBecomesReady() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val repository = launcherRepository(
                failures = PlannedRepositoryFailures(initializationCount = 1),
            )
            advanceUntilIdle()
            val subject = LauncherAppViewModel(
                status = LauncherStatusRepository(repository),
                preferences = repository.preferencesRepository(),
            )
            startCollecting(subject.uiState)
            advanceUntilIdle()
            assertThat(subject.uiState.value.initializationError).isNotNull()

            // WHEN
            subject.onAction(LauncherAppAction.RetryInitialization)
            advanceUntilIdle()

            // THEN
            assertThat(subject.uiState.value.isLoading).isFalse()
            assertThat(subject.uiState.value.initializationError).isNull()
            assertThat(subject.uiState.value.hasSeenNavigationTutorial).isFalse()
        }
}
