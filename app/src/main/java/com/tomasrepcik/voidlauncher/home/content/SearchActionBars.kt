package com.tomasrepcik.voidlauncher.home.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.home.HomeActions
import com.tomasrepcik.voidlauncher.home.SearchOverlayActions

@Composable
internal fun BoxScope.KeyboardSearchActions(
    query: String,
    actions: HomeActions,
) {
    AnimatedVisibility(
        visible = query.isNotBlank(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .imePadding()
            .padding(bottom = 8.dp)
            .zIndex(2f),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
        ) {
            SearchActionButtons(
                actions = SearchOverlayActions(
                    onSuggestionClicked = {},
                    onPlayStoreSearch = actions.onPlayStoreSearch,
                    onMapsSearch = actions.onMapsSearch,
                    onBrowserSearch = actions.onBrowserSearch,
                ),
                testTagPrefix = "home_keyboard",
            )
        }
    }
}

@Composable
internal fun SearchActionButtons(
    actions: SearchOverlayActions,
    testTagPrefix: String,
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SearchActionButton(
            onClick = actions.onPlayStoreSearch,
            testTag = "${testTagPrefix}_play_store_button",
            icon = { Icon(Icons.Filled.Storefront, stringResource(R.string.open_google_play)) },
        )
        SearchActionButton(
            onClick = actions.onMapsSearch,
            testTag = "${testTagPrefix}_maps_button",
            icon = { Icon(Icons.Filled.Map, stringResource(R.string.open_google_maps)) },
        )
        SearchActionButton(
            onClick = actions.onBrowserSearch,
            testTag = "${testTagPrefix}_browser_button",
            icon = { Icon(Icons.Outlined.Language, stringResource(R.string.open_browser_search)) },
        )
    }
}

@Composable
private fun SearchActionButton(
    onClick: () -> Unit,
    testTag: String,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag(testTag),
        content = icon,
    )
}
