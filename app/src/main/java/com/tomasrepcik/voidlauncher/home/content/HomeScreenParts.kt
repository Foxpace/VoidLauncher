package com.tomasrepcik.voidlauncher.home.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.design.components.AppIcon
import com.tomasrepcik.voidlauncher.home.HomeAppRowActions
import com.tomasrepcik.voidlauncher.home.HomeAppRowState
import com.tomasrepcik.voidlauncher.home.SearchOverlayActions
import com.tomasrepcik.voidlauncher.home.SearchOverlayState

@Composable
internal fun SearchOverlayResults(
    state: SearchOverlayState,
    actions: SearchOverlayActions,
) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )

    when {
        state.suggestions.isNotEmpty() -> state.suggestions.forEach { app ->
            key(app.key) {
                SearchSuggestionRow(
                    app = app,
                    onClick = { actions.onSuggestionClicked(app) },
                )
            }
        }
        state.isLoading -> LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
        else -> Text(
            text = stringResource(R.string.no_matching_apps),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SearchSuggestionRow(
    app: InstalledApp,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(
            app = app,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun HomeAppActionButtons(
    appLabel: String,
    state: HomeAppRowState,
    actions: HomeAppRowActions,
) {
    AnimatedVisibility(visible = state.showActions) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = actions.onRemove,
                modifier = Modifier.testTag("home_app_remove_$appLabel"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.remove_from_home),
                )
            }
            Box {
                IconButton(
                    onClick = { actions.onToggleMenu(true) },
                    modifier = Modifier.testTag("home_app_more_$appLabel"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                    )
                }
                DropdownMenu(
                    expanded = state.showMenu,
                    onDismissRequest = { actions.onToggleMenu(false) },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        onClick = {
                            actions.onToggleMenu(false)
                            actions.onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.uninstall)) },
                        onClick = {
                            actions.onToggleMenu(false)
                            actions.onUninstall()
                        },
                    )
                }
            }
        }
    }
}
