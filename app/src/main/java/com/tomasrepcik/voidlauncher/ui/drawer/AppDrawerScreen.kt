package com.tomasrepcik.voidlauncher.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.domain.search.InstalledAppSearch
import com.tomasrepcik.voidlauncher.ui.components.AppIcon
import com.tomasrepcik.voidlauncher.ui.components.LauncherSearchField
import com.tomasrepcik.voidlauncher.ui.components.LauncherSearchOptions
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationContainer
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationActions
import kotlinx.coroutines.launch

private data class DrawerAppRowActions(
    val onClick: () -> Unit,
    val onAddToHome: () -> Unit,
    val onRemoveFromHome: () -> Unit,
    val onUninstall: () -> Unit,
)

@Composable
fun AppDrawerScreen(
    state: DrawerUiState,
    actions: AppDrawerActions,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("drawer_root"),
    ) {
        SwipeNavigationContainer(
            modifier = Modifier.fillMaxSize(),
            actions = SwipeNavigationActions(onClose = actions.onBack),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                AppDrawerHeader(actions)
                AppDrawerContent(
                    state = state,
                    actions = actions,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AppDrawerHeader(actions: AppDrawerActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Text(
            text = stringResource(R.string.all_apps),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = actions.onOpenSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.customize_launcher),
            )
        }
    }
}

@Composable
private fun AppDrawerContent(
    state: DrawerUiState,
    actions: AppDrawerActions,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val alphabetIndex = remember(state.apps) { buildAlphabetIndex(state.apps) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LauncherSearchField(
            value = state.query,
            onValueChange = actions.onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholderText = stringResource(R.string.filter_apps),
            options = LauncherSearchOptions(testTag = "drawer_search_field"),
        )

        if (state.isLoading && state.apps.isEmpty()) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_loading_indicator"),
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
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
                    key = { _, app -> "${app.key.packageName}:${app.key.activityName}" }
                ) { index, app ->
                    val sectionLetter = sectionLetterFor(app)
                    val previousLetter = state.apps.getOrNull(index - 1)?.let(::sectionLetterFor)
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

            AlphabetRail(
                letters = alphabetIndex.keys.toList(),
                onLetterClick = { letter ->
                    alphabetIndex[letter]?.let { position ->
                        scope.launch {
                            listState.animateScrollToItem(position)
                        }
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
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Box(
                modifier = Modifier
                    .clickable { onLetterClick(letter) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
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

private fun buildAlphabetIndex(apps: List<InstalledApp>): Map<Char, Int> {
    val index = linkedMapOf<Char, Int>()
    apps.forEachIndexed { position, app ->
        index.putIfAbsent(sectionLetterFor(app), position)
    }
    return index
}

private fun sectionLetterFor(app: InstalledApp): Char {
    return installedAppSearch.sectionLetter(app.label)
}

private val installedAppSearch = InstalledAppSearch()
