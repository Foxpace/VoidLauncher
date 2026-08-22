package com.tomasrepcik.voidlauncher.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.domain.error.AppErrorMessageMapper

@Composable
fun LauncherRepositoryBlocker(
    state: LauncherRepositoryState,
    onRetry: () -> Unit,
) {
    when (state) {
        is LauncherRepositoryState.Ready -> Unit
        LauncherRepositoryState.Loading -> Surface(modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is LauncherRepositoryState.InitializationError -> {
            val context = LocalContext.current
            val mapper = remember(context) { AppErrorMessageMapper(context) }
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = mapper.message(state.error),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = onRetry) {
                        Text(text = stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}

@Composable
fun CollectRepositoryMutationErrors(
    state: LauncherRepositoryState,
    snackbarHostState: SnackbarHostState,
) {
    val error = (state as? LauncherRepositoryState.Ready)?.mutationError
    val context = LocalContext.current
    val mapper = remember(context) { AppErrorMessageMapper(context) }
    LaunchedEffect(error) {
        if (error != null) snackbarHostState.showSnackbar(mapper.message(error))
    }
}
