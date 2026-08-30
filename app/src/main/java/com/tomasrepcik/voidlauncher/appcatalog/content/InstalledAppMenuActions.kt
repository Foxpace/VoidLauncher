package com.tomasrepcik.voidlauncher.appcatalog.content

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.voidlauncher.R

internal data class InstalledAppMenuActions(
    val onAddToHome: () -> Unit,
    val onRemoveFromHome: () -> Unit,
    val onUninstall: () -> Unit,
)

@Composable
internal fun InstalledAppActionMenu(
    expanded: Boolean,
    isOnHome: Boolean,
    onDismissRequest: () -> Unit,
    actions: InstalledAppMenuActions,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        if (isOnHome) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_from_home)) },
                onClick = {
                    onDismissRequest()
                    actions.onRemoveFromHome()
                },
            )
        } else {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_to_home)) },
                onClick = {
                    onDismissRequest()
                    actions.onAddToHome()
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.uninstall)) },
            onClick = {
                onDismissRequest()
                actions.onUninstall()
            },
        )
    }
}
