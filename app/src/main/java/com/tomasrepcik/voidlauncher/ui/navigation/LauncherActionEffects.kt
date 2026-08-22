package com.tomasrepcik.voidlauncher.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tomasrepcik.voidlauncher.domain.action.LauncherAction
import com.tomasrepcik.voidlauncher.domain.action.LauncherActionExecutor
import com.tomasrepcik.voidlauncher.domain.action.LauncherActionOutcome
import kotlinx.coroutines.flow.Flow

@Composable
fun CollectLauncherActions(
    actions: Flow<LauncherAction>,
    snackbarHostState: SnackbarHostState,
    feedback: Flow<String>? = null,
) {
    val context = LocalContext.current
    val executor = remember(context) { LauncherActionExecutor(context.applicationContext) }

    LaunchedEffect(actions, executor) {
        actions.collect { action ->
            when (val outcome = executor.execute(action)) {
                LauncherActionOutcome.Completed -> Unit
                is LauncherActionOutcome.Recovered -> {
                    outcome.message?.let { snackbarHostState.showSnackbar(it) }
                }
                is LauncherActionOutcome.Failed -> snackbarHostState.showSnackbar(outcome.message)
            }
        }
    }

    LaunchedEffect(feedback) {
        feedback?.collect { message -> snackbarHostState.showSnackbar(message) }
    }
}
