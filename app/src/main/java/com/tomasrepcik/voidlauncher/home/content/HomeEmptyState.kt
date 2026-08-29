package com.tomasrepcik.voidlauncher.home.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R

@Composable
internal fun BoxScope.HomeEmptyState(
    isScheduleActive: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenSchedules: () -> Unit,
) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .padding(top = 88.dp, bottom = 80.dp)
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .testTag("home_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EmptyStateIcon()
        EmptyStateMessage(isScheduleActive)
        EmptyStateActions(onOpenDrawer, onOpenSchedules)
    }
}

@Composable
private fun EmptyStateIcon() {
    Surface(
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Apps,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun EmptyStateMessage(isScheduleActive: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (isScheduleActive) {
                    R.string.no_apps_scheduled_now
                } else {
                    R.string.home_empty_hint
                }
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyStateActions(
    onOpenDrawer: () -> Unit,
    onOpenSchedules: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalButton(
            onClick = onOpenDrawer,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_add_app_button"),
        ) {
            Icon(Icons.Outlined.Apps, contentDescription = null)
            Text(
                text = stringResource(R.string.home_open_app_list),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        OutlinedButton(
            onClick = onOpenSchedules,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_edit_schedules_button"),
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null)
            Text(
                text = stringResource(R.string.home_edit_schedules),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
