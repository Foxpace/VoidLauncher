package com.tomasrepcik.voidlauncher.appcatalog.action

import com.tomasrepcik.voidlauncher.home.data.HomeAppsRepository
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.LauncherRootAction
import com.tomasrepcik.voidlauncher.launcher.action.LauncherAction
import com.tomasrepcik.voidlauncher.storage.launcher.RepositoryWriteResult

internal sealed interface AppSelectionAction {
    data class Open(val app: InstalledApp) : AppSelectionAction
    data class AddToHome(val app: InstalledApp) : AppSelectionAction
    data class RemoveFromHome(val app: InstalledApp) : AppSelectionAction
    data class Uninstall(val app: InstalledApp) : AppSelectionAction
}

class HandleAppSelection internal constructor(
    private val homeApps: HomeAppsRepository,
) {
    internal suspend operator fun invoke(
        action: AppSelectionAction,
    ): LauncherRootAction? = when (action) {
        is AppSelectionAction.Open -> LauncherRootAction.Open(
            LauncherAction.LaunchInstalledApp(action.app),
        )
        is AppSelectionAction.AddToHome -> homeApps.add(action.app.key).toRootAction(
            addedAppLabel = action.app.label,
        )
        is AppSelectionAction.RemoveFromHome -> homeApps.remove(action.app.key).toRootAction()
        is AppSelectionAction.Uninstall -> LauncherRootAction.Open(
            LauncherAction.UninstallApp(action.app),
        )
    }
}

private fun RepositoryWriteResult.toRootAction(
    addedAppLabel: String? = null,
): LauncherRootAction? = when (this) {
    RepositoryWriteResult.Completed -> addedAppLabel?.let {
        LauncherRootAction.ShowAppAddedConfirmation(it)
    }
    is RepositoryWriteResult.Failed -> LauncherRootAction.ShowError(error)
}
