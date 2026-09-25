package com.tomasrepcik.voidlauncher.home.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tomasrepcik.voidlauncher.appcatalog.search.SearchTarget

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun BoxScope.KeyboardSearchActions(
    query: String,
    actions: HomeActions,
) {
    AnimatedVisibility(
        visible = keyboardSearchActionsAreVisible(query, WindowInsets.isImeVisible),
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
                onSearch = actions.onSearch,
                testTagPrefix = "home_keyboard",
            )
        }
    }
}

internal fun keyboardSearchActionsAreVisible(
    query: String,
    isKeyboardVisible: Boolean,
) = query.isNotBlank() && isKeyboardVisible

@Composable
internal fun SearchActionButtons(
    onSearch: (SearchTarget) -> Unit,
    testTagPrefix: String,
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SearchActionButton(
            onClick = { onSearch(SearchTarget.PlayStore) },
            testTag = "${testTagPrefix}_play_store_button",
            icon = { Icon(Icons.Filled.Storefront, stringResource(R.string.open_google_play)) },
        )
        SearchActionButton(
            onClick = { onSearch(SearchTarget.Maps) },
            testTag = "${testTagPrefix}_maps_button",
            icon = { Icon(Icons.Filled.Map, stringResource(R.string.open_google_maps)) },
        )
        AssistantSearchMenu(onSearch, testTagPrefix)
        SearchActionButton(
            onClick = { onSearch(SearchTarget.Browser) },
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

@Composable
private fun AssistantSearchMenu(onSearch: (SearchTarget) -> Unit, testTagPrefix: String) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("${testTagPrefix}_assistant_button"),
        ) {
            Text(stringResource(R.string.ask_ai))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val assistants = listOf(
                SearchTarget.ChatGpt to R.string.chatgpt,
                SearchTarget.Claude to R.string.claude,
                SearchTarget.Gemini to R.string.gemini,
            )
            assistants.forEach { (target, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    modifier = Modifier.testTag("${testTagPrefix}_assistant_$target"),
                    onClick = {
                        expanded = false
                        onSearch(target)
                    },
                )
            }
        }
    }
}
