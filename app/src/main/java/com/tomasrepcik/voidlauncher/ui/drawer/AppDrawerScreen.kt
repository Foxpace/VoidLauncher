package com.tomasrepcik.voidlauncher.ui.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.ui.components.LauncherSearchField
import com.tomasrepcik.voidlauncher.ui.components.LauncherSearchOptions
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationContainer
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationActions

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

        DrawerAppBrowser(
            state = state,
            actions = actions,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
