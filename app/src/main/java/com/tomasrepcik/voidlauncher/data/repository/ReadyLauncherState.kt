package com.tomasrepcik.voidlauncher.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal fun LauncherRepository.readyLauncherState(): Flow<LauncherState?> = state.map { state ->
    (state as? LauncherRepositoryState.Ready)?.launcher
}
