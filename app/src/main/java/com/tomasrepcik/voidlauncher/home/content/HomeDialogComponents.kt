package com.tomasrepcik.voidlauncher.home.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.ResolvedShortcut
import com.tomasrepcik.voidlauncher.launcher.ShortcutSelection
import com.tomasrepcik.voidlauncher.design.components.AppIcon
import com.tomasrepcik.voidlauncher.appcatalog.search.SearchTarget

@Composable
internal fun AssistantPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (SearchTarget) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_ai_assistant)) },
        text = {
            Column {
                listOf(
                    SearchTarget.ChatGpt to R.string.chatgpt,
                    SearchTarget.Claude to R.string.claude,
                    SearchTarget.Gemini to R.string.gemini,
                ).forEach { (target, label) ->
                    TextButton(
                        onClick = { onSelect(target) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_assistant_$target"),
                    ) {
                        Text(stringResource(label))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun RenameAppDialog(
    app: InstalledApp,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var value by remember(app.key) { mutableStateOf(TextFieldValue(app.label)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_app)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.text.trim().ifBlank { null }) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun ShortcutIconButton(
    shortcut: ResolvedShortcut?,
    onClick: () -> Unit,
) {
    val iconModifier = Modifier.size(28.dp)
    IconButton(
        onClick = onClick,
        enabled = shortcut != null,
        modifier = Modifier.testTag("shortcut_${shortcut?.slot?.name ?: "empty"}"),
    ) {
        when (shortcut?.selection) {
            ShortcutSelection.SystemContacts -> Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = stringResource(R.string.contacts),
                modifier = iconModifier,
            )
            ShortcutSelection.SystemCamera -> Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = stringResource(R.string.camera),
                modifier = iconModifier,
            )
            is ShortcutSelection.AppShortcut -> ShortcutAppIcon(shortcut, iconModifier)
            null -> Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = iconModifier,
            )
        }
    }
}

@Composable
private fun ShortcutAppIcon(shortcut: ResolvedShortcut, modifier: Modifier) {
    val installedApp = shortcut.installedApp
    if (installedApp != null) {
        AppIcon(app = installedApp, modifier = modifier.clip(CircleShape))
    } else {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = shortcut.label,
            modifier = modifier,
        )
    }
}
