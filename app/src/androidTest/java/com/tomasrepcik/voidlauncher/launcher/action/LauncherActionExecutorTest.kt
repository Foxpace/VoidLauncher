package com.tomasrepcik.voidlauncher.launcher.action

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
    private val copiedText = mutableListOf<String>()
    private val executor = LauncherActionExecutor(
        copyText = { copiedText += it },
        openApp = appLauncher::open,
        installedApplicationFlags = appLauncher::installedApplicationFlags,
    )
    private val exampleApp = installedApp(
        label = "Example",
        packageName = "dev.example",
        activityName = "dev.example.MainActivity",
    )

    @Test
    fun givenAssistantAcceptsText_whenPromptIsSent_thenTextAndDestinationArePreserved() {
        TextAssistant.entries.forEach { assistant ->
            // GIVEN
            val prompt = "Explain Čas & Počasie? + ☀️"

            // WHEN
            val outcome = executor.execute(LauncherAction.AskAssistant(assistant, prompt))
            val intent = appLauncher.started.last()

            // THEN
            assertThat(outcome).isEqualTo(LauncherActionOutcome.Completed)
            assertThat(intent.action).isEqualTo(Intent.ACTION_SEND)
            assertThat(intent.type).isEqualTo("text/plain")
            assertThat(intent.`package`).isEqualTo(assistant.packageName)
            assertThat(intent.getStringExtra(Intent.EXTRA_TEXT)).isEqualTo(prompt)
            assertThat(copiedText).isEmpty()
        }
    }

    @Test
    fun givenAssistantCannotReceiveText_whenPromptIsSent_thenPromptIsCopiedAndWebsiteOpens() {
        TextAssistant.entries.forEach { assistant ->
            // GIVEN
            appLauncher.startResults.addAll(listOf(false, true))
            val prompt = "Explain Kotlin"

            // WHEN
            val outcome = executor.execute(LauncherAction.AskAssistant(assistant, prompt))

            // THEN
            assertThat(outcome).isEqualTo(LauncherActionOutcome.Recovered(ErrorRecovery.ASSISTANT_WEBSITE))
            assertThat(copiedText.last()).isEqualTo(prompt)
            assertThat(appLauncher.started.last().dataString).isEqualTo(assistant.website)
        }
    }

    @Test
    fun givenAssistantAndBrowserUnavailable_whenPromptIsSent_thenFailureIsReported() {
        // GIVEN
        appLauncher.startResults.addAll(listOf(false, false))

        // WHEN
        val outcome = executor.execute(LauncherAction.AskAssistant(TextAssistant.Claude, "Explain Kotlin"))

        // THEN
        assertThat((outcome as LauncherActionOutcome.Failed).error.kind)
            .isEqualTo(AppErrorKind.DESTINATION_UNAVAILABLE)
        assertThat(copiedText).containsExactly("Explain Kotlin")
    }

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
    fun givenTextWithSpecialCharacters_whenBrowserOpens_thenUrlPreservesTheWholeQuery() {
        // GIVEN
        val query = "Čas & Počasie + C++? #today / 50% ☀️\nsecond line"

        // WHEN
        val outcome = executor.execute(LauncherAction.OpenWebSearch(query))
        val intent = appLauncher.started.single()
        val uri = requireNotNull(intent.data)

        // THEN
        assertThat(outcome).isEqualTo(LauncherActionOutcome.Completed)
        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(uri.scheme).isEqualTo("https")
        assertThat(uri.host).isEqualTo("www.google.com")
        assertThat(uri.path).isEqualTo("/search")
        assertThat(uri.queryParameterNames).containsExactly("q")
        assertThat(uri.getQueryParameter("q")).isEqualTo(query)
        assertThat(uri.fragment).isNull()
        assertThat(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0)
    }

    @Test
    fun givenConsecutiveSearches_whenBrowserIsOpenedAgain_thenEachUrlHasItsOwnQuery() {
        // GIVEN
        val queries = listOf("first search", "different & second search")

        // WHEN
        queries.forEach { query -> executor.execute(LauncherAction.OpenWebSearch(query)) }

        // THEN
        val sentQueries = appLauncher.started.map { it.data?.getQueryParameter("q") }
        assertThat(sentQueries).containsExactlyElementsIn(queries).inOrder()
        assertThat(appLauncher.started.map(Intent::getAction))
            .containsExactly(Intent.ACTION_VIEW, Intent.ACTION_VIEW)
    }

    @Test
    fun givenUnavailableBrowser_whenSearchRuns_thenFailureIsReported() {
        // GIVEN
        appLauncher.startResults += false

        // WHEN
        val outcome = executor.execute(LauncherAction.OpenWebSearch("weather"))

        // THEN
        val failure = outcome as LauncherActionOutcome.Failed
        assertThat(failure.error.kind).isEqualTo(AppErrorKind.DESTINATION_UNAVAILABLE)
        assertThat(appLauncher.started).hasSize(1)
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
