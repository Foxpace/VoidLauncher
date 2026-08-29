package com.tomasrepcik.voidlauncher.launcher.root

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.tomasrepcik.voidlauncher.R
import com.tomasrepcik.voidlauncher.launcher.action.LauncherActionExecutor
import com.tomasrepcik.voidlauncher.launcher.action.LauncherActionOutcome
import com.tomasrepcik.voidlauncher.launcher.error.AppError
import com.tomasrepcik.voidlauncher.launcher.error.AppErrorKind
import com.tomasrepcik.voidlauncher.launcher.error.AppErrorMessageMapper
import com.tomasrepcik.voidlauncher.launcher.error.ErrorRecovery
import com.tomasrepcik.voidlauncher.launcher.LauncherRootAction
import kotlinx.coroutines.flow.Flow
import org.koin.compose.koinInject

internal class AndroidLogUnexpectedErrorReporter {
    fun report(error: AppError) {
        Log.e(
            "VoidLauncher",
            "Unexpected failure while ${error.operation.name.lowercase()}",
            error.cause,
        )
    }
}

internal sealed interface HandledRootAction {
    data object Handled : HandledRootAction
    data object CloseScreen : HandledRootAction
    data class ShowMessage(val value: String) : HandledRootAction
}

internal class AndroidLauncherRootActionMessages(
    context: Context,
    private val messageMapper: AppErrorMessageMapper,
) {
    private val applicationContext = context.applicationContext

    fun errorMessage(error: AppError) = messageMapper.message(applicationContext, error)

    fun recoveryMessage(recovery: ErrorRecovery) =
        messageMapper.recoveryMessage(applicationContext, recovery)

    fun appAddedToHomeMessage(appLabel: String) = applicationContext.getString(
        R.string.app_added_to_home_confirmation,
        appLabel,
    )
}

/** Shared native-action and message policy invoked by every feature root. */
internal class LauncherRootActionHandler(
    private val actionExecutor: LauncherActionExecutor,
    private val reportUnexpectedError: (AppError) -> Unit,
    private val errorMessage: (AppError) -> String,
    private val recoveryMessage: (ErrorRecovery) -> String?,
    private val appAddedToHomeMessage: (String) -> String,
) {
    fun handle(action: LauncherRootAction): HandledRootAction = when (action) {
        is LauncherRootAction.Open -> handle(actionExecutor.execute(action.action))
        is LauncherRootAction.ShowError -> action.error.toMessage()
        is LauncherRootAction.ShowMessage -> HandledRootAction.ShowMessage(action.message)
        is LauncherRootAction.ShowAppAddedConfirmation -> HandledRootAction.ShowMessage(
            appAddedToHomeMessage(action.appLabel),
        )
        LauncherRootAction.CloseScreen -> HandledRootAction.CloseScreen
    }

    private fun handle(outcome: LauncherActionOutcome): HandledRootAction = when (outcome) {
        LauncherActionOutcome.Completed -> HandledRootAction.Handled
        is LauncherActionOutcome.Recovered -> recoveryMessage(outcome.recovery)
            ?.let(HandledRootAction::ShowMessage)
            ?: HandledRootAction.Handled
        is LauncherActionOutcome.Failed -> outcome.error.toMessage()
    }

    private fun AppError.toMessage(): HandledRootAction.ShowMessage {
        if (kind == AppErrorKind.UNEXPECTED) reportUnexpectedError(this)
        return HandledRootAction.ShowMessage(errorMessage(this))
    }
}

@Composable
internal fun HandleRootActions(
    actions: Flow<LauncherRootAction>,
    snackbarHostState: SnackbarHostState,
    onCloseScreen: () -> Unit = {},
    handler: LauncherRootActionHandler = koinInject(),
) {
    val currentOnCloseScreen = rememberUpdatedState(onCloseScreen)

    LaunchedEffect(actions, handler) {
        actions.collect { action ->
            when (val result = handler.handle(action)) {
                HandledRootAction.Handled -> Unit
                HandledRootAction.CloseScreen -> currentOnCloseScreen.value()
                is HandledRootAction.ShowMessage -> snackbarHostState.showSnackbar(result.value)
            }
        }
    }
}
