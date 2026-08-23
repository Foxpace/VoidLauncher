package com.tomasrepcik.voidlauncher.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferencesMutation
import com.tomasrepcik.voidlauncher.data.repository.RepositoryMutationOutcome
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.domain.error.AppErrorMessageMapper
import com.tomasrepcik.voidlauncher.domain.search.SearchTarget
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationScreen
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
import com.tomasrepcik.voidlauncher.ui.onboarding.NavigationTutorial
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleEditorScreen
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleEditorViewModel
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleEffect
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleListScreen
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
    val repositoryState by appContainer.launcherRepository.state.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(HomeRoute)
    val snackbarHostState = remember { SnackbarHostState() }
    val mutationScope = rememberCoroutineScope()
    val errorMessageMapper = remember { AppErrorMessageMapper() }
    var tutorialReplayRequested by rememberSaveable { mutableStateOf(false) }
    var tutorialDismissedThisSession by rememberSaveable { mutableStateOf(false) }
    val hasSeenNavigationTutorial = (repositoryState as? LauncherRepositoryState.Ready)
        ?.launcher
        ?.preferences
        ?.hasSeenNavigationTutorial
    val showNavigationTutorial = tutorialReplayRequested ||
        (hasSeenNavigationTutorial == false && !tutorialDismissedThisSession)

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
                    CollectLauncherActions(
                        actions = viewModel.actions,
                        feedback = viewModel.feedback,
                        snackbarHostState = snackbarHostState,
                    )
                    HomeScreen(
                        state = state,
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
                            onRemoveHomeApp = { app ->
                                mutationScope.launchMutation(
                                    context,
                                    snackbarHostState,
                                    errorMessageMapper,
                                    mutation = { viewModel.removeHomeApp(app) },
                                )
                            },
                            onRenameHomeApp = { app, label ->
                                mutationScope.launchMutation(
                                    context,
                                    snackbarHostState,
                                    errorMessageMapper,
                                    mutation = { viewModel.renameHomeApp(app, label) },
                                )
                            },
                            onUninstallApp = viewModel::uninstallApp,
                            onReorderHomeApps = { fromIndex, toIndex ->
                                mutationScope.launchMutation(
                                    context,
                                    snackbarHostState,
                                    errorMessageMapper,
                                    mutation = {
                                        viewModel.reorderHomeApps(fromIndex, toIndex)
                                    },
                                )
                            },
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
                    CollectLauncherActions(
                        actions = viewModel.actions,
                        snackbarHostState = snackbarHostState,
                    )
                    AppDrawerScreen(
                        state = state,
                        actions = AppDrawerActions(
                            onBack = backStack::popIfNotRoot,
                            onOpenSettings = { backStack.pushSingleTop(CustomizationRoute) },
                            onQueryChange = viewModel::onQueryChange,
                            onAppClicked = viewModel::onAppClicked,
                            onAddHomeApp = { app ->
                                mutationScope.launchMutation(
                                    context,
                                    snackbarHostState,
                                    errorMessageMapper,
                                    mutation = { viewModel.addHomeApp(app) },
                                )
                            },
                            onRemoveHomeApp = { app ->
                                mutationScope.launchMutation(
                                    context,
                                    snackbarHostState,
                                    errorMessageMapper,
                                    mutation = { viewModel.removeHomeApp(app) },
                                )
                            },
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
                        onBack = backStack::popIfNotRoot,
                        onEditShortcut = { slot -> backStack.pushSingleTop(ShortcutPickerRoute(slot)) },
                        onOpenSchedules = { backStack.pushSingleTop(ScheduleListRoute) },
                        onShowNavigationTutorial = { tutorialReplayRequested = true },
                    )
                }

                entry<ScheduleListRoute> {
                    val viewModel: ScheduleListViewModel = viewModel(
                        factory = ScheduleListViewModel.provideFactory(appContainer.launcherRepository)
                    )
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    CollectScheduleEffects(
                        effects = viewModel.effects,
                        context = context,
                        snackbarHostState = snackbarHostState,
                        messageMapper = errorMessageMapper,
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
                    CollectScheduleEffects(
                        effects = viewModel.effects,
                        context = context,
                        snackbarHostState = snackbarHostState,
                        messageMapper = errorMessageMapper,
                        onSaved = backStack::popIfNotRoot,
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
                    ShortcutPickerScreen(
                        slot = route.slot,
                        state = state,
                        actions = ShortcutPickerActions(
                            onBack = backStack::popIfNotRoot,
                            onQueryChange = viewModel::onQueryChange,
                            onContactsSelected = {
                                mutationScope.launchMutation(
                                    context = context,
                                    snackbarHostState = snackbarHostState,
                                    messageMapper = errorMessageMapper,
                                    mutation = viewModel::onContactsSelected,
                                    onCompleted = backStack::popIfNotRoot,
                                )
                            },
                            onCameraSelected = {
                                mutationScope.launchMutation(
                                    context = context,
                                    snackbarHostState = snackbarHostState,
                                    messageMapper = errorMessageMapper,
                                    mutation = viewModel::onCameraSelected,
                                    onCompleted = backStack::popIfNotRoot,
                                )
                            },
                            onAppSelected = { app ->
                                mutationScope.launchMutation(
                                    context = context,
                                    snackbarHostState = snackbarHostState,
                                    messageMapper = errorMessageMapper,
                                    mutation = { viewModel.onAppSelected(app) },
                                    onCompleted = backStack::popIfNotRoot,
                                )
                            },
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
            onRetry = appContainer.launcherRepository::retryInitialization,
        )
    }

    if (showNavigationTutorial) {
        NavigationTutorial(
            onFinish = {
                tutorialReplayRequested = false
                tutorialDismissedThisSession = true
                if (hasSeenNavigationTutorial == false) {
                    mutationScope.launchMutation(
                        context = context,
                        snackbarHostState = snackbarHostState,
                        messageMapper = errorMessageMapper,
                        mutation = {
                            appContainer.launcherRepository.mutatePreferences(
                                LauncherPreferencesMutation.MarkNavigationTutorialSeen
                            )
                        },
                    )
                }
            },
        )
    }
}

@Composable
private fun CollectScheduleEffects(
    effects: Flow<ScheduleEffect>,
    context: Context,
    snackbarHostState: SnackbarHostState,
    messageMapper: AppErrorMessageMapper,
    onSaved: () -> Unit = {},
) {
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                ScheduleEffect.Saved -> onSaved()
                is ScheduleEffect.Failed -> snackbarHostState.showSnackbar(
                    messageMapper.message(context, effect.error),
                )
            }
        }
    }
}

private fun CoroutineScope.launchMutation(
    context: Context,
    snackbarHostState: SnackbarHostState,
    messageMapper: AppErrorMessageMapper,
    mutation: suspend () -> RepositoryMutationOutcome,
    onCompleted: () -> Unit = {},
) {
    launch {
        when (val outcome = mutation()) {
            RepositoryMutationOutcome.Completed -> onCompleted()
            is RepositoryMutationOutcome.Failed -> snackbarHostState.showSnackbar(
                messageMapper.message(context, outcome.error),
            )
        }
    }
}
