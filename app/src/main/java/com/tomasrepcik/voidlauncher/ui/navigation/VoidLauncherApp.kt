package com.tomasrepcik.voidlauncher.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.tomasrepcik.voidlauncher.LauncherApplication
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.domain.search.SearchTarget
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationScreen
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationActions
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationViewModel
import com.tomasrepcik.voidlauncher.ui.customization.ShortcutPickerScreen
import com.tomasrepcik.voidlauncher.ui.customization.ShortcutPickerActions
import com.tomasrepcik.voidlauncher.ui.customization.ShortcutPickerViewModel
import com.tomasrepcik.voidlauncher.ui.drawer.AppDrawerScreen
import com.tomasrepcik.voidlauncher.ui.drawer.AppDrawerActions
import com.tomasrepcik.voidlauncher.ui.drawer.DrawerViewModel
import com.tomasrepcik.voidlauncher.ui.home.HomeScreen
import com.tomasrepcik.voidlauncher.ui.home.HomeActions
import com.tomasrepcik.voidlauncher.ui.home.HomeViewModel
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceActions
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceViewModel
import com.tomasrepcik.voidlauncher.ui.onboarding.NavigationTutorial
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleEditorScreen
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleEditorViewModel
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleListScreen
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleListViewModel
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object AppListRoute : NavKey

@Serializable
data object CustomizationRoute : NavKey

@Serializable
data class ShortcutPickerRoute(val slot: ShortcutSlot) : NavKey

@Serializable
data object ScheduleListRoute : NavKey

@Serializable
data class ScheduleEditorRoute(val scheduleId: String? = null) : NavKey

@Composable
fun VoidLauncherApp() {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as LauncherApplication).appContainer
    val appViewModel: LauncherAppViewModel = viewModel(
        factory = LauncherAppViewModel.provideFactory(appContainer.launcherRepository),
    )
    val repositoryState by appViewModel.repositoryState.collectAsStateWithLifecycle()
    val appearanceViewModel: HomeAppearanceViewModel = viewModel(
        factory = HomeAppearanceViewModel.provideFactory(
            context = context,
            repository = appContainer.launcherRepository,
        ),
    )
    val appearance by appearanceViewModel.state.collectAsStateWithLifecycle()
    val appearanceActions = remember(appearanceViewModel) {
        HomeAppearanceActions(
            onBackgroundSelected = appearanceViewModel::selectBackground,
            onRestoreDefault = appearanceViewModel::restoreDefault,
            onUseBackgroundColorsChange = appearanceViewModel::setUseBackgroundColors,
        )
    }
    val backStack = rememberNavBackStack(HomeRoute)
    val snackbarHostState = remember { SnackbarHostState() }
    var tutorialReplayRequested by rememberSaveable { mutableStateOf(false) }
    var tutorialDismissedThisSession by rememberSaveable { mutableStateOf(false) }
    val hasSeenNavigationTutorial = (repositoryState as? LauncherRepositoryState.Ready)
        ?.launcher
        ?.preferences
        ?.hasSeenNavigationTutorial
    val showNavigationTutorial = tutorialReplayRequested ||
        (hasSeenNavigationTutorial == false && !tutorialDismissedThisSession)

    LauncherEffectHost(
        effects = appViewModel.effects,
        snackbarHostState = snackbarHostState,
    )
    LauncherEffectHost(
        effects = appearanceViewModel.effects,
        snackbarHostState = snackbarHostState,
    )

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            onBack = backStack::popIfNotRoot,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<HomeRoute> {
                    val viewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.provideFactory(
                            repository = appContainer.launcherRepository,
                            installedAppSearch = appContainer.installedAppSearch,
                        )
                    )
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    LauncherEffectHost(
                        effects = viewModel.effects,
                        snackbarHostState = snackbarHostState,
                    )
                    HomeScreen(
                        state = state,
                        appearance = appearance,
                        actions = HomeActions(
                            onQueryChange = viewModel::onQueryChange,
                            onPrimarySearch = { viewModel.onSearch(SearchTarget.Primary) },
                            onBrowserSearch = { viewModel.onSearch(SearchTarget.Browser) },
                            onPlayStoreSearch = { viewModel.onSearch(SearchTarget.PlayStore) },
                            onMapsSearch = { viewModel.onSearch(SearchTarget.Maps) },
                            onAppHint = viewModel::onAppHint,
                            onAppClicked = viewModel::onAppClicked,
                            onShortcutClicked = viewModel::onShortcutClicked,
                            onOpenDrawer = { backStack.pushSingleTop(AppListRoute) },
                            onOpenSchedules = { backStack.pushSingleTop(ScheduleListRoute) },
                            onRemoveHomeApp = viewModel::removeHomeApp,
                            onRenameHomeApp = viewModel::renameHomeApp,
                            onUninstallApp = viewModel::uninstallApp,
                            onReorderHomeApps = viewModel::reorderHomeApps,
                        ),
                    )
                }

                entry<AppListRoute> {
                    val viewModel: DrawerViewModel = viewModel(
                        factory = DrawerViewModel.provideFactory(
                            appContainer.launcherRepository,
                            appContainer.installedAppSearch,
                        )
                    )
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    LauncherEffectHost(
                        effects = viewModel.effects,
                        snackbarHostState = snackbarHostState,
                    )
                    AppDrawerScreen(
                        state = state,
                        actions = AppDrawerActions(
                            onBack = backStack::popIfNotRoot,
                            onOpenSettings = { backStack.pushSingleTop(CustomizationRoute) },
                            onQueryChange = viewModel::onQueryChange,
                            onAppClicked = viewModel::onAppClicked,
                            onAddHomeApp = viewModel::addHomeApp,
                            onRemoveHomeApp = viewModel::removeHomeApp,
                            onUninstallApp = viewModel::uninstallApp,
                        ),
                    )
                }

                entry<CustomizationRoute> {
                    val viewModel: CustomizationViewModel = viewModel(
                        factory = CustomizationViewModel.provideFactory(appContainer.launcherRepository)
                    )
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    CustomizationScreen(
                        state = state,
                        appearance = appearance,
                        appearanceActions = appearanceActions,
                        actions = CustomizationActions(
                            onBack = backStack::popIfNotRoot,
                            onEditShortcut = { slot ->
                                backStack.pushSingleTop(ShortcutPickerRoute(slot))
                            },
                            onOpenSchedules = { backStack.pushSingleTop(ScheduleListRoute) },
                            onShowNavigationTutorial = { tutorialReplayRequested = true },
                        ),
                    )
                }

                entry<ScheduleListRoute> {
                    val viewModel: ScheduleListViewModel = viewModel(
                        factory = ScheduleListViewModel.provideFactory(appContainer.launcherRepository)
                    )
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    LauncherEffectHost(
                        effects = viewModel.effects,
                        snackbarHostState = snackbarHostState,
                    )
                    ScheduleListScreen(
                        state = state,
                        onBack = backStack::popIfNotRoot,
                        onAdd = { backStack.pushSingleTop(ScheduleEditorRoute()) },
                        onEdit = { id -> backStack.pushSingleTop(ScheduleEditorRoute(id)) },
                        onIntent = viewModel::onIntent,
                    )
                }

                entry<ScheduleEditorRoute> { route ->
                    val viewModel: ScheduleEditorViewModel = viewModel(
                        factory = ScheduleEditorViewModel.provideFactory(
                            appContainer.launcherRepository,
                            route.scheduleId,
                            appContainer.installedAppSearch,
                        )
                    )
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    LauncherEffectHost(
                        effects = viewModel.effects,
                        snackbarHostState = snackbarHostState,
                        onCompleted = backStack::popIfNotRoot,
                    )
                    ScheduleEditorScreen(
                        state = state,
                        onBack = backStack::popIfNotRoot,
                        onIntent = viewModel::onIntent,
                    )
                }

                entry<ShortcutPickerRoute> { route ->
                    val viewModel: ShortcutPickerViewModel = viewModel(
                        factory = ShortcutPickerViewModel.provideFactory(
                            repository = appContainer.launcherRepository,
                            slot = route.slot,
                            installedAppSearch = appContainer.installedAppSearch,
                        )
                    )
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    LauncherEffectHost(
                        effects = viewModel.effects,
                        snackbarHostState = snackbarHostState,
                        onCompleted = backStack::popIfNotRoot,
                    )
                    ShortcutPickerScreen(
                        slot = route.slot,
                        state = state,
                        actions = ShortcutPickerActions(
                            onBack = backStack::popIfNotRoot,
                            onQueryChange = viewModel::onQueryChange,
                            onContactsSelected = viewModel::onContactsSelected,
                            onCameraSelected = viewModel::onCameraSelected,
                            onAppSelected = viewModel::onAppSelected,
                        ),
                    )
                }
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )

        LauncherRepositoryBlocker(
            state = repositoryState,
            onRetry = appViewModel::retryInitialization,
        )
    }

    if (showNavigationTutorial) {
        NavigationTutorial(
            onFinish = {
                tutorialReplayRequested = false
                tutorialDismissedThisSession = true
                if (hasSeenNavigationTutorial == false) {
                    appViewModel.finishNavigationTutorial()
                }
            },
        )
    }
}
