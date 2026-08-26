package com.tomasrepcik.voidlauncher.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationActions
import com.tomasrepcik.voidlauncher.ui.components.SwipeNavigationContainer

@Composable
internal fun ScheduleScaffold(
    onBack: () -> Unit,
    topContent: @Composable () -> Unit,
    bottomContent: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        SwipeNavigationContainer(
            modifier = Modifier.fillMaxSize(),
            actions = SwipeNavigationActions(onClose = onBack),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                topContent()
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentPadding = PaddingValues(
                            bottom = if (bottomContent == null) 12.dp else 88.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content,
                    )
                    bottomContent?.let { fixedContent ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                        ) {
                            fixedContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ScheduleHeader(
    title: String,
    onBack: () -> Unit,
    onAdd: (() -> Unit)? = null,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.weight(1f),
        )
        onAdd?.let { add ->
            IconButton(onClick = add) {
                Icon(Icons.Outlined.Add, stringResource(R.string.new_schedule))
            }
        }
    }
}
