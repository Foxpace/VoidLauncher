package com.tomasrepcik.voidlauncher.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.design.components.AppIcon
import kotlinx.coroutines.launch

private data class DrawerAppRowActions(
    val onClick: () -> Unit,
    val onAddToHome: () -> Unit,
    val onRemoveFromHome: () -> Unit,
    val onUninstall: () -> Unit,
)

@Composable
internal fun DrawerAppBrowser(
    state: DrawerUiState,
    actions: AppDrawerActions,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("drawer_list"),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(
                items = state.apps,
                key = { _, app -> "${app.key.packageName}:${app.key.activityName}" },
            ) { index, app ->
                val sectionLetter = state.sectionLetters.getValue(app.key)
                val previousLetter = state.apps.getOrNull(index - 1)?.let { previousApp ->
                    state.sectionLetters.getValue(previousApp.key)
                }
                if (sectionLetter != previousLetter) {
                    Text(
                        text = sectionLetter.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )
                }
                DrawerAppRow(
                    app = app,
                    isPinned = app.key in state.pinnedAppKeys,
                    actions = DrawerAppRowActions(
                        onClick = { actions.onAppClicked(app) },
                        onAddToHome = { actions.onAddHomeApp(app) },
                        onRemoveFromHome = { actions.onRemoveHomeApp(app) },
                        onUninstall = { actions.onUninstallApp(app) },
                    ),
                )
            }
        }

        if (state.query.isBlank()) {
            AlphabetRail(
                letters = state.alphabetIndex.keys.toList(),
                onLetterClick = { letter ->
                    state.alphabetIndex[letter]?.let { position ->
                        scope.launch { listState.animateScrollToItem(position) }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppRow(
    app: InstalledApp,
    isPinned: Boolean,
    actions: DrawerAppRowActions,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .combinedClickable(
                    onClick = actions.onClick,
                    onLongClick = { showMenu = true },
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(22.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                app = app,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Text(text = app.label, style = MaterialTheme.typography.bodyLarge)
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            if (isPinned) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.remove_from_home)) },
                    onClick = {
                        showMenu = false
                        actions.onRemoveFromHome()
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_to_home)) },
                    onClick = {
                        showMenu = false
                        actions.onAddToHome()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.uninstall)) },
                onClick = {
                    showMenu = false
                    actions.onUninstall()
                },
            )
        }
    }
}

@Composable
private fun AlphabetRail(
    letters: List<Char>,
    onLetterClick: (Char) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .testTag("drawer_alphabet_rail")
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Box(
                modifier = Modifier
                    .clickable { onLetterClick(letter) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
