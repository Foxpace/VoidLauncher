package com.tomasrepcik.voidlauncher.data.repository

import android.database.sqlite.SQLiteException
import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.error.AppErrorKind
import com.tomasrepcik.voidlauncher.testing.PlannedRepositoryFailures
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.launcherRepository
import com.tomasrepcik.voidlauncher.testing.readyState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherRepositoryTest {
    @Test
    fun initializationFailureBlocksUntilRetryThenCreatesDefaults() = runTest {
        val repository = launcherRepository(
            failures = PlannedRepositoryFailures(initializationCount = 1),
        )
        advanceUntilIdle()

        val failure = repository.state.value as LauncherRepositoryState.InitializationError
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.STORAGE_INITIALIZATION_FAILED)
        assertThat(failure.error.cause).isInstanceOf(SQLiteException::class.java)

        repository.retryInitialization()
        advanceUntilIdle()

        val launcher = repository.readyState().launcher
        val slots = launcher.bottomShortcuts.map { it.slot }
        assertThat(slots)
            .containsExactly(ShortcutSlot.LEFT, ShortcutSlot.RIGHT).inOrder()
    }

    @Test
    fun writeFailurePreservesLastValidStateAndReturnsError() = runTest {
        val camera = installedApp("Camera")
        val repository = launcherRepository(
            installedApps = listOf(camera),
            pinnedApps = listOf(camera),
            failures = PlannedRepositoryFailures(writeCount = 1),
        )
        advanceUntilIdle()
        val before = repository.readyState().launcher

        val outcome = repository.removeHomeApp(camera.key)
        advanceUntilIdle()

        val failure = outcome as RepositoryMutationOutcome.Failed
        val ready = repository.readyState()
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.STORAGE_WRITE_FAILED)
        assertThat(failure.error.cause).isInstanceOf(SQLiteException::class.java)
        assertThat(ready.launcher).isEqualTo(before)
    }

    @Test
    fun namedMutationsUpdateObservableLauncherState() = runTest {
        val camera = installedApp("Camera")
        val maps = installedApp("Maps")
        val repository = launcherRepository(installedApps = listOf(camera, maps))
        advanceUntilIdle()

        repository.saveHomeApps(listOf(camera.key, maps.key, camera.key))
        repository.renameHomeApp(maps.key, "Navigation")
        repository.reorderHomeApps(1, 0)
        repository.setHomeAppCount(100)
        advanceUntilIdle()

        val launcher = repository.readyState().launcher
        assertThat(launcher.pinnedHomeApps.map { it.label })
            .containsExactly("Navigation", "Camera").inOrder()
        assertThat(launcher.preferences.homeAppCount).isEqualTo(10)
    }
}
