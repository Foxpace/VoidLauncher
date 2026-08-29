package com.tomasrepcik.voidlauncher.shortcuts.picker

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot
import com.tomasrepcik.voidlauncher.launcher.root.HandleRootActions
import com.tomasrepcik.voidlauncher.launcher.navigation.LauncherNavigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun ShortcutPickerRoot(
    slot: ShortcutSlot,
    snackbarHostState: SnackbarHostState,
    navigator: LauncherNavigator,
    viewModel: ShortcutPickerViewModel = koinViewModel { parametersOf(slot) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HandleRootActions(
        actions = viewModel.rootActions,
        snackbarHostState = snackbarHostState,
        onCloseScreen = navigator::goBack,
    )
    ShortcutPickerScreen(
        slot = slot,
        state = state,
        actions = ShortcutPickerActions(
            onBack = { viewModel.onAction(ShortcutPickerAction.Back) },
            onQueryChange = { viewModel.onAction(ShortcutPickerAction.QueryChanged(it)) },
            onContactsSelected = { viewModel.onAction(ShortcutPickerAction.SelectContacts) },
            onCameraSelected = { viewModel.onAction(ShortcutPickerAction.SelectCamera) },
            onAppSelected = { viewModel.onAction(ShortcutPickerAction.SelectApp(it)) },
        ),
    )
}
