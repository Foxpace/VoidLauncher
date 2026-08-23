package com.tomasrepcik.voidlauncher.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tomasrepcik.voidlauncher.domain.action.LauncherAction
import com.tomasrepcik.voidlauncher.domain.action.LauncherActionExecutor
import com.tomasrepcik.voidlauncher.domain.action.LauncherActionOutcome
import com.tomasrepcik.voidlauncher.domain.error.AppErrorMessageMapper
import kotlinx.coroutines.flow.Flow

@Composable
fun CollectLauncherActions(
    actions: Flow<LauncherAction>,
    snackbarHostState: SnackbarHostState,
    feedback: Flow<String>? = null,
) {
    val context = LocalContext.current
    val executor = remember { LauncherActionExecutor() }
    val messageMapper = remember { AppErrorMessageMapper() }

    LaunchedEffect(actions, context, executor) {
        actions.collect { action ->
            when (val outcome = executor.execute(context, action)) {
                LauncherActionOutcome.Completed -> Unit
                is LauncherActionOutcome.Recovered -> {
                    messageMapper.recoveryMessage(context, outcome.recovery)?.let { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }
                is LauncherActionOutcome.Failed -> snackbarHostState.showSnackbar(
                    messageMapper.message(context, outcome.error),
                )
            }
        }
    }

    LaunchedEffect(feedback) {
        feedback?.collect { message -> snackbarHostState.showSnackbar(message) }
    }
}
