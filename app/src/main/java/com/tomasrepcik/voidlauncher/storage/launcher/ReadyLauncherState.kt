package com.tomasrepcik.voidlauncher.storage.launcher

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal fun LauncherRepository.readyLauncherState(): Flow<LauncherState?> = state.map { state ->
    (state as? LauncherRepositoryState.Ready)?.launcher
}
