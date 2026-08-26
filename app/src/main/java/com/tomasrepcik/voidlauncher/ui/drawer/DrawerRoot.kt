package com.tomasrepcik.voidlauncher.ui.drawer

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.voidlauncher.ui.navigation.HandleRootActions
import com.tomasrepcik.voidlauncher.ui.navigation.CustomizationRoute
import com.tomasrepcik.voidlauncher.ui.navigation.LauncherNavigator
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DrawerRoot(
    snackbarHostState: SnackbarHostState,
    navigator: LauncherNavigator,
    viewModel: DrawerViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HandleRootActions(viewModel.rootActions, snackbarHostState)
    LaunchedEffect(viewModel, navigator) {
        viewModel.navigation.collect { destination ->
            when (destination) {
                DrawerNavigationEvent.Back -> navigator.goBack()
                DrawerNavigationEvent.OpenCustomization -> navigator.open(CustomizationRoute)
            }
        }
    }
    AppDrawerScreen(
        state = state,
        actions = AppDrawerActions(
            onBack = { viewModel.onAction(DrawerAction.Back) },
            onOpenSettings = { viewModel.onAction(DrawerAction.OpenCustomization) },
            onQueryChange = { viewModel.onAction(DrawerAction.QueryChanged(it)) },
            onAppClicked = { viewModel.onAction(DrawerAction.OpenApp(it)) },
            onAddHomeApp = { viewModel.onAction(DrawerAction.AddHomeApp(it)) },
            onRemoveHomeApp = { viewModel.onAction(DrawerAction.RemoveHomeApp(it)) },
            onUninstallApp = { viewModel.onAction(DrawerAction.UninstallApp(it)) },
        ),
    )
}
