package com.tomasrepcik.voidlauncher.drawer

import com.tomasrepcik.voidlauncher.launcher.AppKey
import com.tomasrepcik.voidlauncher.launcher.InstalledApp

data class DrawerUiState(
    val query: String = "",
    val apps: List<InstalledApp> = emptyList(),
    val pinnedAppKeys: Set<AppKey> = emptySet(),
    val sectionLetters: Map<AppKey, Char> = emptyMap(),
    val alphabetIndex: Map<Char, Int> = emptyMap(),
    val isLoading: Boolean = true,
)

sealed interface DrawerAction {
    data object Back : DrawerAction
    data object OpenCustomization : DrawerAction
    data class QueryChanged(val value: String) : DrawerAction
    data class OpenApp(val app: InstalledApp) : DrawerAction
    data class AddHomeApp(val app: InstalledApp) : DrawerAction
    data class RemoveHomeApp(val app: InstalledApp) : DrawerAction
    data class UninstallApp(val app: InstalledApp) : DrawerAction
}
