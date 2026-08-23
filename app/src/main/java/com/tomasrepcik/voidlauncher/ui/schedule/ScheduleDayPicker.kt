package com.tomasrepcik.voidlauncher.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import java.time.DayOfWeek

@Composable
internal fun DayPicker(
    selected: Set<DayOfWeek>,
    onIntent: (ScheduleEditorIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DayPresetTabs(selected, onIntent)
        DayToggleRow(selected, onIntent)
    }
}

@Composable
private fun DayPresetTabs(
    selected: Set<DayOfWeek>,
    onIntent: (ScheduleEditorIntent) -> Unit,
) {
    val presets = listOf(
        stringResource(R.string.every_day) to EVERY_DAY,
        stringResource(R.string.weekdays) to WEEKDAYS,
        stringResource(R.string.weekend) to WEEKEND,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { preset ->
            DayPresetTab(
                label = preset.first,
                selected = selected == preset.second,
                onClick = { onIntent(ScheduleEditorIntent.DaysChanged(preset.second)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayPresetTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag("schedule_preset_$label"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(vertical = 10.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
        )
    }
}

@Composable
private fun DayToggleRow(
    selected: Set<DayOfWeek>,
    onIntent: (ScheduleEditorIntent) -> Unit,
) {
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
