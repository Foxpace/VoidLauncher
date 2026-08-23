package com.tomasrepcik.voidlauncher.ui.home.appearance

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferencesMutation
import com.tomasrepcik.voidlauncher.domain.error.AppErrorKind
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.PlannedRepositoryFailures
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import com.tomasrepcik.voidlauncher.testing.startCollecting
import com.tomasrepcik.voidlauncher.ui.LauncherUiEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeAppearanceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun givenExistingBackground_whenNewBackgroundIsSelectedAndRestored_thenPermissionsAndPreferencesAreOwned() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val repository = launcherRepository()
            advanceUntilIdle()
            repository.mutatePreferences(
                LauncherPreferencesMutation.SetHomeBackground("content://images/first"),
            )
            repository.mutatePreferences(
                LauncherPreferencesMutation.SetUseBackgroundColors(true),
            )
            advanceUntilIdle()
            val taken = mutableListOf<String>()
            val released = mutableListOf<String>()
            val decoded = mutableListOf<String>()
            val subject = HomeAppearanceViewModel.createForTest(
                repository = repository,
                takePermission = { uri -> taken += uri; true },
                releasePermission = released::add,
                decode = { uri -> decoded += uri; null },
            )
            startCollecting(subject.state)
            advanceUntilIdle()

            // WHEN
            subject.selectBackground("content://images/second")
            advanceUntilIdle()
            subject.setUseBackgroundColors(true)
            advanceUntilIdle()

            // THEN
            val preferences = repository.readyState().launcher.preferences
            assertThat(preferences.homeBackgroundUri).isEqualTo("content://images/second")
            assertThat(preferences.useBackgroundColors).isTrue()
            assertThat(subject.state.value.backgroundUri).isEqualTo("content://images/second")
            assertThat(taken).containsExactly("content://images/second")
            assertThat(released).containsExactly("content://images/first")
            assertThat(decoded).containsExactly(
                "content://images/first",
                "content://images/second",
            ).inOrder()

            // WHEN
            subject.restoreDefault()
            advanceUntilIdle()

            // THEN
            val restoredPreferences = repository.readyState().launcher.preferences
            assertThat(restoredPreferences.homeBackgroundUri).isNull()
            assertThat(restoredPreferences.useBackgroundColors).isFalse()
            assertThat(released).containsExactly(
                "content://images/first",
                "content://images/second",
            ).inOrder()
        }

    @Test
    fun givenBackgroundPersistenceFailure_whenNewBackgroundIsSelected_thenPermissionIsReleasedAndErrorIsEmitted() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val repository = launcherRepository(
                failures = PlannedRepositoryFailures(writeCount = 1),
            )
            advanceUntilIdle()
            val released = mutableListOf<String>()
            val subject = HomeAppearanceViewModel.createForTest(
                repository = repository,
                releasePermission = released::add,
            )
            startCollecting(subject.state)
            val effect = async { subject.effects.first() }

            // WHEN
            subject.selectBackground("content://images/new")
            advanceUntilIdle()

            // THEN
            val preferences = repository.readyState().launcher.preferences
            assertThat(preferences.homeBackgroundUri).isNull()
            assertThat(released).containsExactly("content://images/new")
            val error = effect.await() as LauncherUiEffect.Error
            assertThat(error.error.kind).isEqualTo(AppErrorKind.STORAGE_WRITE_FAILED)
        }
}
