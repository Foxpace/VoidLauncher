package com.tomasrepcik.voidlauncher.ui.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.ui.components.AppIcon

@Composable
internal fun ShortcutSettings(
    shortcuts: List<ResolvedShortcut>,
    onEditShortcut: (ShortcutSlot) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionTitle(stringResource(R.string.bottom_shortcuts))
        shortcuts.forEach { shortcut ->
            ShortcutEditorRow(
                shortcut = shortcut,
                onEdit = { onEditShortcut(shortcut.slot) },
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
                        shortcut.slot.displayName(),
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
private fun ShortcutVisual(shortcut: ResolvedShortcut) {
    Box(
        modifier = Modifier.size(32.dp),
        contentAlignment = Alignment.Center,
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
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(4.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ShortcutSlot.displayName(): String = when (this) {
    ShortcutSlot.LEFT -> stringResource(R.string.left)
    ShortcutSlot.RIGHT -> stringResource(R.string.right)
}
