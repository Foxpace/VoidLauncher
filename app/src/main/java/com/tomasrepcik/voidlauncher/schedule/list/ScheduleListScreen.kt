package com.tomasrepcik.voidlauncher.schedule.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.schedule.data.AppSchedule
import com.tomasrepcik.voidlauncher.design.components.LauncherSwitch
import com.tomasrepcik.voidlauncher.schedule.ScheduleHeader
import com.tomasrepcik.voidlauncher.schedule.ScheduleScaffold
import com.tomasrepcik.voidlauncher.schedule.asTime
import com.tomasrepcik.voidlauncher.schedule.summary

@Composable
fun ScheduleListScreen(
    state: ScheduleListUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onAction: (ScheduleListAction) -> Unit,
) {
    ScheduleScaffold(
        onBack = onBack,
        topContent = {
            ScheduleHeader(stringResource(R.string.app_schedules), onBack, onAdd)
        },
    ) {
        if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (!state.isLoading && state.schedules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    ScheduleEmptyState(onAdd)
                }
            }
        }
        items(state.schedules, key = AppSchedule::id) { schedule ->
            ScheduleRow(
                schedule = schedule,
                onEdit = { onEdit(schedule.id) },
                    onDelete = { onAction(ScheduleListAction.DeleteSchedule(schedule.id)) },
                onEnabledChanged = { enabled ->
                        onAction(ScheduleListAction.SetScheduleEnabled(schedule, enabled))
                },
            )
        }
    }
}

@Composable
private fun ScheduleRow(
    schedule: AppSchedule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
) {
    ElevatedCard(
        onClick = onEdit,
        modifier = Modifier.testTag("schedule_${schedule.id}"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${schedule.startMinute.asTime()} to ${schedule.endMinute.asTime()}",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(schedule.name, style = MaterialTheme.typography.titleMedium)
                }
                LauncherSwitch(
                    checked = schedule.enabled,
                    onCheckedChange = onEnabledChanged,
                    modifier = Modifier.testTag("schedule_enabled_${schedule.id}"),
                )
            }
            Text(
                text = schedule.summary(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.schedule_app_count, schedule.appKeys.size),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text(stringResource(R.string.edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, stringResource(R.string.delete_schedule))
                }
            }
        }
    }
}
