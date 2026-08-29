package com.tomasrepcik.voidlauncher.appcatalog.search

import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.action.LauncherAction
import java.text.Normalizer
import kotlin.math.max

private const val DEFAULT_SUGGESTION_LIMIT = 5
private const val MIN_PREFIX_QUERY_LENGTH = 2
private const val BEST_MATCH_THRESHOLD = 0.93
private const val HINT_MATCH_THRESHOLD = 0.45
private const val EXACT_MATCH_SCORE = 1.0
private const val PREFIX_MATCH_SCORE = 0.95
private const val PREFIX_LENGTH_PENALTY = 0.01
private const val CONTAINS_MATCH_SCORE = 0.82
private const val TOKEN_PREFIX_MATCH_SCORE = 0.76

enum class SearchTarget {
    BestMatch,
    Browser,
    PlayStore,
    Maps,
}

class InstalledAppSearch {
    fun filter(
        query: String,
        installedApps: List<InstalledApp>,
    ): List<InstalledApp> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return installedApps
        return installedApps.filter { app -> normalize(app.label).contains(normalizedQuery) }
    }

    fun suggestions(
        query: String,
        installedApps: List<InstalledApp>,
        limit: Int = DEFAULT_SUGGESTION_LIMIT,
    ): List<InstalledApp> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()
        return rankedMatches(normalizedQuery, installedApps)
            .filter { it.score >= HINT_MATCH_THRESHOLD }
            .take(limit)
            .map(MatchScore::app)
    }

    fun resolve(
        target: SearchTarget,
        query: String,
        installedApps: List<InstalledApp>,
    ): LauncherAction? {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return null
        return when (target) {
            SearchTarget.BestMatch -> resolveBestMatch(trimmedQuery, installedApps)
            SearchTarget.Browser -> LauncherAction.OpenWebSearch(trimmedQuery)
            SearchTarget.PlayStore -> LauncherAction.OpenPlayStoreSearch(trimmedQuery)
            SearchTarget.Maps -> LauncherAction.OpenMapsSearch(trimmedQuery)
        }
    }

    fun sectionLetter(value: String): Char {
        val first = normalize(value).firstOrNull()?.uppercaseChar() ?: '#'
        return if (first in 'A'..'Z') first else '#'
    }

    private fun resolveBestMatch(
        query: String,
        installedApps: List<InstalledApp>,
    ): LauncherAction {
        val normalizedQuery = normalize(query)
        val exactMatch = installedApps.firstOrNull { normalize(it.label) == normalizedQuery }
        if (exactMatch != null) return LauncherAction.LaunchInstalledApp(exactMatch)

        val prefixMatches = installedApps.filter { normalize(it.label).startsWith(normalizedQuery) }
        if (prefixMatches.size == 1 && normalizedQuery.length >= MIN_PREFIX_QUERY_LENGTH) {
            return LauncherAction.LaunchInstalledApp(prefixMatches.first())
        }

        val fuzzyMatch = rankedMatches(normalizedQuery, installedApps)
            .firstOrNull { it.score >= BEST_MATCH_THRESHOLD }
        return fuzzyMatch?.let { LauncherAction.LaunchInstalledApp(it.app) }
            ?: LauncherAction.OpenWebSearch(query)
    }

    private fun rankedMatches(
        normalizedQuery: String,
        installedApps: List<InstalledApp>,
    ): List<MatchScore> = installedApps
        .map { app -> MatchScore(app, score(normalizedQuery, normalize(app.label))) }
        .sortedWith(compareByDescending<MatchScore>(MatchScore::score).thenBy { it.app.sortLabel })

    private fun score(query: String, label: String): Double {
        val queryTokens = query.split(" ").filter(String::isNotBlank)
        val labelTokens = label.split(" ").filter(String::isNotBlank)
        val tokenPrefix = labelTokens.any { token -> queryTokens.any(token::startsWith) }
        return when {
            query == label -> EXACT_MATCH_SCORE
            label.startsWith(query) -> {
                PREFIX_MATCH_SCORE -
                    ((label.length - query.length).coerceAtLeast(0) * PREFIX_LENGTH_PENALTY)
            }
            label.contains(query) -> CONTAINS_MATCH_SCORE
            tokenPrefix -> TOKEN_PREFIX_MATCH_SCORE
            else -> similarity(query, label)
        }
    }

    private fun similarity(query: String, label: String): Double {
        val distance = levenshtein(query, label)
        val longestLength = max(query.length, label.length)
        if (longestLength == 0) return EXACT_MATCH_SCORE
        return (EXACT_MATCH_SCORE - distance.toDouble() / longestLength)
            .coerceIn(0.0, EXACT_MATCH_SCORE)
    }

    private fun levenshtein(lhs: String, rhs: String): Int {
        if (lhs.isEmpty()) return rhs.length
        if (rhs.isEmpty()) return lhs.length
        val costs = IntArray(rhs.length + 1) { it }
        lhs.forEachIndexed { lhsIndex, left ->
            var previousDiagonal = costs[0]
            costs[0] = lhsIndex + 1
            rhs.forEachIndexed { rhsIndex, right ->
                val previousCost = costs[rhsIndex + 1]
                val substitutionCost = if (left == right) 0 else 1
                costs[rhsIndex + 1] = minOf(
                    costs[rhsIndex + 1] + 1,
                    costs[rhsIndex] + 1,
                    previousDiagonal + substitutionCost,
                )
                previousDiagonal = previousCost
            }
        }
        return costs[rhs.length]
    }

    private fun normalize(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase()
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()
        .replace("\\s+".toRegex(), " ")

    private data class MatchScore(
        val app: InstalledApp,
        val score: Double,
    )
}
