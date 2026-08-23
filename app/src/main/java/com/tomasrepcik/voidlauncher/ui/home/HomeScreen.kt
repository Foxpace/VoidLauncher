package com.tomasrepcik.voidlauncher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.ui.components.AppIcon
import com.tomasrepcik.voidlauncher.ui.components.LauncherSearchField
import com.tomasrepcik.voidlauncher.ui.components.LauncherSearchOptions
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationContainer
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationActions
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationConfig
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceState
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeBackgroundContainer
import kotlin.math.abs

private val BottomSwipeActivationZone = 96.dp
private val BottomSwipeFocusThreshold = 24.dp

@Composable
fun HomeScreen(
    state: HomeUiState,
    appearance: HomeAppearanceState,
    actions: HomeActions,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val controller = rememberHomeScreenController()
    val bottomSwipeInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(state.homeApps) {
        controller.retainApps(state.homeApps)
    }

    HomeBackgroundContainer(state = appearance) {
        SwipeNavigationContainer(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_root"),
            config = SwipeNavigationConfig(
                bottomActivationZone = BottomSwipeActivationZone,
                bottomThreshold = BottomSwipeFocusThreshold,
                bottomInset = bottomSwipeInset,
            ),
            actions = SwipeNavigationActions(
                onOpen = actions.onOpenDrawer,
                onBottomSwipeUp = {
                    searchFocusRequester.requestFocus()
                    keyboardController?.show()
                },
            ),
        ) {
            HomeContent(
                state = state,
                actions = actions,
                controller = controller,
                searchFocusRequester = searchFocusRequester,
                onDismiss = focusManager::clearFocus,
            )
        }

        controller.renamingApp?.let { app ->
            RenameAppDialog(
                app = app,
                onDismiss = controller::dismissRename,
                onSave = { newLabel ->
                    actions.onRenameHomeApp(app, newLabel)
                    controller.finishRename()
                },
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    actions: HomeActions,
    controller: HomeScreenController,
    searchFocusRequester: FocusRequester,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .pointerInput(state.query) {
                detectTapGestures {
                    if (state.query.isNotBlank()) actions.onQueryChange("")
                    controller.clearSelection()
                    onDismiss()
                }
            },
    ) {
        HomeApps(state, actions, controller)
        HomeShortcuts(state.shortcuts, actions.onShortcutClicked)
        HomeSearch(state, actions, searchFocusRequester)
        KeyboardSearchActions(state.query, actions)
    }
}

@Composable
private fun BoxScope.HomeApps(
    state: HomeUiState,
    actions: HomeActions,
    controller: HomeScreenController,
) {
    when {
        state.isLoading && state.homeApps.isEmpty() -> HomeLoadingState(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(top = 104.dp),
        )
        state.homeApps.isEmpty() -> HomeEmptyState(
            isScheduleActive = state.isScheduleActive,
            onOpenDrawer = actions.onOpenDrawer,
            onOpenSchedules = actions.onOpenSchedules,
        )
        else -> HomeAppList(state.homeApps, actions, controller)
    }
}

@Composable
private fun BoxScope.HomeAppList(
    apps: List<InstalledApp>,
    actions: HomeActions,
    controller: HomeScreenController,
) {
    LazyColumn(
        state = controller.listState,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(top = 88.dp, bottom = 96.dp)
            .testTag("home_primary_apps"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(
            items = apps,
            key = { _, app -> "${app.key.packageName}:${app.key.activityName}" },
        ) { index, app ->
            val rowState = controller.rowState(app)
            val elevation by animateDpAsState(
                targetValue = if (rowState.isDragging) 8.dp else 0.dp,
                label = "dragElevation",
            )
            HomeAppRow(
                app = app,
                state = rowState,
                actions = controller.rowActions(app, index, actions),
                modifier = Modifier.then(
                    when {
                        rowState.isDragging -> Modifier
                            .zIndex(1f)
                            .graphicsLayer { translationY = controller.dragOffsetY }
                            .shadow(elevation, RoundedCornerShape(16.dp))
                        !controller.isDragging -> Modifier.animateItem()
                        else -> Modifier
                    }
                ),
            )
        }
    }
}

@Composable
private fun BoxScope.HomeShortcuts(
    shortcuts: List<ResolvedShortcut>,
    onShortcutClicked: (ResolvedShortcut) -> Unit,
) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .testTag("home_shortcuts"),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val leftShortcut = shortcuts.firstOrNull { it.slot.ordinal == 0 }
        val rightShortcut = shortcuts.firstOrNull { it.slot.ordinal == 1 }
        ShortcutIconButton(leftShortcut) { leftShortcut?.let(onShortcutClicked) }
        ShortcutIconButton(rightShortcut) { rightShortcut?.let(onShortcutClicked) }
    }
}

@Composable
private fun BoxScope.HomeSearch(
    state: HomeUiState,
    actions: HomeActions,
    searchFocusRequester: FocusRequester,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .zIndex(1f),
    ) {
        LauncherSearchField(
            value = state.query,
            onValueChange = actions.onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholderText = stringResource(R.string.search_apps_or_web),
            options = LauncherSearchOptions(
                focusRequester = searchFocusRequester,
                testTag = "home_search_field",
                onSubmit = actions.onPrimarySearch,
            ),
        )
        AnimatedVisibility(
            visible = state.query.isNotBlank(),
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            SearchOverlay(
                state = SearchOverlayState(state.isLoading, state.searchSuggestions, state.hintMessage),
                actions = SearchOverlayActions(
                    onSuggestionClicked = { app ->
                        actions.onQueryChange("")
                        actions.onAppClicked(app)
                    },
                    onPlayStoreSearch = actions.onPlayStoreSearch,
                    onMapsSearch = actions.onMapsSearch,
                    onBrowserSearch = actions.onBrowserSearch,
                    onAppHint = actions.onAppHint,
                ),
            )
        }
    }
}

private data class SearchOverlayState(
    val isLoading: Boolean,
    val suggestions: List<InstalledApp>,
    val hintMessage: String?,
)

internal data class SearchOverlayActions(
    val onSuggestionClicked: (InstalledApp) -> Unit,
    val onPlayStoreSearch: () -> Unit,
    val onMapsSearch: () -> Unit,
    val onBrowserSearch: () -> Unit,
    val onAppHint: () -> Unit,
)

private data class HomeAppRowState(
    val showActions: Boolean,
    val showMenu: Boolean,
    val isDragging: Boolean,
)

private data class HomeAppRowActions(
    val onClick: () -> Unit,
    val onRemove: () -> Unit,
    val onRename: () -> Unit,
    val onUninstall: () -> Unit,
    val onToggleMenu: (Boolean) -> Unit,
    val onLongPress: () -> Unit,
    val onDragStart: () -> Unit,
    val onDrag: (Float) -> Unit,
    val onDragEnd: () -> Unit,
)

@Composable
private fun rememberHomeScreenController(): HomeScreenController {
    val listState = rememberLazyListState()
    return remember(listState) { HomeScreenController(listState) }
}

private class HomeScreenController(
    val listState: LazyListState,
) {
    private var draggedIndex by mutableIntStateOf(-1)
    private var draggedAppKey by mutableStateOf<AppKey?>(null)
    private var activeAppKey by mutableStateOf<AppKey?>(null)
    private var menuAppKey by mutableStateOf<AppKey?>(null)
    var dragOffsetY by mutableFloatStateOf(0f)
        private set
    var renamingApp by mutableStateOf<InstalledApp?>(null)
        private set

    val isDragging: Boolean
        get() = draggedAppKey != null

    fun retainApps(apps: List<InstalledApp>) {
        val keys = apps.map(InstalledApp::key).toSet()
        activeAppKey = activeAppKey.takeIf { it in keys }
        menuAppKey = menuAppKey.takeIf { it in keys }
        renamingApp = renamingApp?.takeIf { it.key in keys }
        if (draggedAppKey !in keys) finishDrag()
    }

    fun clearSelection() {
        activeAppKey = null
        menuAppKey = null
    }

    fun dismissRename() {
        renamingApp = null
    }

    fun finishRename() {
        renamingApp = null
        clearSelection()
    }

    fun rowState(app: InstalledApp): HomeAppRowState = HomeAppRowState(
        showActions = (!isDragging && activeAppKey == app.key) || menuAppKey == app.key,
        showMenu = menuAppKey == app.key,
        isDragging = draggedAppKey == app.key,
    )

    fun rowActions(app: InstalledApp, index: Int, actions: HomeActions) = HomeAppRowActions(
        onClick = { actions.onAppClicked(app) },
        onRemove = {
            clearSelection()
            actions.onRemoveHomeApp(app)
        },
        onRename = {
            menuAppKey = null
            renamingApp = app
        },
        onUninstall = {
            clearSelection()
            actions.onUninstallApp(app)
        },
        onToggleMenu = { expanded ->
            activeAppKey = app.key.takeIf { expanded }
            menuAppKey = app.key.takeIf { expanded }
        },
        onLongPress = {
            activeAppKey = app.key
            menuAppKey = null
        },
        onDragStart = {
            activeAppKey = app.key
            menuAppKey = null
            draggedAppKey = app.key
            draggedIndex = index
            dragOffsetY = 0f
        },
        onDrag = { amount -> moveDraggedApp(amount, actions.onReorderHomeApps) },
        onDragEnd = ::finishDrag,
    )

    private fun moveDraggedApp(amount: Float, reorder: (Int, Int) -> Unit) {
        dragOffsetY += amount
        val draggedItem = listState.layoutInfo.visibleItemsInfo.find { it.index == draggedIndex }
        val targetIndex = draggedIndex + amount.compareTo(0f)
        val targetItem = listState.layoutInfo.visibleItemsInfo.find { it.index == targetIndex }
        if (draggedItem != null && targetItem != null && passedTarget(amount, draggedItem, targetItem)) {
            reorder(draggedIndex, targetItem.index)
            dragOffsetY -= (targetItem.offset - draggedItem.offset).toFloat()
            draggedIndex = targetItem.index
        }
    }

    private fun passedTarget(
        amount: Float,
        draggedItem: androidx.compose.foundation.lazy.LazyListItemInfo,
        targetItem: androidx.compose.foundation.lazy.LazyListItemInfo,
    ): Boolean {
        val draggedCenter = draggedItem.offset + draggedItem.size / 2 + dragOffsetY.toInt()
        val targetCenter = targetItem.offset + targetItem.size / 2
        return if (amount > 0) draggedCenter > targetCenter else draggedCenter < targetCenter
    }

    private fun finishDrag() {
        draggedAppKey = null
        draggedIndex = -1
        dragOffsetY = 0f
    }
}

@Composable
private fun SearchOverlay(
    state: SearchOverlayState,
    actions: SearchOverlayActions,
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
            SearchActionButtons(actions = actions, testTagPrefix = "home")

            state.hintMessage?.let { message ->
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

            if (state.suggestions.isNotEmpty()) {
                state.suggestions.forEach { app ->
                    key(app.key) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { actions.onSuggestionClicked(app) }
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
                }
            } else if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
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

@Composable
private fun HomeLoadingState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_loading_indicator"),
        )
        Text(
            text = stringResource(R.string.loading_apps),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeAppRow(
    app: InstalledApp,
    state: HomeAppRowState,
    actions: HomeAppRowActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = !state.showActions, onClick = actions.onClick)
                .pointerInput(app.key) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val longPress = awaitLongPressOrCancellation(down.id)
                        if (longPress != null) {
                            actions.onLongPress()
                            trackHomeAppDrag(
                                pointerId = down.id,
                                touchSlop = viewConfiguration.touchSlop,
                                actions = actions,
                            )
                        }
                    }
                }
                .padding(horizontal = 8.dp, vertical = 8.dp)
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
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(visible = state.showActions) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = actions.onRemove,
                        modifier = Modifier.testTag("home_app_remove_${app.label}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.remove_from_home),
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { actions.onToggleMenu(true) },
                            modifier = Modifier.testTag("home_app_more_${app.label}"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.more_options),
                            )
                        }
                        DropdownMenu(
                            expanded = state.showMenu,
                            onDismissRequest = { actions.onToggleMenu(false) },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename)) },
                                onClick = {
                                    actions.onToggleMenu(false)
                                    actions.onRename()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.uninstall)) },
                                onClick = {
                                    actions.onToggleMenu(false)
                                    actions.onUninstall()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.trackHomeAppDrag(
    pointerId: PointerId,
    touchSlop: Float,
    actions: HomeAppRowActions,
) {
    var isDragging = false
    var totalDeltaY = 0f
    var change = awaitPointerEvent().changes.firstOrNull { it.id == pointerId }
    while (change?.pressed == true) {
        val dragAmount = change.positionChange().y
        totalDeltaY += dragAmount
        if (!isDragging && abs(totalDeltaY) > touchSlop) {
            isDragging = true
            actions.onToggleMenu(false)
            actions.onDragStart()
        }
        if (isDragging && dragAmount != 0f) {
            actions.onDrag(dragAmount)
            change.consume()
        }
        change = awaitPointerEvent().changes.firstOrNull { it.id == pointerId }
    }
    if (isDragging) actions.onDragEnd()
}
