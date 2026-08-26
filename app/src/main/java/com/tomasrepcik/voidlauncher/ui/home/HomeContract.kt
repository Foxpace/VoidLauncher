package com.tomasrepcik.voidlauncher.ui.home

import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.repository.HomeAppsData
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import com.tomasrepcik.voidlauncher.domain.search.SearchTarget

data class HomeUiState(
    val query: String = "",
    val homeApps: List<InstalledApp> = emptyList(),
    val shortcuts: List<ResolvedShortcut> = emptyList(),
    val searchSuggestions: List<InstalledApp> = emptyList(),
    val isScheduleActive: Boolean = false,
    val isLoading: Boolean = true,
)

sealed interface HomeAction {
    data object OpenDrawer : HomeAction
    data object OpenSchedules : HomeAction
    data class QueryChanged(val value: String) : HomeAction
    data class Search(val target: SearchTarget) : HomeAction
    data class OpenApp(val app: InstalledApp) : HomeAction
    data class OpenShortcut(val shortcut: ResolvedShortcut) : HomeAction
    data class RemoveApp(val app: InstalledApp) : HomeAction
    data class RenameApp(val app: InstalledApp, val label: String?) : HomeAction
    data class ReorderApps(val fromIndex: Int, val toIndex: Int) : HomeAction
    data class UninstallApp(val app: InstalledApp) : HomeAction
}

internal data class HomeRepositoryData(
    val installedApps: List<InstalledApp>?,
    val homeApps: HomeAppsData?,
    val shortcuts: List<ResolvedShortcut>?,
    val schedules: List<AppSchedule>?,
)
