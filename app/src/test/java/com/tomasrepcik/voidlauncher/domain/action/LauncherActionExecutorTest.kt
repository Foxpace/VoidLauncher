package com.tomasrepcik.voidlauncher.domain.action

import android.content.pm.ApplicationInfo
import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.error.AppErrorKind
import com.tomasrepcik.voidlauncher.domain.error.ErrorRecovery
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LauncherActionExecutorTest {
    private val platform = RecordingLauncherActionPlatform()
    private val executor = LauncherActionExecutor()

    @Test
    fun givenInstalledApp_whenLaunched_thenActionCompletes() = runTest {
        // WHEN
        val outcome = executor.execute(LauncherAction.LaunchInstalledApp(app()), platform)

        // THEN
        assertThat(outcome).isEqualTo(LauncherActionOutcome.Completed)
        assertThat(platform.calls).containsExactly("launch:dev.example")
    }

    @Test
    fun givenMissingApp_whenLaunched_thenAppUnavailableIsReturned() = runTest {
        // GIVEN
        platform.launchInstalledApp = false

        // WHEN
        val outcome = executor.execute(LauncherAction.LaunchInstalledApp(app()), platform)

        // THEN
        assertThat(outcome).isInstanceOf(LauncherActionOutcome.Failed::class.java)
        val failure = outcome as LauncherActionOutcome.Failed
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.APP_UNAVAILABLE)
    }

    @Test
    fun givenUnavailableSearchApp_whenWebSearchRuns_thenBrowserFallbackCompletes() = runTest {
        // GIVEN
        platform.openWebSearch = false

        // WHEN
        val outcome = executor.execute(LauncherAction.OpenWebSearch("weather"), platform)

        // THEN
        assertThat(outcome).isEqualTo(
            LauncherActionOutcome.Recovered(ErrorRecovery.BROWSER_FALLBACK),
        )
        assertThat(platform.calls).containsExactly("web:weather", "browser:weather").inOrder()
    }

    @Test
    fun givenUnavailableAppShortcut_whenOpened_thenAppUnavailableIsReturnedWithoutLaunch() = runTest {
        // GIVEN
        val shortcut = ResolvedShortcut(
            slot = ShortcutSlot.LEFT,
            label = "Missing",
            selection = ShortcutSelection.AppShortcut(AppKey("missing", "MissingActivity")),
            isAvailable = false,
        )

        // WHEN
        val outcome = executor.execute(LauncherAction.OpenShortcut(shortcut), platform)

        // THEN
        val failure = outcome as LauncherActionOutcome.Failed
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.APP_UNAVAILABLE)
        assertThat(platform.calls).isEmpty()
    }

    @Test
    fun givenSystemApp_whenUninstallRuns_thenAppInfoRecoveryCompletes() = runTest {
        // GIVEN
        platform.applicationFlags = ApplicationInfo.FLAG_SYSTEM

        // WHEN
        val outcome = executor.execute(LauncherAction.UninstallApp(app()), platform)

        // THEN
        assertThat(outcome).isEqualTo(
            LauncherActionOutcome.Recovered(
                ErrorRecovery.SYSTEM_APP_INFO,
            ),
        )
        assertThat(platform.calls).containsExactly("flags:dev.example", "info:dev.example").inOrder()
    }

    @Test
    fun givenUnavailableUninstallerAndAppInfo_whenUninstallRuns_thenRecoveryFailureIsReturned() = runTest {
        // GIVEN
        platform.openUninstaller = false
        platform.openAppInfo = false

        // WHEN
        val outcome = executor.execute(LauncherAction.UninstallApp(app()), platform)

        // THEN
        val failure = outcome as LauncherActionOutcome.Failed
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.DESTINATION_UNAVAILABLE)
        assertThat(failure.error.recovery)
            .isEqualTo(ErrorRecovery.UNINSTALL_UNAVAILABLE_APP_INFO)
        assertThat(platform.calls).containsExactly(
            "flags:dev.example",
            "uninstall:dev.example",
            "info:dev.example",
        ).inOrder()
    }

    @Test
    fun givenUnexpectedDefect_whenActionRuns_thenAppErrorContainsDefect() = runTest {
        // GIVEN
        val defect = IllegalStateException("defect")
        platform.unexpectedFailure = defect

        // WHEN
        val outcome = executor.execute(LauncherAction.LaunchInstalledApp(app()), platform)

        // THEN
        val failure = outcome as LauncherActionOutcome.Failed
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.UNEXPECTED)
        assertThat(failure.error.cause).isSameInstanceAs(defect)
    }

    private fun app() = InstalledApp(
        key = AppKey("dev.example", "dev.example.MainActivity"),
        label = "Example",
        sortLabel = "example",
    )
}

private class RecordingLauncherActionPlatform : LauncherActionPlatform {
    val calls = mutableListOf<String>()
    var launchInstalledApp = true
    var openShortcut = true
    var openWebSearch = true
    var openBrowserSearch = true
    var openPlayStore = true
    var openPlayStoreWebsite = true
    var openMaps = true
    var openMapsWebsite = true
    var applicationFlags = 0
    var openUninstaller = true
    var openAppInfo = true
    var unexpectedFailure: RuntimeException? = null

    override fun launchInstalledApp(app: InstalledApp): Boolean {
        unexpectedFailure?.let { throw it }
        calls += "launch:${app.key.packageName}"
        return launchInstalledApp
    }

    override fun openShortcut(shortcut: ResolvedShortcut): Boolean {
        calls += "shortcut:${shortcut.label}"
        return openShortcut
    }

    override fun openWebSearch(query: String): Boolean {
        calls += "web:$query"
        return openWebSearch
    }

    override fun openBrowserSearch(query: String): Boolean {
        calls += "browser:$query"
        return openBrowserSearch
    }

    override fun openPlayStore(query: String): Boolean {
        calls += "store:$query"
        return openPlayStore
    }

    override fun openPlayStoreWebsite(query: String): Boolean {
        calls += "storeWeb:$query"
        return openPlayStoreWebsite
    }

    override fun openMaps(query: String): Boolean {
        calls += "maps:$query"
        return openMaps
    }

    override fun openMapsWebsite(query: String): Boolean {
        calls += "mapsWeb:$query"
        return openMapsWebsite
    }

    override fun applicationFlags(packageName: String): Int {
        calls += "flags:$packageName"
        return applicationFlags
    }

    override fun openUninstaller(packageName: String): Boolean {
        calls += "uninstall:$packageName"
        return openUninstaller
    }

    override fun openAppInfo(packageName: String): Boolean {
        calls += "info:$packageName"
        return openAppInfo
    }
}
