package com.tomasrepcik.voidlauncher.ui.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.ui.components.AppIcon
import com.tomasrepcik.voidlauncher.ui.components.LauncherSearchField
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationContainer
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationActions

@Composable
fun CustomizationScreen(
    state: CustomizationUiState,
    onBack: () -> Unit,
    onEditShortcut: (ShortcutSlot) -> Unit,
    onOpenSchedules: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        SwipeNavigationContainer(
            modifier = Modifier.fillMaxSize(),
            actions = SwipeNavigationActions(onClose = onBack),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CustomizationHeader(
                    title = stringResource(R.string.customize_launcher),
                    onBack = onBack,
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .testTag("customization_root"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.bottom_shortcuts),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    items(
                        items = state.shortcuts,
                        key = { it.slot.name }
                    ) { shortcut ->
                        ShortcutEditorRow(
                            shortcut = shortcut,
                            onEdit = { onEditShortcut(shortcut.slot) },
                        )
                    }

                    item {
                        Text(
                            text = stringResource(R.string.app_schedules),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = onOpenSchedules,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_schedules_button"),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 14.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.plan_visible_apps),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(R.string.plan_visible_apps_summary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutPickerScreen(
    slot: ShortcutSlot,
    state: ShortcutPickerUiState,
    actions: ShortcutPickerActions,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        SwipeNavigationContainer(
            modifier = Modifier.fillMaxSize(),
            actions = SwipeNavigationActions(onClose = actions.onBack),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CustomizationHeader(
                    title = stringResource(R.string.pick_shortcut_title, slot.displayName()),
                    onBack = actions.onBack,
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.system_shortcuts),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(onClick = actions.onContactsSelected) {
                                Text(stringResource(R.string.contacts))
                            }
                            OutlinedButton(onClick = actions.onCameraSelected) {
                                Text(stringResource(R.string.camera))
                            }
                        }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.apps),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    item {
                        LauncherSearchField(
                            value = state.query,
                            onValueChange = actions.onQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholderText = stringResource(R.string.filter_apps),
                        )
                    }

                    if (state.isLoading && state.apps.isEmpty()) {
                        item {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }

                    items(
                        items = state.apps,
                        key = { "${it.key.packageName}:${it.key.activityName}" }
                    ) { app ->
                        ShortcutAppRow(
                            app = app,
                            onSelect = { actions.onAppSelected(app) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizationHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("customization_back_button"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}

@Composable
private fun ShortcutAppRow(
    app: InstalledApp,
    onSelect: () -> Unit,
) {
    ElevatedCard(onClick = onSelect) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppIcon(
                app = app,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(6.dp)
            )
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ShortcutEditorRow(
    shortcut: ResolvedShortcut,
    onEdit: () -> Unit,
) {
    ElevatedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ShortcutVisual(shortcut = shortcut)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.bottom_shortcut_title,
                        shortcut.slot.displayName()
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = shortcut.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit),
                )
            }
        }
    }
}

@Composable
private fun ShortcutVisual(
    shortcut: ResolvedShortcut,
) {
    when (shortcut.selection) {
        ShortcutSelection.SystemContacts -> Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )

        ShortcutSelection.SystemCamera -> Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )

        is ShortcutSelection.AppShortcut -> {
            shortcut.installedApp?.let { installedApp ->
                AppIcon(
                    app = installedApp,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun ShortcutSlot.displayName(): String = when (this) {
    ShortcutSlot.LEFT -> stringResource(R.string.left)
    ShortcutSlot.RIGHT -> stringResource(R.string.right)
}
