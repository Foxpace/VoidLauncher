package com.tomasrepcik.voidlauncher.launcher.action

import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.ResolvedShortcut
import com.tomasrepcik.voidlauncher.launcher.error.AppError
import com.tomasrepcik.voidlauncher.launcher.error.ErrorRecovery

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
