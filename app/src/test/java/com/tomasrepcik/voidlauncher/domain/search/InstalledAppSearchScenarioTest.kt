package com.tomasrepcik.voidlauncher.domain.search

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.domain.action.LauncherAction
import org.junit.Test

class InstalledAppSearchScenarioTest {
    private val search = InstalledAppSearch()
    private val apps = listOf(
        app("Čas & Počasie"),
        app("Signal"),
        app("Signal Beta"),
        app("Supercalifragilistic"),
        app("Telegram"),
    )

    @Test
    fun givenNormalizedQueries_whenAppsAreFiltered_thenDrawerAndShortcutRulesMatch() {
        // THEN
        assertThat(search.filter("  POCASIE!! ", apps).map(InstalledApp::label))
            .containsExactly("Čas & Počasie")
        assertThat(search.filter("nal", apps).map(InstalledApp::label))
            .containsExactly("Signal", "Signal Beta").inOrder()
        assertThat(search.filter("   ", apps)).containsExactlyElementsIn(apps).inOrder()
    }

    @Test
    fun givenMatchingApps_whenHomeSuggestionsAreRequested_thenResultsAreRankedAndLimited() {
        // GIVEN
        val suggestionApps = listOf(
            app("Signal Beta"),
            app("Signals"),
            app("Signal"),
            app("Sign"),
            app("Sigil"),
            app("Silo"),
        )

        // WHEN
        val result = search.suggestions("signal", suggestionApps, limit = 3)

        // THEN
        assertThat(result.map(InstalledApp::label))
            .containsExactly("Signal", "Signals", "Signal Beta").inOrder()
    }

    @Test
    fun givenExactPrefixAndFuzzyQueries_whenPrimaryActionIsResolved_thenMatchingAppsAreLaunched() {
        // THEN
        assertThat(search.resolve(SearchTarget.Primary, "cas pocasIE", apps))
            .isEqualTo(LauncherAction.LaunchInstalledApp(apps[0]))
        assertThat(search.resolve(SearchTarget.Primary, "tele", apps))
            .isEqualTo(LauncherAction.LaunchInstalledApp(apps[4]))
        assertThat(search.resolve(SearchTarget.Primary, "Supercalifragilistix", apps))
            .isEqualTo(LauncherAction.LaunchInstalledApp(apps[3]))
    }

    @Test
    fun givenAmbiguousOrDistantQuery_whenPrimaryActionIsResolved_thenWebSearchActionIsReturned() {
        // THEN
        assertThat(search.resolve(SearchTarget.Primary, "sig", apps))
            .isEqualTo(LauncherAction.OpenWebSearch("sig"))
        assertThat(search.resolve(SearchTarget.Primary, "best ramen nearby", apps))
            .isEqualTo(LauncherAction.OpenWebSearch("best ramen nearby"))
    }

    @Test
    fun givenBlankOrUnmatchedQuery_whenSearchFeedbackIsResolved_thenNoActionOrHintIsReturned() {
        // THEN
        assertThat(search.resolve(SearchTarget.Primary, "   ", apps)).isNull()
        assertThat(search.hint("zzzzzz", apps)).isNull()
    }

    @Test
    fun givenDestinationQueries_whenActionsAreResolved_thenDestinationActionsAreReturnedDirectly() {
        // THEN
        assertThat(search.resolve(SearchTarget.Browser, " Kotlin flows ", apps))
            .isEqualTo(LauncherAction.OpenWebSearch("Kotlin flows"))
        assertThat(search.resolve(SearchTarget.PlayStore, " weather app ", apps))
            .isEqualTo(LauncherAction.OpenPlayStoreSearch("weather app"))
        assertThat(search.resolve(SearchTarget.Maps, " coffee nearby ", apps))
            .isEqualTo(LauncherAction.OpenMapsSearch("coffee nearby"))
    }

    @Test
    fun givenLabelsWithDiacriticsOrDigits_whenSectionLettersAreResolved_thenNormalizationIsApplied() {
        // THEN
        assertThat(search.sectionLetter("Škola")).isEqualTo('S')
        assertThat(search.sectionLetter("123 Player")).isEqualTo('#')
    }

    private fun app(label: String) = InstalledApp(
        key = AppKey("pkg.${label.lowercase()}", "Activity${label.lowercase()}"),
        label = label,
        sortLabel = label.lowercase(),
    )
}
