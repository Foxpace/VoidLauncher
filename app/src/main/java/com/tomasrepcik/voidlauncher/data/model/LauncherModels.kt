package com.tomasrepcik.voidlauncher.data.model

data class AppKey(
    val packageName: String,
    val activityName: String,
)

data class InstalledApp(
    val key: AppKey,
    val label: String,
    val sortLabel: String,
)

data class LauncherPreferences(
    val hasSeenNavigationTutorial: Boolean = false,
    val homeBackgroundUri: String? = null,
    val useBackgroundColors: Boolean = false,
)

internal fun LauncherPreferences.withHomeBackground(uri: String?): LauncherPreferences = copy(
    homeBackgroundUri = uri,
    useBackgroundColors = useBackgroundColors && uri != null,
)

internal fun LauncherPreferences.withBackgroundColorsEnabled(
    enabled: Boolean,
): LauncherPreferences = copy(
    useBackgroundColors = enabled && homeBackgroundUri != null,
)

internal fun LauncherPreferences.withNavigationTutorialSeen(): LauncherPreferences = copy(
    hasSeenNavigationTutorial = true,
)

enum class ShortcutSlot {
    LEFT,
    RIGHT,
}

sealed interface ShortcutSelection {
    data object SystemContacts : ShortcutSelection
    data object SystemCamera : ShortcutSelection
    data class AppShortcut(val key: AppKey) : ShortcutSelection
}

data class ResolvedShortcut(
    val slot: ShortcutSlot,
    val label: String,
    val selection: ShortcutSelection,
    val installedApp: InstalledApp? = null,
    val isAvailable: Boolean = true,
)
