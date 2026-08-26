package com.tomasrepcik.voidlauncher.ui.customization

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceActions
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceAction
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceViewModel
import com.tomasrepcik.voidlauncher.ui.home.appearance.rememberSystemImageSelector
import com.tomasrepcik.voidlauncher.ui.navigation.HandleRootActions
import com.tomasrepcik.voidlauncher.ui.navigation.LauncherNavigator
import com.tomasrepcik.voidlauncher.ui.navigation.ScheduleListRoute
import com.tomasrepcik.voidlauncher.ui.navigation.ShortcutPickerRoute
import com.tomasrepcik.voidlauncher.ui.onboarding.NavigationTutorial
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CustomizationRoot(
    snackbarHostState: SnackbarHostState,
    navigator: LauncherNavigator,
    viewModel: CustomizationViewModel = koinViewModel(),
    appearanceViewModel: HomeAppearanceViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appearance by appearanceViewModel.state.collectAsStateWithLifecycle()
    var showNavigationTutorial by rememberSaveable { mutableStateOf(false) }
    val imageSelector = rememberSystemImageSelector { uri ->
        appearanceViewModel.onAction(HomeAppearanceAction.SelectBackground(uri))
    }
    val appearanceActions = remember(appearanceViewModel, imageSelector) {
        HomeAppearanceActions(
            onChooseBackground = imageSelector::chooseImage,
            onRestoreDefault = {
                appearanceViewModel.onAction(HomeAppearanceAction.RestoreDefaultBackground)
            },
            onUseBackgroundColorsChange = {
                appearanceViewModel.onAction(HomeAppearanceAction.SetUseBackgroundColors(it))
            },
        )
    }
    LaunchedEffect(viewModel, navigator) {
        viewModel.navigation.collect { destination ->
            when (destination) {
                CustomizationNavigationEvent.Back -> navigator.goBack()
                is CustomizationNavigationEvent.EditShortcut -> {
                    navigator.open(ShortcutPickerRoute(destination.slot))
                }
                CustomizationNavigationEvent.OpenSchedules -> navigator.open(ScheduleListRoute)
                CustomizationNavigationEvent.ShowNavigationTutorial -> {
                    showNavigationTutorial = true
                }
            }
        }
    }
    HandleRootActions(appearanceViewModel.rootActions, snackbarHostState)
    CustomizationScreen(
        state = state,
        appearance = appearance,
        appearanceActions = appearanceActions,
        actions = CustomizationActions(
            onBack = { viewModel.onAction(CustomizationAction.Back) },
            onEditShortcut = { viewModel.onAction(CustomizationAction.EditShortcut(it)) },
            onOpenSchedules = { viewModel.onAction(CustomizationAction.OpenSchedules) },
            onShowNavigationTutorial = {
                viewModel.onAction(CustomizationAction.ShowNavigationTutorial)
            },
        ),
    )
    if (showNavigationTutorial) {
        NavigationTutorial(onFinish = { showNavigationTutorial = false })
    }
}
