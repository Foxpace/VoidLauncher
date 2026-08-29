package com.tomasrepcik.voidlauncher.schedule.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.design.components.AppIcon
import com.tomasrepcik.voidlauncher.design.components.LauncherSearchField
import com.tomasrepcik.voidlauncher.design.components.LauncherSearchOptions
import com.tomasrepcik.voidlauncher.schedule.ScheduleHeader
import com.tomasrepcik.voidlauncher.schedule.ScheduleScaffold

@Composable
internal fun ScheduleAppPickerScreen(
    state: ScheduleEditorUiState,
    onBack: () -> Unit,
    onAction: (ScheduleEditorAction) -> Unit,
) {
    ScheduleScaffold(
        onBack = onBack,
        topContent = {
            ScheduleHeader(
                title = stringResource(R.string.choose_schedule_apps),
                onBack = onBack,
            )
        },
        bottomContent = { AppPickerDoneBar(onBack) },
    ) {
        if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item {
            Text(
                text = stringResource(R.string.schedule_apps_selected, state.selectedAppKeys.size),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        item {
            LauncherSearchField(
                value = state.appQuery,
                onValueChange = { onAction(ScheduleEditorAction.ChangeAppQuery(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholderText = stringResource(R.string.filter_apps),
                options = LauncherSearchOptions(testTag = "schedule_app_search"),
            )
        }
        items(
            items = state.installedApps,
            key = { app -> "${app.key.packageName}:${app.key.activityName}" },
        ) { app ->
            ScheduleAppRow(
                app = app,
                selected = app.key in state.selectedAppKeys,
                onToggle = { onAction(ScheduleEditorAction.ToggleApp(app.key)) },
            )
        }
    }
}

@Composable
private fun AppPickerDoneBar(onDone: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("schedule_app_picker_done"),
        ) {
            Text(stringResource(R.string.done))
        }
    }
}

@Composable
private fun ScheduleAppRow(
    app: InstalledApp,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    ElevatedCard(
        onClick = onToggle,
        modifier = Modifier.testTag("schedule_app_${app.key.packageName}_${app.key.activityName}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(app = app, modifier = Modifier.size(40.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
        }
    }
}
