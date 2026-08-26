package com.tomasrepcik.voidlauncher.ui.navigation

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.tomasrepcik.voidlauncher.domain.action.LauncherActionExecutor
import com.tomasrepcik.voidlauncher.domain.action.LauncherActionOutcome
import com.tomasrepcik.voidlauncher.domain.error.AppError
import com.tomasrepcik.voidlauncher.domain.error.AppErrorKind
import com.tomasrepcik.voidlauncher.domain.error.AppErrorMessageMapper
import com.tomasrepcik.voidlauncher.domain.error.ErrorRecovery
import com.tomasrepcik.voidlauncher.ui.LauncherRootAction
import kotlinx.coroutines.flow.Flow
import org.koin.compose.koinInject

fun interface UnexpectedErrorReporter {
    fun report(error: AppError)
}

internal class AndroidLogUnexpectedErrorReporter : UnexpectedErrorReporter {
    override fun report(error: AppError) {
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

internal interface LauncherRootActionMessages {
    fun errorMessage(error: AppError): String
    fun recoveryMessage(recovery: ErrorRecovery): String?
}

internal class AndroidLauncherRootActionMessages(
    context: Context,
    private val messageMapper: AppErrorMessageMapper,
) : LauncherRootActionMessages {
    private val applicationContext = context.applicationContext

    override fun errorMessage(error: AppError) = messageMapper.message(applicationContext, error)

    override fun recoveryMessage(recovery: ErrorRecovery) =
        messageMapper.recoveryMessage(applicationContext, recovery)
}

/** Shared native-action and message policy invoked by every feature root. */
internal class LauncherRootActionHandler(
    private val actionExecutor: LauncherActionExecutor,
    private val unexpectedErrorReporter: UnexpectedErrorReporter,
    private val messages: LauncherRootActionMessages,
) {
    fun handle(action: LauncherRootAction): HandledRootAction = when (action) {
        is LauncherRootAction.Open -> handle(actionExecutor.execute(action.action))
        is LauncherRootAction.ShowError -> action.error.toMessage()
        is LauncherRootAction.ShowMessage -> HandledRootAction.ShowMessage(action.message)
        LauncherRootAction.CloseScreen -> HandledRootAction.CloseScreen
    }

    private fun handle(outcome: LauncherActionOutcome): HandledRootAction = when (outcome) {
        LauncherActionOutcome.Completed -> HandledRootAction.Handled
        is LauncherActionOutcome.Recovered -> messages.recoveryMessage(outcome.recovery)
            ?.let(HandledRootAction::ShowMessage)
            ?: HandledRootAction.Handled
        is LauncherActionOutcome.Failed -> outcome.error.toMessage()
    }

    private fun AppError.toMessage(): HandledRootAction.ShowMessage {
        if (kind == AppErrorKind.UNEXPECTED) unexpectedErrorReporter.report(this)
        return HandledRootAction.ShowMessage(messages.errorMessage(this))
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
