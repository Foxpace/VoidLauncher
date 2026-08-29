package com.tomasrepcik.voidlauncher.schedule.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.schedule.ScheduleHeader
import com.tomasrepcik.voidlauncher.schedule.ScheduleScaffold

@Composable
fun ScheduleEditorScreen(
    state: ScheduleEditorUiState,
    onBack: () -> Unit,
    onAction: (ScheduleEditorAction) -> Unit,
) {
    if (state.isAppPickerOpen) {
        BackHandler { onAction(ScheduleEditorAction.CloseAppPicker) }
        ScheduleAppPickerScreen(
            state = state,
            onBack = { onAction(ScheduleEditorAction.CloseAppPicker) },
            onAction = onAction,
        )
        return
    }

    ScheduleScaffold(
        onBack = onBack,
        topContent = {
            ScheduleHeader(
                title = stringResource(
                    if (state.id == null) R.string.new_schedule else R.string.edit_schedule,
                ),
                onBack = onBack,
            )
        },
        bottomContent = {
            ScheduleSaveBar(
                canSave = state.canSave,
                onSave = { onAction(ScheduleEditorAction.SaveSchedule) },
            )
        },
    ) {
        scheduleEditorItems(state = state, onAction = onAction)
    }
}

private fun LazyListScope.scheduleEditorItems(
    state: ScheduleEditorUiState,
    onAction: (ScheduleEditorAction) -> Unit,
) {
    scheduleIdentityItems(state = state, onAction = onAction)
    scheduleTimingItems(state = state, onAction = onAction)
    scheduleAppSelectionItems(state = state, onAction = onAction)
}

private fun LazyListScope.scheduleIdentityItems(
    state: ScheduleEditorUiState,
    onAction: (ScheduleEditorAction) -> Unit,
) {
    if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    if (state.id != null) {
        item {
            ScheduleEnabledField(
                enabled = state.enabled,
                onEnabledChange = { onAction(ScheduleEditorAction.SetEnabled(it)) },
            )
        }
    }
    item {
        ScheduleNameField(
            name = state.name,
            onNameChange = { onAction(ScheduleEditorAction.ChangeName(it)) },
        )
    }
}

private fun LazyListScope.scheduleTimingItems(
    state: ScheduleEditorUiState,
    onAction: (ScheduleEditorAction) -> Unit,
) {
    item { SectionTitle(stringResource(R.string.schedule_days)) }
    item { DayPicker(state.days, onAction) }
    item { SectionTitle(stringResource(R.string.schedule_time)) }
    item {
        ScheduleTimeRangePicker(
            startMinute = state.startMinute,
            endMinute = state.endMinute,
            onStartTimeSelected = { onAction(ScheduleEditorAction.ChangeStartTime(it)) },
            onEndTimeSelected = { onAction(ScheduleEditorAction.ChangeEndTime(it)) },
        )
    }
    item {
        Text(
            text = stringResource(R.string.schedule_time_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

private fun LazyListScope.scheduleAppSelectionItems(
    state: ScheduleEditorUiState,
    onAction: (ScheduleEditorAction) -> Unit,
) {
    item {
        SectionTitle(stringResource(R.string.schedule_apps_selected, state.selectedAppKeys.size))
    }
    item {
        Text(
            text = stringResource(R.string.schedule_apps_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
    item {
        ScheduleAppSelectionCard(
            selectedAppCount = state.selectedAppKeys.size,
            onChooseApps = { onAction(ScheduleEditorAction.OpenAppPicker) },
        )
    }
    items(
        items = state.installedApps.filter { app -> app.key in state.selectedAppKeys },
        key = { app -> "selected:${app.key.packageName}:${app.key.activityName}" },
    ) { app ->
        SelectedScheduleAppRow(app = app)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )
}
