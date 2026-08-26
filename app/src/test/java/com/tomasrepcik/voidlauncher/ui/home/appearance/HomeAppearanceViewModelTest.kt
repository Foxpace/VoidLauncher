package com.tomasrepcik.voidlauncher.ui.home.appearance

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.domain.error.AppErrorKind
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.PlannedRepositoryFailures
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.preferencesRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import com.tomasrepcik.voidlauncher.testing.startCollecting
import com.tomasrepcik.voidlauncher.ui.LauncherRootAction
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
            val preferencesRepository = repository.preferencesRepository()
            preferencesRepository.setHomeBackground("content://images/first")
            preferencesRepository.setUseBackgroundColors(true)
            advanceUntilIdle()
            val taken = mutableListOf<String>()
            val released = mutableListOf<String>()
            val decoded = mutableListOf<String>()
            val subject = HomeAppearanceViewModel(
                preferences = preferencesRepository,
                contentPermissions = RecordingContentPermissions(
                    onKeep = { uri -> taken += uri; true },
                    onRelease = released::add,
                ),
                backgroundImageReader = RecordingBackgroundImageReader(decoded::add),
            )
            startCollecting(subject.state)
            advanceUntilIdle()

            // WHEN
            subject.onAction(
                HomeAppearanceAction.SelectBackground("content://images/second"),
            )
            advanceUntilIdle()
            subject.onAction(HomeAppearanceAction.SetUseBackgroundColors(true))
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
            subject.onAction(HomeAppearanceAction.RestoreDefaultBackground)
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
            val subject = HomeAppearanceViewModel(
                preferences = repository.preferencesRepository(),
                contentPermissions = RecordingContentPermissions(onRelease = released::add),
                backgroundImageReader = RecordingBackgroundImageReader(),
            )
            startCollecting(subject.state)
            val effect = async { subject.rootActions.first() }

            // WHEN
            subject.onAction(HomeAppearanceAction.SelectBackground("content://images/new"))
            advanceUntilIdle()

            // THEN
            val preferences = repository.readyState().launcher.preferences
            assertThat(preferences.homeBackgroundUri).isNull()
            assertThat(released).containsExactly("content://images/new")
            val error = effect.await() as LauncherRootAction.ShowError
            assertThat(error.error.kind).isEqualTo(AppErrorKind.STORAGE_WRITE_FAILED)
        }
}

private class RecordingContentPermissions(
    private val onKeep: (String) -> Boolean = { true },
    private val onRelease: (String) -> Unit = {},
) : ContentPermissionManager {
    override fun keepReadAccess(uri: String): Result<Unit> =
        if (onKeep(uri)) Result.success(Unit) else Result.failure(
            IllegalStateException("Read access was not kept"),
        )

    override fun releaseReadAccess(uri: String) = onRelease(uri)
}

private class RecordingBackgroundImageReader(
    private val onRead: (String) -> Unit = {},
) : BackgroundImageReader {
    override suspend fun read(uri: String): HomeBackgroundImage? {
        onRead(uri)
        return null
    }
}
