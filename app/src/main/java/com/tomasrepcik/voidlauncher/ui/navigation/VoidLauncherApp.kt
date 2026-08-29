package com.tomasrepcik.voidlauncher.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceViewModel
import com.tomasrepcik.voidlauncher.ui.onboarding.NavigationTutorial
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun VoidLauncherApp(
    appViewModel: LauncherAppViewModel = koinViewModel(),
    appearanceViewModel: HomeAppearanceViewModel = koinViewModel(),
    messages: AndroidLauncherRootActionMessages = koinInject(),
) {
    val state by appViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var tutorialDismissedThisSession by rememberSaveable { mutableStateOf(false) }
    val showNavigationTutorial = state.hasSeenNavigationTutorial == false &&
        !tutorialDismissedThisSession

    HandleRootActions(appViewModel.rootActions, snackbarHostState)
    HandleRootActions(appearanceViewModel.rootActions, snackbarHostState)

    Box(modifier = Modifier.fillMaxSize()) {
        LauncherNavigation(
            snackbarHostState = snackbarHostState,
            appearanceViewModel = appearanceViewModel,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )

        LauncherStartupBlocker(
            state = state,
            errorMessage = state.initializationError?.let(messages::errorMessage),
            onRetry = { appViewModel.onAction(LauncherAppAction.RetryInitialization) },
        )
    }

    if (showNavigationTutorial) {
        NavigationTutorial(
            onFinish = {
                tutorialDismissedThisSession = true
                appViewModel.onAction(LauncherAppAction.MarkNavigationTutorialSeen)
            },
        )
    }
}
