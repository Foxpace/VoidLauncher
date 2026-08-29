package com.tomasrepcik.voidlauncher.appearance

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.launcher.error.AppErrorKind
import com.tomasrepcik.voidlauncher.launcher.error.AppOperation
import com.tomasrepcik.voidlauncher.testing.MainDispatcherRule
import com.tomasrepcik.voidlauncher.testing.PlannedRepositoryFailures
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.preferencesRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import com.tomasrepcik.voidlauncher.testing.startCollecting
import com.tomasrepcik.voidlauncher.launcher.LauncherRootAction
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
            val contentPermissions = RecordingContentPermissions(
                onKeep = { uri -> taken += uri; true },
                onRelease = released::add,
            )
            val backgroundImageReader = RecordingBackgroundImageReader(decoded::add)
            val subject = HomeAppearanceViewModel(
                preferences = preferencesRepository,
                keepBackgroundReadAccess = contentPermissions::keepReadAccess,
                releaseBackgroundReadAccess = contentPermissions::releaseReadAccess,
                readBackground = backgroundImageReader::read,
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
            val contentPermissions = RecordingContentPermissions(onRelease = released::add)
            val backgroundImageReader = RecordingBackgroundImageReader()
            val subject = HomeAppearanceViewModel(
                preferences = repository.preferencesRepository(),
                keepBackgroundReadAccess = contentPermissions::keepReadAccess,
                releaseBackgroundReadAccess = contentPermissions::releaseReadAccess,
                readBackground = backgroundImageReader::read,
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

    @Test
    fun givenBackgroundAccessFailure_whenNewBackgroundIsSelected_thenExistingBackgroundIsPreserved() =
        runTest(mainDispatcherRule.dispatcher) {
            // GIVEN
            val repository = launcherRepository()
            advanceUntilIdle()
            val preferencesRepository = repository.preferencesRepository()
            preferencesRepository.setHomeBackground("content://images/existing")
            advanceUntilIdle()
            val released = mutableListOf<String>()
            val contentPermissions = RecordingContentPermissions(
                onKeep = { false },
                onRelease = released::add,
            )
            val backgroundImageReader = RecordingBackgroundImageReader()
            val subject = HomeAppearanceViewModel(
                preferences = preferencesRepository,
                keepBackgroundReadAccess = contentPermissions::keepReadAccess,
                releaseBackgroundReadAccess = contentPermissions::releaseReadAccess,
                readBackground = backgroundImageReader::read,
            )
            startCollecting(subject.state)
            advanceUntilIdle()
            val error = async { subject.rootActions.first() }

            // WHEN
            subject.onAction(HomeAppearanceAction.SelectBackground("content://images/rejected"))
            advanceUntilIdle()

            // THEN
            val preferences = repository.readyState().launcher.preferences
            assertThat(preferences.homeBackgroundUri).isEqualTo("content://images/existing")
            assertThat(subject.state.value.backgroundUri).isEqualTo("content://images/existing")
            assertThat(released).isEmpty()
            val action = error.await() as LauncherRootAction.ShowError
            assertThat(action.error.kind).isEqualTo(AppErrorKind.BACKGROUND_ACCESS_FAILED)
            assertThat(action.error.operation).isEqualTo(AppOperation.SAVE_HOME_BACKGROUND)
        }
}

private class RecordingContentPermissions(
    private val onKeep: (String) -> Boolean = { true },
    private val onRelease: (String) -> Unit = {},
) {
    fun keepReadAccess(uri: String): Result<Unit> =
        if (onKeep(uri)) Result.success(Unit) else Result.failure(
            IllegalStateException("Read access was not kept"),
        )

    fun releaseReadAccess(uri: String) = onRelease(uri)
}

private class RecordingBackgroundImageReader(
    private val onRead: (String) -> Unit = {},
) {
    fun read(uri: String): HomeBackgroundImage? {
        onRead(uri)
        return null
    }
}
