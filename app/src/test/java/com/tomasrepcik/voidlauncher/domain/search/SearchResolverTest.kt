package com.tomasrepcik.voidlauncher.domain.search

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import org.junit.Test

class SearchResolverTest {
    private val subject = SearchResolver()

    @Test
    fun givenExactInstalledAppMatch_whenResolvePrimary_thenLaunchInstalledAppIsReturned() {
        val apps = listOf(
            installedApp(label = "Spotify"),
            installedApp(label = "Signal"),
        )

        val result = subject.resolvePrimary(query = "Spotify", installedApps = apps)

        assertThat(result).isEqualTo(SearchResolution.LaunchInstalledApp(apps.first()))
    }

    @Test
    fun givenSinglePrefixInstalledAppMatch_whenResolvePrimary_thenLaunchInstalledAppIsReturned() {
        val apps = listOf(
            installedApp(label = "WhatsApp"),
            installedApp(label = "YouTube"),
        )

        val result = subject.resolvePrimary(query = "what", installedApps = apps)

        assertThat(result).isEqualTo(SearchResolution.LaunchInstalledApp(apps.first()))
    }

    @Test
    fun givenQueryWithoutDiacritics_whenResolvePrimary_thenMatchingAppIsReturned() {
        val apps = listOf(
            installedApp(label = "Minúta"),
            installedApp(label = "Mapy"),
        )

        val result = subject.resolvePrimary(query = "minuta", installedApps = apps)

        assertThat(result).isEqualTo(SearchResolution.LaunchInstalledApp(apps.first()))
    }

    @Test
    fun givenUnknownQuery_whenResolvePrimary_thenWebSearchIsReturned() {
        val apps = listOf(
            installedApp(label = "Maps"),
            installedApp(label = "Camera"),
        )

        val result = subject.resolvePrimary(query = "best ramen nearby", installedApps = apps)

        assertThat(result).isEqualTo(SearchResolution.WebSearch("best ramen nearby"))
    }

    @Test
    fun givenTypoQuery_whenResolveHint_thenBestInstalledAppHintIsReturned() {
        val apps = listOf(
            installedApp(label = "Telegram"),
            installedApp(label = "Threads"),
        )

        val result = subject.resolveHint(query = "telegrm", installedApps = apps)

        assertThat(result).isEqualTo(SearchResolution.AppHint(apps.first()))
    }

    @Test
    fun givenMapsQuery_whenResolveMaps_thenMapsSearchIsReturned() {
        val result = subject.resolveMaps(query = "coffee near me")

        assertThat(result).isEqualTo(SearchResolution.MapsSearch("coffee near me"))
    }

    private fun installedApp(label: String): InstalledApp = InstalledApp(
        key = AppKey(
            packageName = "pkg.${label.lowercase()}",
            activityName = "Activity${label.lowercase()}",
        ),
        label = label,
        sortLabel = label.lowercase(),
    )
}
