package com.tomasrepcik.voidlauncher.design.components

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tomasrepcik.voidlauncher.design.theme.voidLauncherSwitchColors

@Composable
internal fun LauncherSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = voidLauncherSwitchColors(),
    )
}
