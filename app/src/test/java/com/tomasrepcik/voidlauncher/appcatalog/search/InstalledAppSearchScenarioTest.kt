package com.tomasrepcik.voidlauncher.appcatalog.search

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.action.LauncherAction
import com.tomasrepcik.voidlauncher.testing.installedApp
import org.junit.Test

class InstalledAppSearchScenarioTest {
    private val search = InstalledAppSearch()
    private val apps = listOf(
        installedApp("Čas & Počasie"),
        installedApp("Signal"),
        installedApp("Signal Beta"),
        installedApp("Supercalifragilistic"),
        installedApp("Telegram"),
    )

    @Test
    fun givenNormalizedQueries_whenAppsAreFiltered_thenDrawerAndShortcutRulesMatch() {
        // GIVEN
        val normalizedQuery = "  POCASIE!! "
        val substringQuery = "nal"
        val blankQuery = "   "

        // WHEN
        val normalizedMatches = search.filter(normalizedQuery, apps)
        val substringMatches = search.filter(substringQuery, apps)
        val blankMatches = search.filter(blankQuery, apps)

        // THEN
        assertThat(normalizedMatches.map(InstalledApp::label))
            .containsExactly("Čas & Počasie")
        assertThat(substringMatches.map(InstalledApp::label))
            .containsExactly("Signal", "Signal Beta").inOrder()
        assertThat(blankMatches).containsExactlyElementsIn(apps).inOrder()
    }

    @Test
    fun givenMatchingApps_whenHomeSuggestionsAreRequested_thenResultsAreRankedAndLimited() {
        // GIVEN
        val suggestionApps = listOf(
            installedApp("Signal Beta"),
            installedApp("Signals"),
            installedApp("Signal"),
            installedApp("Sign"),
            installedApp("Sigil"),
            installedApp("Silo"),
        )

        // WHEN
        val result = search.suggestions("signal", suggestionApps, limit = 3)

        // THEN
        assertThat(result.map(InstalledApp::label))
            .containsExactly("Signal", "Signals", "Signal Beta").inOrder()
    }

    @Test
    fun givenExactPrefixAndFuzzyQueries_whenBestMatchIsResolved_thenMatchingAppsAreOpened() {
        // GIVEN
        val exactQuery = "cas pocasIE"
        val prefixQuery = "tele"
        val fuzzyQuery = "Supercalifragilistix"

        // WHEN
        val exactMatch = search.resolve(SearchTarget.BestMatch, exactQuery, apps)
        val prefixMatch = search.resolve(SearchTarget.BestMatch, prefixQuery, apps)
        val fuzzyMatch = search.resolve(SearchTarget.BestMatch, fuzzyQuery, apps)

        // THEN
        assertThat(exactMatch)
            .isEqualTo(LauncherAction.LaunchInstalledApp(apps[0]))
        assertThat(prefixMatch)
            .isEqualTo(LauncherAction.LaunchInstalledApp(apps[4]))
        assertThat(fuzzyMatch)
            .isEqualTo(LauncherAction.LaunchInstalledApp(apps[3]))
    }

    @Test
    fun givenAmbiguousOrDistantQuery_whenBestMatchIsResolved_thenWebSearchIsReturned() {
        // GIVEN
        val ambiguousQuery = "sig"
        val distantQuery = "best ramen nearby"

        // WHEN
        val ambiguousMatch = search.resolve(SearchTarget.BestMatch, ambiguousQuery, apps)
        val distantMatch = search.resolve(SearchTarget.BestMatch, distantQuery, apps)

        // THEN
        assertThat(ambiguousMatch).isEqualTo(LauncherAction.OpenWebSearch(ambiguousQuery))
        assertThat(distantMatch).isEqualTo(LauncherAction.OpenWebSearch(distantQuery))
    }

    @Test
    fun givenBlankQuery_whenBestMatchIsResolved_thenNoActionIsReturned() {
        // GIVEN
        val query = "   "

        // WHEN
        val result = search.resolve(SearchTarget.BestMatch, query, apps)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun givenDestinationQueries_whenActionsAreResolved_thenDestinationActionsAreReturnedDirectly() {
        // GIVEN
        val browserQuery = " Kotlin flows "
        val storeQuery = " weather app "
        val mapsQuery = " coffee nearby "

        // WHEN
        val browserAction = search.resolve(SearchTarget.Browser, browserQuery, apps)
        val storeAction = search.resolve(SearchTarget.PlayStore, storeQuery, apps)
        val mapsAction = search.resolve(SearchTarget.Maps, mapsQuery, apps)

        // THEN
        assertThat(browserAction)
            .isEqualTo(LauncherAction.OpenWebSearch("Kotlin flows"))
        assertThat(storeAction)
            .isEqualTo(LauncherAction.OpenPlayStoreSearch("weather app"))
        assertThat(mapsAction)
            .isEqualTo(LauncherAction.OpenMapsSearch("coffee nearby"))
    }

    @Test
    fun givenLabelsWithDiacriticsOrDigits_whenSectionLettersAreResolved_thenNormalizationIsApplied() {
        // GIVEN
        val diacriticLabel = "Škola"
        val numericLabel = "123 Player"

        // WHEN
        val diacriticSection = search.sectionLetter(diacriticLabel)
        val numericSection = search.sectionLetter(numericLabel)

        // THEN
        assertThat(diacriticSection).isEqualTo('S')
        assertThat(numericSection).isEqualTo('#')
    }
}
