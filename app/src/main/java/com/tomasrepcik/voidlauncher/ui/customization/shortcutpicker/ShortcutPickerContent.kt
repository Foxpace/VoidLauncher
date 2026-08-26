package com.tomasrepcik.voidlauncher.ui.customization.shortcutpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.ui.components.AppIcon
import com.tomasrepcik.voidlauncher.ui.components.LauncherSearchField
import com.tomasrepcik.voidlauncher.ui.customization.SettingsSectionTitle

@Composable
internal fun ShortcutPickerContent(
    state: ShortcutPickerUiState,
    actions: ShortcutPickerActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        systemShortcutItems(actions, enabled = !state.isSaving)
        appShortcutItems(state, actions)
    }
}

private fun LazyListScope.systemShortcutItems(
    actions: ShortcutPickerActions,
    enabled: Boolean,
) {
    item { SettingsSectionTitle(stringResource(R.string.system_shortcuts)) }
    item {
        SystemShortcutButtons(
            onContactsSelected = actions.onContactsSelected,
            onCameraSelected = actions.onCameraSelected,
            enabled = enabled,
        )
    }
}

private fun LazyListScope.appShortcutItems(
    state: ShortcutPickerUiState,
    actions: ShortcutPickerActions,
) {
    item { SettingsSectionTitle(stringResource(R.string.apps)) }
    item {
        LauncherSearchField(
            value = state.query,
            onValueChange = actions.onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholderText = stringResource(R.string.filter_apps),
        )
    }
    if (state.isLoading && state.apps.isEmpty()) {
        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
    }
    if (state.isSaving) {
        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
    }
    items(
        items = state.apps,
        key = { "${it.key.packageName}:${it.key.activityName}" },
    ) { app ->
        ShortcutAppRow(
            app = app,
            enabled = !state.isSaving,
            onSelect = { actions.onAppSelected(app) },
        )
    }
}

@Composable
private fun SystemShortcutButtons(
    onContactsSelected: () -> Unit,
    onCameraSelected: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(onClick = onContactsSelected, enabled = enabled) {
            Text(stringResource(R.string.contacts))
        }
        OutlinedButton(onClick = onCameraSelected, enabled = enabled) {
            Text(stringResource(R.string.camera))
        }
    }
}

@Composable
private fun ShortcutAppRow(
    app: InstalledApp,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    ElevatedCard(onClick = onSelect, enabled = enabled) {
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
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(6.dp),
            )
            Text(text = app.label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
