package com.tomasrepcik.voidlauncher.storage.launcher

import kotlinx.coroutines.flow.StateFlow

/** App-wide startup status. Feature view models do not depend on this repository. */
class LauncherStatusRepository internal constructor(
    private val launcher: LauncherRepository,
) {
    val state: StateFlow<LauncherRepositoryState> = launcher.state

    fun retryInitialization() = launcher.retryInitialization()
}
