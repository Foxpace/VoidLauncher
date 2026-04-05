package com.tomasrepcik.voidlauncher.domain.search

import java.text.Normalizer

fun normalizeSearchText(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase()
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()
        .replace("\\s+".toRegex(), " ")

fun matchesSearchQuery(value: String, query: String): Boolean {
    val normalizedQuery = normalizeSearchText(query)
    if (normalizedQuery.isEmpty()) return true
    return normalizeSearchText(value).contains(normalizedQuery)
}

fun startsWithSearchQuery(value: String, query: String): Boolean {
    val normalizedQuery = normalizeSearchText(query)
    if (normalizedQuery.isEmpty()) return false
    return normalizeSearchText(value).startsWith(normalizedQuery)
}

fun searchSectionLetter(value: String): Char {
    val first = normalizeSearchText(value).firstOrNull()?.uppercaseChar() ?: '#'
    return if (first in 'A'..'Z') first else '#'
}
