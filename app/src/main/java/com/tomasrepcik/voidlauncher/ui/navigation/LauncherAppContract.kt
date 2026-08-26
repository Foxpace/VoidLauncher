package com.tomasrepcik.voidlauncher.ui.navigation

import com.tomasrepcik.voidlauncher.domain.error.AppError

internal data class LauncherAppUiState(
    val isLoading: Boolean = true,
    val initializationError: AppError? = null,
    val hasSeenNavigationTutorial: Boolean? = null,
)

internal sealed interface LauncherAppAction {
    data object MarkNavigationTutorialSeen : LauncherAppAction
    data object RetryInitialization : LauncherAppAction
}
