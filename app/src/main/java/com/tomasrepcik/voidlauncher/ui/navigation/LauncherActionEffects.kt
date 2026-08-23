package com.tomasrepcik.voidlauncher.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.tomasrepcik.voidlauncher.domain.action.LauncherActionExecutor
import com.tomasrepcik.voidlauncher.domain.action.LauncherActionOutcome
import com.tomasrepcik.voidlauncher.domain.error.AppErrorMessageMapper
import com.tomasrepcik.voidlauncher.ui.LauncherUiEffect
import kotlinx.coroutines.flow.Flow

@Composable
internal fun LauncherEffectHost(
    effects: Flow<LauncherUiEffect>,
    snackbarHostState: SnackbarHostState,
    onCompleted: () -> Unit = {},
) {
    val context = LocalContext.current
    val executor = remember { LauncherActionExecutor() }
    val messageMapper = remember { AppErrorMessageMapper() }
    val currentOnCompleted = rememberUpdatedState(onCompleted)

    LaunchedEffect(effects, context, executor) {
        effects.collect { effect ->
            when (effect) {
                is LauncherUiEffect.Action -> {
                    when (val outcome = executor.execute(context, effect.action)) {
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
                is LauncherUiEffect.Error -> snackbarHostState.showSnackbar(
                    messageMapper.message(context, effect.error),
                )
                is LauncherUiEffect.Feedback -> snackbarHostState.showSnackbar(effect.message)
                LauncherUiEffect.Completed -> currentOnCompleted.value()
            }
        }
    }
}
