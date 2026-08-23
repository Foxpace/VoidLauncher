package com.tomasrepcik.voidlauncher.ui.home.appearance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R

@Composable
internal fun HomeAppearanceSettings(
    state: HomeAppearanceState,
    actions: HomeAppearanceActions,
) {
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.toString()?.let(actions.onBackgroundSelected)
    }
    val hasBackground = state.backgroundUri != null

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(imageVector = Icons.Outlined.Wallpaper, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_background),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.home_background_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            BackgroundActions(
                hasBackground = hasBackground,
                onPickBackground = { imagePicker.launch(arrayOf("image/*")) },
                onRestoreDefault = actions.onRestoreDefault,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            BackgroundColorOption(
                hasBackground = hasBackground,
                useBackgroundColors = state.useBackgroundColors,
                onUseBackgroundColorsChange = actions.onUseBackgroundColorsChange,
            )
        }
    }
}

@Composable
private fun BackgroundActions(
    hasBackground: Boolean,
    onPickBackground: () -> Unit,
    onRestoreDefault: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onPickBackground,
            modifier = Modifier.testTag("pick_home_background_button"),
        ) {
            Text(
                stringResource(
                    if (hasBackground) R.string.change_background else R.string.choose_background
                )
            )
        }
        if (hasBackground) {
            TextButton(
                onClick = onRestoreDefault,
                modifier = Modifier.testTag("restore_default_background_button"),
            ) {
                Text(stringResource(R.string.restore_default_background))
            }
        }
    }
}

@Composable
private fun BackgroundColorOption(
    hasBackground: Boolean,
    useBackgroundColors: Boolean,
    onUseBackgroundColorsChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.use_background_colors),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    if (hasBackground) {
                        R.string.use_background_colors_summary
                    } else {
                        R.string.use_background_colors_unavailable
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Switch(
            checked = useBackgroundColors,
            onCheckedChange = onUseBackgroundColorsChange,
            enabled = hasBackground,
            modifier = Modifier.testTag("use_background_colors_switch"),
        )
    }
}
