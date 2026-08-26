package com.tomasrepcik.voidlauncher.ui.customization.shortcutpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationActions
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationContainer
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationHeader
import com.tomasrepcik.voidlauncher.ui.customization.displayName

@Composable
fun ShortcutPickerScreen(
    slot: ShortcutSlot,
    state: ShortcutPickerUiState,
    actions: ShortcutPickerActions,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        SwipeNavigationContainer(
            modifier = Modifier.fillMaxSize(),
            actions = SwipeNavigationActions(onClose = actions.onBack),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CustomizationHeader(
                    title = stringResource(R.string.pick_shortcut_title, slot.displayName()),
                    onBack = actions.onBack,
                )
                ShortcutPickerContent(
                    state = state,
                    actions = actions,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
