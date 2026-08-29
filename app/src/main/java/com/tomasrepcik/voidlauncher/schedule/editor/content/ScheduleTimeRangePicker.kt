package com.tomasrepcik.voidlauncher.schedule.editor.content

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.schedule.data.MINUTES_PER_DAY
import com.tomasrepcik.voidlauncher.schedule.MINUTES_PER_HOUR
import com.tomasrepcik.voidlauncher.schedule.asTime

@Composable
internal fun ScheduleTimeRangePicker(
    startMinute: Int,
    endMinute: Int,
    onStartTimeSelected: (Int) -> Unit,
    onEndTimeSelected: (Int) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeButton(
                label = stringResource(R.string.start_time),
                minute = startMinute,
                modifier = Modifier
                    .weight(1f)
                    .testTag("schedule_start_time_button"),
                onSelected = onStartTimeSelected,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            TimeButton(
                label = stringResource(R.string.end_time),
                minute = endMinute,
                modifier = Modifier
                    .weight(1f)
                    .testTag("schedule_end_time_button"),
                onSelected = onEndTimeSelected,
            )
        }
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
    Column(
        modifier = modifier
            .clickable(role = Role.Button) {
                TimePickerDialog(
                    context,
                    { _, hour, selectedMinute ->
                        onSelected(hour * MINUTES_PER_HOUR + selectedMinute)
                    },
                    safeMinute / MINUTES_PER_HOUR,
                    safeMinute % MINUTES_PER_HOUR,
                    android.text.format.DateFormat.is24HourFormat(context),
                ).show()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(safeMinute.asTime(), style = MaterialTheme.typography.titleLarge)
    }
}
