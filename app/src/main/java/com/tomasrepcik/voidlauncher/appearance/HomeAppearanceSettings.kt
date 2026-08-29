package com.tomasrepcik.voidlauncher.appearance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.design.components.LauncherSwitch

@Composable
internal fun HomeAppearanceSettings(
    state: HomeAppearanceState,
    actions: HomeAppearanceActions,
) {
    val hasBackground = state.backgroundUri != null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackgroundImageOption(
            hasBackground = hasBackground,
            onPickBackground = actions.onChooseBackground,
            onRestoreDefault = actions.onRestoreDefault,
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            BackgroundColorOption(
                hasBackground = hasBackground,
                useBackgroundColors = state.useBackgroundColors,
                onUseBackgroundColorsChange = actions.onUseBackgroundColorsChange,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun BackgroundImageOption(
    hasBackground: Boolean,
    onPickBackground: () -> Unit,
    onRestoreDefault: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPickBackground)
                .testTag("pick_home_background_button")
                .padding(16.dp),
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
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
            )
        }
        if (hasBackground) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            TextButton(
                onClick = onRestoreDefault,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("restore_default_background_button"),
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
        LauncherSwitch(
            checked = useBackgroundColors,
            onCheckedChange = onUseBackgroundColorsChange,
            enabled = hasBackground,
            modifier = Modifier.testTag("use_background_colors_switch"),
        )
    }
}
