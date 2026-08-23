package com.tomasrepcik.voidlauncher.data.model

const val DEFAULT_HOME_APP_COUNT = 8
const val MIN_HOME_APP_COUNT = 5
const val MAX_HOME_APP_COUNT = 10

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
    val homeAppCount: Int = DEFAULT_HOME_APP_COUNT,
    val hasSeenNavigationTutorial: Boolean = false,
    val homeBackgroundUri: String? = null,
    val useBackgroundColors: Boolean = false,
)

sealed interface LauncherPreferencesMutation {
    data class SetHomeAppCount(val count: Int) : LauncherPreferencesMutation
    data class SetHomeBackground(val uri: String?) : LauncherPreferencesMutation
    data class SetUseBackgroundColors(val enabled: Boolean) : LauncherPreferencesMutation
    data object MarkNavigationTutorialSeen : LauncherPreferencesMutation
}

internal fun LauncherPreferencesMutation.transition(
    current: LauncherPreferences,
): LauncherPreferences {
    val updated = when (this) {
        is LauncherPreferencesMutation.SetHomeAppCount -> current.copy(homeAppCount = count)
        is LauncherPreferencesMutation.SetHomeBackground -> current.copy(homeBackgroundUri = uri)
        is LauncherPreferencesMutation.SetUseBackgroundColors -> current.copy(
            useBackgroundColors = enabled,
        )
        LauncherPreferencesMutation.MarkNavigationTutorialSeen -> current.copy(
            hasSeenNavigationTutorial = true,
        )
    }
    return updated.copy(
        homeAppCount = updated.homeAppCount.coerceIn(MIN_HOME_APP_COUNT, MAX_HOME_APP_COUNT),
        useBackgroundColors = updated.useBackgroundColors && updated.homeBackgroundUri != null,
    )
}

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
