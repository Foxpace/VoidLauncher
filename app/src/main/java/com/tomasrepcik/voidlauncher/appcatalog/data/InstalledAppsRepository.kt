package com.tomasrepcik.voidlauncher.appcatalog.data

import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherRepository
import com.tomasrepcik.voidlauncher.storage.launcher.readyLauncherState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The feature-facing source of the installed app catalogue. */
class InstalledAppsRepository internal constructor(
    launcher: LauncherRepository,
) {
    val apps: Flow<List<InstalledApp>?> = launcher.readyLauncherState()
        .map { state -> state?.installedApps }
        .distinctUntilChanged()
}
