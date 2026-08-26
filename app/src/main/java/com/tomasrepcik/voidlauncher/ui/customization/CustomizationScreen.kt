package com.tomasrepcik.voidlauncher.ui.customization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationActions
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationContainer
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceActions
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceState

@Composable
fun CustomizationScreen(
    state: CustomizationUiState,
    appearance: HomeAppearanceState,
    appearanceActions: HomeAppearanceActions,
    actions: CustomizationActions,
) {
    var showOpenSourceLicenses by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        SwipeNavigationContainer(
            modifier = Modifier.fillMaxSize(),
            actions = SwipeNavigationActions(onClose = actions.onBack),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CustomizationHeader(
                    title = stringResource(R.string.customize_launcher),
                    onBack = actions.onBack,
                )
                CustomizationSettings(
                    state = state,
                    appearance = appearance,
                    appearanceActions = appearanceActions,
                    actions = actions,
                    onOpenLicenses = { showOpenSourceLicenses = true },
                )
            }
        }
    }

    if (showOpenSourceLicenses) {
        OpenSourceLicensesDialog(
            onDismiss = { showOpenSourceLicenses = false },
        )
    }
}

data class CustomizationActions(
    val onBack: () -> Unit,
    val onEditShortcut: (ShortcutSlot) -> Unit,
    val onOpenSchedules: () -> Unit,
    val onShowNavigationTutorial: () -> Unit,
)
