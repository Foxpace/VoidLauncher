package com.tomasrepcik.voidlauncher.launcher.root

import com.tomasrepcik.voidlauncher.launcher.error.AppError

internal data class LauncherAppUiState(
    val isLoading: Boolean = true,
    val initializationError: AppError? = null,
    val hasSeenNavigationTutorial: Boolean? = null,
)

internal sealed interface LauncherAppAction {
    data object MarkNavigationTutorialSeen : LauncherAppAction
    data object RetryInitialization : LauncherAppAction
}
