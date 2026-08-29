package com.tomasrepcik.voidlauncher.launcher.action

import android.app.SearchManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.launcher.AppKey
import com.tomasrepcik.voidlauncher.launcher.ResolvedShortcut
import com.tomasrepcik.voidlauncher.launcher.ShortcutSelection
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot
import com.tomasrepcik.voidlauncher.launcher.error.AppErrorKind
import com.tomasrepcik.voidlauncher.launcher.error.ErrorRecovery
import com.tomasrepcik.voidlauncher.testing.installedApp
import org.junit.Test

class LauncherActionExecutorTest {
    private val appLauncher = RecordingAppLauncher()
    private val executor = LauncherActionExecutor(
        openApp = appLauncher::open,
        installedApplicationFlags = appLauncher::installedApplicationFlags,
    )
    private val exampleApp = installedApp(
        label = "Example",
        packageName = "dev.example",
        activityName = "dev.example.MainActivity",
    )

    @Test
    fun givenInstalledApp_whenLaunched_thenActionCompletes() {
        // GIVEN
        val action = LauncherAction.LaunchInstalledApp(exampleApp)

        // WHEN
        val outcome = executor.execute(action)
        val startedIntent = appLauncher.started.single()
        val startedPackage = startedIntent.component?.packageName

        // THEN
        assertThat(outcome).isEqualTo(LauncherActionOutcome.Completed)
        assertThat(startedPackage).isEqualTo("dev.example")
    }

    @Test
    fun givenMissingApp_whenLaunched_thenAppUnavailableIsReturned() {
        // GIVEN
        appLauncher.startResults += false

        // WHEN
        val outcome = executor.execute(LauncherAction.LaunchInstalledApp(exampleApp))

        // THEN
        val failure = outcome as LauncherActionOutcome.Failed
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.APP_UNAVAILABLE)
    }

    @Test
    fun givenUnavailableSearchApp_whenWebSearchRuns_thenBrowserFallbackCompletes() {
        // GIVEN
        appLauncher.startResults.addAll(listOf(false, true))

        // WHEN
        val outcome = executor.execute(LauncherAction.OpenWebSearch("weather"))

        // THEN
        assertThat(outcome).isEqualTo(
            LauncherActionOutcome.Recovered(ErrorRecovery.WEB_SEARCH_PAGE),
        )
        assertThat(appLauncher.started.map(Intent::getAction)).containsExactly(
            Intent.ACTION_WEB_SEARCH,
            Intent.ACTION_VIEW,
        ).inOrder()
        assertThat(appLauncher.started.first().getStringExtra(SearchManager.QUERY))
            .isEqualTo("weather")
        assertThat(appLauncher.started.last().dataString).contains("weather")
    }

    @Test
    fun givenUnavailableAppShortcut_whenOpened_thenAppUnavailableIsReturnedWithoutLaunch() {
        // GIVEN
        val shortcut = ResolvedShortcut(
            slot = ShortcutSlot.LEFT,
            label = "Missing",
            selection = ShortcutSelection.AppShortcut(AppKey("missing", "MissingActivity")),
            isAvailable = false,
        )

        // WHEN
        val outcome = executor.execute(LauncherAction.OpenShortcut(shortcut))

        // THEN
        val failure = outcome as LauncherActionOutcome.Failed
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.APP_UNAVAILABLE)
        assertThat(appLauncher.started).isEmpty()
    }

    @Test
    fun givenSystemApp_whenUninstallRuns_thenAppInfoRecoveryCompletes() {
        // GIVEN
        appLauncher.flags = ApplicationInfo.FLAG_SYSTEM

        // WHEN
        val outcome = executor.execute(LauncherAction.UninstallApp(exampleApp))

        // THEN
        assertThat(outcome).isEqualTo(
            LauncherActionOutcome.Recovered(ErrorRecovery.SYSTEM_APP_INFO),
        )
        assertThat(appLauncher.flagRequests).containsExactly("dev.example")
        assertThat(appLauncher.started.single().action)
            .isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    }

    @Test
    fun givenUnavailableUninstallerAndAppInfo_whenUninstallRuns_thenRecoveryFailureIsReturned() {
        // GIVEN
        appLauncher.startResults.addAll(listOf(false, false))

        // WHEN
        val outcome = executor.execute(LauncherAction.UninstallApp(exampleApp))

        // THEN
        val failure = outcome as LauncherActionOutcome.Failed
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.DESTINATION_UNAVAILABLE)
        assertThat(failure.error.recovery)
            .isEqualTo(ErrorRecovery.UNINSTALL_UNAVAILABLE_APP_INFO)
        assertThat(appLauncher.started.map(Intent::getAction)).containsExactly(
            Intent.ACTION_DELETE,
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        ).inOrder()
    }

    @Test
    fun givenUnexpectedDefect_whenActionRuns_thenAppErrorContainsDefect() {
        // GIVEN
        val defect = IllegalStateException("defect")
        appLauncher.unexpectedFailure = defect

        // WHEN
        val outcome = executor.execute(LauncherAction.LaunchInstalledApp(exampleApp))

        // THEN
        val failure = outcome as LauncherActionOutcome.Failed
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.UNEXPECTED)
        assertThat(failure.error.cause).isSameInstanceAs(defect)
    }
}

private class RecordingAppLauncher {
    val started = mutableListOf<Intent>()
    val startResults = ArrayDeque<Boolean>()
    val flagRequests = mutableListOf<String>()
    var flags = 0
    var unexpectedFailure: RuntimeException? = null

    fun open(intent: Intent): Boolean {
        unexpectedFailure?.let { throw it }
        started += intent
        return startResults.removeFirstOrNull() ?: true
    }

    fun installedApplicationFlags(packageName: String): Int {
        flagRequests += packageName
        return flags
    }
}
