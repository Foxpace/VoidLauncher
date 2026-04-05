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
