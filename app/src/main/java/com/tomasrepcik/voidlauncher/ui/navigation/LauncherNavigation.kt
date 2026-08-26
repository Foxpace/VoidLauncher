package com.tomasrepcik.voidlauncher.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationRoot
import com.tomasrepcik.voidlauncher.ui.customization.shortcutpicker.ShortcutPickerRoot
import com.tomasrepcik.voidlauncher.ui.drawer.DrawerRoot
import com.tomasrepcik.voidlauncher.ui.home.HomeRoot
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceViewModel
import com.tomasrepcik.voidlauncher.ui.schedule.editor.ScheduleEditorRoot
import com.tomasrepcik.voidlauncher.ui.schedule.list.ScheduleListRoot

private const val NAVIGATION_TRANSITION_MILLIS = 220
private const val APP_DRAWER_TRANSITION_MILLIS = 380
private const val APP_DRAWER_FADE_MILLIS = 240

@Composable
internal fun LauncherNavigation(
    snackbarHostState: SnackbarHostState,
    appearanceViewModel: HomeAppearanceViewModel,
) {
    val backStack = rememberNavBackStack(HomeRoute)
    val navigator = LauncherNavigator(backStack)

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        onBack = backStack::popIfNotRoot,
        transitionSpec = { launcherForwardTransition() },
        popTransitionSpec = { launcherBackTransition() },
        predictivePopTransitionSpec = { launcherBackTransition() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeRoute> {
                HomeRoot(
                    snackbarHostState = snackbarHostState,
                    navigator = navigator,
                    appearanceViewModel = appearanceViewModel,
                )
            }
            entry<AppListRoute>(metadata = appDrawerTransitionMetadata()) {
                DrawerRoot(
                    snackbarHostState = snackbarHostState,
                    navigator = navigator,
                )
            }
            entry<CustomizationRoute> {
                CustomizationRoot(
                    navigator = navigator,
                    appearanceViewModel = appearanceViewModel,
                )
            }
            entry<ScheduleListRoute> {
                ScheduleListRoot(
                    snackbarHostState = snackbarHostState,
                    navigator = navigator,
                )
            }
            entry<ScheduleEditorRoute> { route ->
                ScheduleEditorRoot(
                    scheduleId = route.scheduleId,
                    snackbarHostState = snackbarHostState,
                    navigator = navigator,
                )
            }
            entry<ShortcutPickerRoute> { route ->
                ShortcutPickerRoot(
                    slot = route.slot,
                    snackbarHostState = snackbarHostState,
                    navigator = navigator,
                )
            }
        },
    )

    ConsumeBackAtRoot(isAtRoot = navigator.isAtHome)
}

@Composable
internal fun ConsumeBackAtRoot(isAtRoot: Boolean) {
    BackHandler(enabled = isAtRoot) {}
}

private fun launcherForwardTransition(): ContentTransform =
    slideInHorizontally(
        initialOffsetX = { width -> width },
        animationSpec = tween(NAVIGATION_TRANSITION_MILLIS),
    ) togetherWith slideOutHorizontally(
        targetOffsetX = { width -> -width / 4 },
        animationSpec = tween(NAVIGATION_TRANSITION_MILLIS),
    )

private fun launcherBackTransition(): ContentTransform =
    slideInHorizontally(
        initialOffsetX = { width -> -width / 4 },
        animationSpec = tween(NAVIGATION_TRANSITION_MILLIS),
    ) togetherWith slideOutHorizontally(
        targetOffsetX = { width -> width },
        animationSpec = tween(NAVIGATION_TRANSITION_MILLIS),
    )

private fun appDrawerTransitionMetadata(): Map<String, Any> =
    NavDisplay.transitionSpec { appDrawerForwardTransition() } +
        NavDisplay.popTransitionSpec { appDrawerBackTransition() } +
        NavDisplay.predictivePopTransitionSpec { _ -> appDrawerBackTransition() }

private fun appDrawerForwardTransition(): ContentTransform =
    (slideInHorizontally(
        initialOffsetX = { width -> width / 3 },
        animationSpec = appDrawerMotion(),
    ) + fadeIn(animationSpec = tween(APP_DRAWER_FADE_MILLIS))) togetherWith
        (slideOutHorizontally(
            targetOffsetX = { width -> -width / 10 },
            animationSpec = appDrawerMotion(),
        ) + fadeOut(animationSpec = tween(APP_DRAWER_FADE_MILLIS)))

private fun appDrawerBackTransition(): ContentTransform =
    (slideInHorizontally(
        initialOffsetX = { width -> -width / 10 },
        animationSpec = appDrawerMotion(),
    ) + fadeIn(animationSpec = tween(APP_DRAWER_FADE_MILLIS))) togetherWith
        (slideOutHorizontally(
            targetOffsetX = { width -> width / 3 },
            animationSpec = appDrawerMotion(),
        ) + fadeOut(animationSpec = tween(APP_DRAWER_FADE_MILLIS)))

private fun appDrawerMotion() = tween<IntOffset>(
    durationMillis = APP_DRAWER_TRANSITION_MILLIS,
    easing = FastOutSlowInEasing,
)
