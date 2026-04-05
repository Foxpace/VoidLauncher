package com.tomasrepcik.voidlauncher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.ui.components.AppIcon
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationContainer
import kotlin.math.abs

@Composable
fun HomeScreen(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onPrimarySearch: () -> Unit,
    onBrowserSearch: () -> Unit,
    onPlayStoreSearch: () -> Unit,
    onMapsSearch: () -> Unit,
    onAppHint: () -> Unit,
    onAppClicked: (InstalledApp) -> Unit,
    onShortcutClicked: (ResolvedShortcut) -> Unit,
    onOpenDrawer: () -> Unit,
    onRemoveHomeApp: (InstalledApp) -> Unit,
    onReorderHomeApps: (Int, Int) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val isDraggingAnyItem = draggedIndex >= 0

    SwipeNavigationContainer(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag("home_root")
            .pointerInput(state.query) {
                detectTapGestures {
                    if (state.query.isNotBlank()) {
                        onQueryChange("")
                    }
                    focusManager.clearFocus()
                }
            },
        onOpen = onOpenDrawer,
    ) {
        if (state.homeApps.isEmpty()) {
            FilledTonalButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 88.dp)
                    .testTag("home_add_app_button"),
            ) {
                Text(stringResource(R.string.home_add_app))
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 88.dp, bottom = 96.dp)
                    .testTag("home_primary_apps"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(
                    items = state.homeApps,
                    key = { _, app -> "${app.key.packageName}:${app.key.activityName}" }
                ) { index, app ->
                    val isDragging = index == draggedIndex
                    val elevation by animateDpAsState(
                        targetValue = if (isDragging) 8.dp else 0.dp,
                        label = "dragElevation",
                    )
                    HomeAppRow(
                        app = app,
                        onClick = { onAppClicked(app) },
                        onRemove = { onRemoveHomeApp(app) },
                        onDragStart = {
                            draggedIndex = index
                            dragOffsetY = 0f
                        },
                        onDrag = { dragAmount ->
                            dragOffsetY += dragAmount
                            if (draggedIndex < 0) return@HomeAppRow

                            val draggedItem = listState.layoutInfo.visibleItemsInfo
                                .find { it.index == draggedIndex }
                                ?: return@HomeAppRow
                            val draggedCenter =
                                draggedItem.offset + draggedItem.size / 2 + dragOffsetY.toInt()
                            val targetIndex = when {
                                dragAmount > 0 -> draggedIndex + 1
                                dragAmount < 0 -> draggedIndex - 1
                                else -> draggedIndex
                            }
                            val targetItem = listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == targetIndex }

                            if (targetItem != null) {
                                val targetCenter = targetItem.offset + targetItem.size / 2
                                val passedTarget =
                                    (dragAmount > 0 && draggedCenter > targetCenter) ||
                                        (dragAmount < 0 && draggedCenter < targetCenter)
                                if (passedTarget) {
                                    val offsetDelta = targetItem.offset - draggedItem.offset
                                    onReorderHomeApps(draggedIndex, targetItem.index)
                                    dragOffsetY -= offsetDelta.toFloat()
                                    draggedIndex = targetItem.index
                                }
                            }
                        },
                        onDragEnd = {
                            draggedIndex = -1
                            dragOffsetY = 0f
                        },
                        modifier = Modifier
                            .then(
                                if (isDragging) {
                                    Modifier
                                        .zIndex(1f)
                                        .graphicsLayer { translationY = dragOffsetY }
                                        .shadow(elevation, RoundedCornerShape(16.dp))
                                } else if (!isDraggingAnyItem) {
                                    Modifier.animateItem()
                                } else {
                                    Modifier
                                }
                            ),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .testTag("home_shortcuts"),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val leftShortcut = state.shortcuts.firstOrNull { it.slot.ordinal == 0 }
            val rightShortcut = state.shortcuts.firstOrNull { it.slot.ordinal == 1 }
            ShortcutIconButton(
                shortcut = leftShortcut,
                onClick = { leftShortcut?.let(onShortcutClicked) },
            )
            ShortcutIconButton(
                shortcut = rightShortcut,
                onClick = { rightShortcut?.let(onShortcutClicked) },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .zIndex(1f),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_field"),
                placeholder = { Text(stringResource(R.string.search_apps_or_web)) },
                trailingIcon = {
                    IconButton(onClick = onPrimarySearch) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.search),
                        )
                    }
                },
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
            )

            AnimatedVisibility(
                visible = state.query.isNotBlank(),
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                SearchOverlay(
                    suggestions = state.searchSuggestions,
                    hintMessage = state.hintMessage,
                    onSuggestionClicked = { app ->
                        onQueryChange("")
                        onAppClicked(app)
                    },
                    onPlayStoreSearch = onPlayStoreSearch,
                    onMapsSearch = onMapsSearch,
                    onBrowserSearch = onBrowserSearch,
                    onAppHint = onAppHint,
                )
            }
        }
    }
}

@Composable
private fun SearchOverlay(
    suggestions: List<InstalledApp>,
    hintMessage: String?,
    onSuggestionClicked: (InstalledApp) -> Unit,
    onPlayStoreSearch: () -> Unit,
    onMapsSearch: () -> Unit,
    onBrowserSearch: () -> Unit,
    onAppHint: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconButton(
                    onClick = onPlayStoreSearch,
                    modifier = Modifier.testTag("home_play_store_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = stringResource(R.string.open_google_play),
                    )
                }
                IconButton(
                    onClick = onMapsSearch,
                    modifier = Modifier.testTag("home_maps_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = stringResource(R.string.open_google_maps),
                    )
                }
                IconButton(
                    onClick = onBrowserSearch,
                    modifier = Modifier.testTag("home_browser_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = stringResource(R.string.open_browser_search),
                    )
                }
                IconButton(
                    onClick = onAppHint,
                    modifier = Modifier.testTag("home_hint_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiObjects,
                        contentDescription = stringResource(R.string.show_app_hint),
                    )
                }
            }

            hintMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            if (suggestions.isNotEmpty()) {
                suggestions.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSuggestionClicked(app) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppIcon(
                            app = app,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.no_matching_apps),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeAppRow(
    app: InstalledApp,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .pointerInput(app.key) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val longPress = awaitLongPressOrCancellation(down.id)
                        if (longPress == null) {
                            return@awaitEachGesture
                        }

                        var isDragging = false
                        var totalDeltaY = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) {
                                if (isDragging) {
                                    onDragEnd()
                                } else {
                                    showMenu = true
                                }
                                break
                            }

                            totalDeltaY += change.positionChange().y
                            if (!isDragging && abs(totalDeltaY) > viewConfiguration.touchSlop) {
                                isDragging = true
                                showMenu = false
                                onDragStart()
                            }

                            if (isDragging) {
                                val dragAmount = change.positionChange().y
                                if (dragAmount != 0f) {
                                    onDrag(dragAmount)
                                    change.consume()
                                }
                            }
                        }
                    }
                }
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .testTag("home_app_${app.label}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppIcon(
                app = app,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_from_home)) },
                onClick = {
                    showMenu = false
                    onRemove()
                },
            )
        }
    }
}

@Composable
private fun ShortcutIconButton(
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

            is ShortcutSelection.AppShortcut -> {
                val installedApp = shortcut.installedApp
                if (installedApp != null) {
                    AppIcon(
                        app = installedApp,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = shortcut.label,
                        modifier = iconModifier,
                    )
                }
            }

            null -> Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = iconModifier,
            )
        }
    }
}
