package com.tomasrepcik.voidlauncher.domain.action

import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.domain.error.AppError
import com.tomasrepcik.voidlauncher.domain.error.ErrorRecovery

sealed interface LauncherAction {
    data class LaunchInstalledApp(val app: InstalledApp) : LauncherAction
    data class OpenWebSearch(val query: String) : LauncherAction
    data class OpenPlayStoreSearch(val query: String) : LauncherAction
    data class OpenMapsSearch(val query: String) : LauncherAction
    data class OpenShortcut(val shortcut: ResolvedShortcut) : LauncherAction
    data class UninstallApp(val app: InstalledApp) : LauncherAction
}

sealed interface LauncherActionOutcome {
    data object Completed : LauncherActionOutcome

    data class Recovered(
        val recovery: ErrorRecovery,
    ) : LauncherActionOutcome

    data class Failed(
        val error: AppError,
    ) : LauncherActionOutcome
}
