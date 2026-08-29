package com.tomasrepcik.voidlauncher.schedule.list

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.voidlauncher.launcher.navigation.HandleRootActions
import com.tomasrepcik.voidlauncher.launcher.navigation.LauncherNavigator
import com.tomasrepcik.voidlauncher.launcher.navigation.ScheduleEditorRoute
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun ScheduleListRoot(
    snackbarHostState: SnackbarHostState,
    navigator: LauncherNavigator,
    viewModel: ScheduleListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HandleRootActions(viewModel.rootActions, snackbarHostState)
    LaunchedEffect(viewModel, navigator) {
        viewModel.navigation.collect { destination ->
            when (destination) {
                ScheduleListNavigationEvent.Back -> navigator.goBack()
                ScheduleListNavigationEvent.Add -> navigator.open(ScheduleEditorRoute())
                is ScheduleListNavigationEvent.Edit -> {
                    navigator.open(ScheduleEditorRoute(destination.id))
                }
            }
        }
    }
    ScheduleListScreen(
        state = state,
        onBack = { viewModel.onAction(ScheduleListAction.Back) },
        onAdd = { viewModel.onAction(ScheduleListAction.AddSchedule) },
        onEdit = { id -> viewModel.onAction(ScheduleListAction.EditSchedule(id)) },
        onAction = viewModel::onAction,
    )
}
