package com.tomasrepcik.voidlauncher.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.voidlauncher.domain.search.SearchTarget
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceViewModel
import com.tomasrepcik.voidlauncher.ui.navigation.HandleRootActions
import com.tomasrepcik.voidlauncher.ui.navigation.AppListRoute
import com.tomasrepcik.voidlauncher.ui.navigation.LauncherNavigator
import com.tomasrepcik.voidlauncher.ui.navigation.ScheduleListRoute
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun HomeRoot(
    snackbarHostState: SnackbarHostState,
    navigator: LauncherNavigator,
    appearanceViewModel: HomeAppearanceViewModel,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appearance by appearanceViewModel.state.collectAsStateWithLifecycle()
    HandleRootActions(viewModel.rootActions, snackbarHostState)
    LaunchedEffect(viewModel, navigator) {
        viewModel.navigation.collect { destination ->
            when (destination) {
                HomeNavigationEvent.OpenDrawer -> navigator.open(AppListRoute)
                HomeNavigationEvent.OpenSchedules -> navigator.open(ScheduleListRoute)
            }
        }
    }
    HomeScreen(
        state = state,
        appearance = appearance,
        actions = HomeActions(
            onQueryChange = { viewModel.onAction(HomeAction.QueryChanged(it)) },
            onPrimarySearch = { viewModel.onAction(HomeAction.Search(SearchTarget.BestMatch)) },
            onBrowserSearch = { viewModel.onAction(HomeAction.Search(SearchTarget.Browser)) },
            onPlayStoreSearch = { viewModel.onAction(HomeAction.Search(SearchTarget.PlayStore)) },
            onMapsSearch = { viewModel.onAction(HomeAction.Search(SearchTarget.Maps)) },
            onAppClicked = { viewModel.onAction(HomeAction.OpenApp(it)) },
            onShortcutClicked = { viewModel.onAction(HomeAction.OpenShortcut(it)) },
            onOpenDrawer = { viewModel.onAction(HomeAction.OpenDrawer) },
            onOpenSchedules = { viewModel.onAction(HomeAction.OpenSchedules) },
            onRemoveHomeApp = { viewModel.onAction(HomeAction.RemoveApp(it)) },
            onRenameHomeApp = { app, label ->
                viewModel.onAction(HomeAction.RenameApp(app, label))
            },
            onUninstallApp = { viewModel.onAction(HomeAction.UninstallApp(it)) },
            onReorderHomeApps = { from, to ->
                viewModel.onAction(HomeAction.ReorderApps(from, to))
            },
        ),
    )
}
