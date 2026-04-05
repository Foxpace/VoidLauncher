package com.tomasrepcik.voidlauncher.domain.search

import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import kotlin.math.max

sealed interface SearchResolution {
    data class LaunchInstalledApp(val app: InstalledApp) : SearchResolution
    data class WebSearch(val query: String) : SearchResolution
    data class PlayStoreSearch(val query: String) : SearchResolution
    data class MapsSearch(val query: String) : SearchResolution
    data class AppHint(val app: InstalledApp) : SearchResolution
    data class NoMatch(val query: String) : SearchResolution
}

class SearchResolver {
    fun resolvePrimary(query: String, installedApps: List<InstalledApp>): SearchResolution {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return SearchResolution.NoMatch(query = "")
        }

        val normalizedQuery = normalizeSearchText(trimmedQuery)
        val exactMatch = installedApps.firstOrNull { normalizeSearchText(it.label) == normalizedQuery }
        if (exactMatch != null) {
            return SearchResolution.LaunchInstalledApp(exactMatch)
        }

        val prefixMatches = installedApps.filter {
            normalizeSearchText(it.label).startsWith(normalizedQuery)
        }
        if (prefixMatches.size == 1 && normalizedQuery.length >= 2) {
            return SearchResolution.LaunchInstalledApp(prefixMatches.first())
        }

        val bestMatch = findBestMatch(trimmedQuery, installedApps)
        return if (bestMatch != null && bestMatch.score >= 0.93) {
            SearchResolution.LaunchInstalledApp(bestMatch.app)
        } else {
            SearchResolution.WebSearch(trimmedQuery)
        }
    }

    fun resolvePlayStore(query: String): SearchResolution {
        val trimmedQuery = query.trim()
        return if (trimmedQuery.isEmpty()) {
            SearchResolution.NoMatch(query = "")
        } else {
            SearchResolution.PlayStoreSearch(trimmedQuery)
        }
    }

    fun resolveBrowser(query: String): SearchResolution {
        val trimmedQuery = query.trim()
        return if (trimmedQuery.isEmpty()) {
            SearchResolution.NoMatch(query = "")
        } else {
            SearchResolution.WebSearch(trimmedQuery)
        }
    }

    fun resolveMaps(query: String): SearchResolution {
        val trimmedQuery = query.trim()
        return if (trimmedQuery.isEmpty()) {
            SearchResolution.NoMatch(query = "")
        } else {
            SearchResolution.MapsSearch(trimmedQuery)
        }
    }

    fun resolveHint(query: String, installedApps: List<InstalledApp>): SearchResolution {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return SearchResolution.NoMatch(query = "")
        }

        val bestMatch = findBestMatch(trimmedQuery, installedApps)
        return if (bestMatch != null && bestMatch.score >= 0.45) {
            SearchResolution.AppHint(bestMatch.app)
        } else {
            SearchResolution.NoMatch(trimmedQuery)
        }
    }

    private fun findBestMatch(
        query: String,
        installedApps: List<InstalledApp>,
    ): MatchScore? {
        val normalizedQuery = normalizeSearchText(query)
        return installedApps
            .map { app ->
                MatchScore(
                    app = app,
                    score = calculateScore(normalizedQuery, normalizeSearchText(app.label)),
                )
            }
            .maxByOrNull(MatchScore::score)
    }

    private fun calculateScore(query: String, label: String): Double {
        if (query == label) return 1.0
        if (label.startsWith(query)) {
            return 0.95 - ((label.length - query.length).coerceAtLeast(0) * 0.01)
        }
        if (label.contains(query)) {
            return 0.82
        }

        val queryTokens = query.split(" ").filter(String::isNotBlank)
        val labelTokens = label.split(" ").filter(String::isNotBlank)
        val tokenPrefix = labelTokens.any { token -> queryTokens.any { token.startsWith(it) } }
        if (tokenPrefix) {
            return 0.76
        }

        val editDistance = levenshtein(query, label)
        val similarity = 1.0 - (editDistance.toDouble() / max(query.length, label.length))
        return similarity.coerceIn(0.0, 1.0)
    }

    private fun levenshtein(lhs: String, rhs: String): Int {
        if (lhs.isEmpty()) return rhs.length
        if (rhs.isEmpty()) return lhs.length
        val costs = IntArray(rhs.length + 1) { it }

        lhs.forEachIndexed { i, left ->
            var previousDiagonal = costs[0]
            costs[0] = i + 1
            rhs.forEachIndexed { j, right ->
                val temp = costs[j + 1]
                val substitutionCost = if (left == right) 0 else 1
                costs[j + 1] = minOf(
                    costs[j + 1] + 1,
                    costs[j] + 1,
                    previousDiagonal + substitutionCost,
                )
                previousDiagonal = temp
            }
        }
        return costs[rhs.length]
    }

    private data class MatchScore(
        val app: InstalledApp,
        val score: Double,
    )
}
