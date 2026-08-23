package com.tomasrepcik.voidlauncher.ui.schedule

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import com.tomasrepcik.voidlauncher.domain.schedule.MINUTES_PER_DAY
import com.tomasrepcik.voidlauncher.ui.components.AppIcon
import com.tomasrepcik.voidlauncher.ui.components.LauncherSearchField
import com.tomasrepcik.voidlauncher.ui.components.LauncherSearchOptions
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationActions
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationContainer
import java.time.DayOfWeek

@Composable
fun ScheduleListScreen(
    state: ScheduleListUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onIntent: (ScheduleListIntent) -> Unit,
) {
    ScheduleSurface(
        onBack = onBack,
        topContent = {
            ScheduleHeader(stringResource(R.string.app_schedules), onBack, onAdd)
        },
    ) {
        if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (!state.isLoading && state.schedules.isEmpty()) {
            item {
                ElevatedCard {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.no_schedules),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.regular_apps_when_no_schedule),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Button(
                            onClick = onAdd,
                            modifier = Modifier.testTag("schedule_add_button"),
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Text(stringResource(R.string.add_schedule))
                        }
                    }
                }
            }
        }
        items(state.schedules, key = AppSchedule::id) { schedule ->
            ScheduleRow(
                schedule = schedule,
                onEdit = { onEdit(schedule.id) },
                onDelete = { onIntent(ScheduleListIntent.Delete(schedule.id)) },
                onEnabledChanged = { enabled ->
                    onIntent(ScheduleListIntent.SetEnabled(schedule, enabled))
                },
            )
        }
    }
}

@Composable
fun ScheduleEditorScreen(
    state: ScheduleEditorUiState,
    onBack: () -> Unit,
    onIntent: (ScheduleEditorIntent) -> Unit,
) {
    if (state.isAppPickerOpen) {
        BackHandler { onIntent(ScheduleEditorIntent.CloseAppPicker) }
        ScheduleAppPickerScreen(
            state = state,
            onBack = { onIntent(ScheduleEditorIntent.CloseAppPicker) },
            onIntent = onIntent,
        )
        return
    }

    ScheduleSurface(
        onBack = onBack,
        topContent = {
            ScheduleHeader(
                title = stringResource(
                    if (state.id == null) R.string.new_schedule else R.string.edit_schedule
                ),
                onBack = onBack,
            )
        },
        bottomContent = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { onIntent(ScheduleEditorIntent.Save) },
                    enabled = state.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .testTag("schedule_save_button"),
                ) {
                    Text(stringResource(R.string.save_schedule))
                }
            }
        },
    ) {
        if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (state.id != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.schedule_enabled),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.schedule_enabled_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = { onIntent(ScheduleEditorIntent.EnabledChanged(it)) },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.name,
                onValueChange = { onIntent(ScheduleEditorIntent.NameChanged(it)) },
                label = { Text(stringResource(R.string.schedule_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { SectionTitle(stringResource(R.string.schedule_days)) }
        item { DayPicker(state.days, onIntent) }
        item { SectionTitle(stringResource(R.string.schedule_time)) }
        item { TimeRangePicker(state.startMinute, state.endMinute, onIntent) }
        item {
            Text(
                text = stringResource(R.string.schedule_time_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        item {
            SectionTitle(
                stringResource(R.string.schedule_apps_selected, state.selectedAppKeys.size)
            )
        }
        item {
            Text(
                text = stringResource(R.string.schedule_apps_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        item {
            OutlinedButton(
                onClick = { onIntent(ScheduleEditorIntent.OpenAppPicker) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_choose_apps_button"),
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Text(
                    text = stringResource(R.string.choose_schedule_apps),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
                Text(stringResource(R.string.schedule_app_count, state.selectedAppKeys.size))
            }
        }
        items(
            items = state.installedApps.filter { app -> app.key in state.selectedAppKeys },
            key = { app -> "selected:${app.key.packageName}:${app.key.activityName}" },
        ) { app ->
            ElevatedCard(
                modifier = Modifier.testTag(
                    "schedule_selected_app_${app.key.packageName}_${app.key.activityName}"
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppIcon(
                        app = app,
                        modifier = Modifier.size(40.dp),
                    )
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleAppPickerScreen(
    state: ScheduleEditorUiState,
    onBack: () -> Unit,
    onIntent: (ScheduleEditorIntent) -> Unit,
) {
    ScheduleSurface(
        onBack = onBack,
        topContent = {
            ScheduleHeader(
                title = stringResource(R.string.choose_schedule_apps),
                onBack = onBack,
            )
        },
        bottomContent = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .testTag("schedule_app_picker_done"),
                ) {
                    Text(stringResource(R.string.done))
                }
            }
        },
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
                onValueChange = { onIntent(ScheduleEditorIntent.AppQueryChanged(it)) },
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
                onToggle = { onIntent(ScheduleEditorIntent.AppToggled(app.key)) },
            )
        }
    }
}

@Composable
private fun ScheduleSurface(
    onBack: () -> Unit,
    topContent: @Composable () -> Unit,
    bottomContent: (@Composable () -> Unit)? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        SwipeNavigationContainer(
            modifier = Modifier.fillMaxSize(),
            actions = SwipeNavigationActions(onClose = onBack),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                topContent()
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            bottom = if (bottomContent == null) 12.dp else 88.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content,
                    )
                    bottomContent?.let { fixedContent ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                        ) {
                            fixedContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleHeader(
    title: String,
    onBack: () -> Unit,
    onAdd: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.weight(1f),
        )
        onAdd?.let { add ->
            IconButton(onClick = add) {
                Icon(Icons.Outlined.Add, stringResource(R.string.new_schedule))
            }
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${schedule.startMinute.asTime()} to ${schedule.endMinute.asTime()}",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(schedule.name, style = MaterialTheme.typography.titleMedium)
                }
                Switch(
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

@Composable
private fun DayPicker(
    selected: Set<DayOfWeek>,
    onIntent: (ScheduleEditorIntent) -> Unit,
) {
    val presets = listOf(
        stringResource(R.string.every_day) to EVERY_DAY,
        stringResource(R.string.weekdays) to WEEKDAYS,
        stringResource(R.string.weekend) to WEEKEND,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { preset ->
                FilterChip(
                    selected = selected == preset.second,
                    onClick = { onIntent(ScheduleEditorIntent.DaysChanged(preset.second)) },
                    label = { Text(preset.first) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("schedule_preset_${preset.first}"),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DayOfWeek.entries.forEach { day ->
                val isSelected = day in selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .background(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape,
                        )
                        .selectable(
                            selected = isSelected,
                            role = Role.Checkbox,
                            onClick = { onIntent(ScheduleEditorIntent.DayToggled(day)) },
                        )
                        .testTag("schedule_day_${day.name}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = day.shortName(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeRangePicker(
    startMinute: Int,
    endMinute: Int,
    onIntent: (ScheduleEditorIntent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TimeButton(
            label = stringResource(R.string.start_time),
            minute = startMinute,
            modifier = Modifier.weight(1f),
            onSelected = { onIntent(ScheduleEditorIntent.StartTimeChanged(it)) },
        )
        TimeButton(
            label = stringResource(R.string.end_time),
            minute = endMinute,
            modifier = Modifier.weight(1f),
            onSelected = { onIntent(ScheduleEditorIntent.EndTimeChanged(it)) },
        )
    }
}

@Composable
private fun TimeButton(
    label: String,
    minute: Int,
    modifier: Modifier,
    onSelected: (Int) -> Unit,
) {
    val context = LocalContext.current
    val safeMinute = minute.coerceIn(0, MINUTES_PER_DAY - 1)
    OutlinedButton(
        onClick = {
            TimePickerDialog(
                context,
                { _, hour, selectedMinute -> onSelected(hour * MINUTES_PER_HOUR + selectedMinute) },
                safeMinute / MINUTES_PER_HOUR,
                safeMinute % MINUTES_PER_HOUR,
                android.text.format.DateFormat.is24HourFormat(context),
            ).show()
        },
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(safeMinute.asTime(), style = MaterialTheme.typography.titleMedium)
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
            AppIcon(
                app = app,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.secondary,
    )
}
