package com.tomasrepcik.voidlauncher.ui.customization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R

@Composable
internal fun ScheduleSettings(onOpenSchedules: () -> Unit) {
    SettingsLinkSection(
        content = SettingsLinkContent(
            sectionTitle = stringResource(R.string.app_schedules),
            title = stringResource(R.string.plan_visible_apps),
            summary = stringResource(R.string.plan_visible_apps_summary),
            icon = Icons.Outlined.Schedule,
            testTag = "open_schedules_button",
        ),
        onClick = onOpenSchedules,
    )
}

@Composable
internal fun HelpSettings(onShowNavigationTutorial: () -> Unit) {
    SettingsLinkSection(
        content = SettingsLinkContent(
            sectionTitle = stringResource(R.string.help),
            title = stringResource(R.string.show_navigation_tutorial),
            summary = stringResource(R.string.show_navigation_tutorial_summary),
            icon = Icons.Outlined.TouchApp,
            testTag = "show_navigation_tutorial_button",
        ),
        onClick = onShowNavigationTutorial,
    )
}

@Composable
private fun SettingsLinkSection(
    content: SettingsLinkContent,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionTitle(content.sectionTitle)
        ElevatedCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(content.testTag),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(imageVector = content.icon, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = content.title,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = content.summary,
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
        }
    }
}

private data class SettingsLinkContent(
    val sectionTitle: String,
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val testTag: String,
)
