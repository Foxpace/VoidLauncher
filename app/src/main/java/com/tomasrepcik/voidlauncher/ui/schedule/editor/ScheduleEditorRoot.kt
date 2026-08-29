package com.tomasrepcik.voidlauncher.ui.schedule.editor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.voidlauncher.ui.navigation.HandleRootActions
import com.tomasrepcik.voidlauncher.ui.navigation.LauncherNavigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun ScheduleEditorRoot(
    scheduleId: String?,
    snackbarHostState: SnackbarHostState,
    navigator: LauncherNavigator,
    viewModel: ScheduleEditorViewModel = koinViewModel {
        parametersOf(ScheduleEditorArgs(scheduleId))
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HandleRootActions(
        actions = viewModel.rootActions,
        snackbarHostState = snackbarHostState,
        onCloseScreen = navigator::goBack,
    )
    ScheduleEditorScreen(
        state = state,
        onBack = { viewModel.onAction(ScheduleEditorAction.Back) },
        onAction = viewModel::onAction,
    )
}
