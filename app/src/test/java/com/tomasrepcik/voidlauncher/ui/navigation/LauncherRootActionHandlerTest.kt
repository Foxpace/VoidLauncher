package com.tomasrepcik.voidlauncher.ui.navigation

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.domain.action.LauncherActionExecutor
import com.tomasrepcik.voidlauncher.domain.action.AppLauncher
import com.tomasrepcik.voidlauncher.domain.error.AppError
import com.tomasrepcik.voidlauncher.domain.error.AppErrorKind
import com.tomasrepcik.voidlauncher.domain.error.AppOperation
import com.tomasrepcik.voidlauncher.domain.error.ErrorRecovery
import com.tomasrepcik.voidlauncher.ui.LauncherRootAction
import org.junit.Test

class LauncherRootActionHandlerTest {
    @Test
    fun givenUnexpectedError_whenHandled_thenCauseIsReportedAndSafeMessageIsReturned() {
        // GIVEN
        val cause = IllegalStateException("private diagnostic")
        val error = AppError(
            kind = AppErrorKind.UNEXPECTED,
            operation = AppOperation.READ_STORAGE,
            recovery = ErrorRecovery.NONE,
            cause = cause,
        )
        val reported = mutableListOf<AppError>()
        val handler = LauncherRootActionHandler(
            actionExecutor = LauncherActionExecutor(NoOpAppLauncher),
            unexpectedErrorReporter = UnexpectedErrorReporter(reported::add),
            messages = object : LauncherRootActionMessages {
                override fun errorMessage(error: AppError) = "Something went wrong"
                override fun recoveryMessage(recovery: ErrorRecovery): String? = null
            },
        )

        // WHEN
        val result = handler.handle(LauncherRootAction.ShowError(error))

        // THEN
        assertThat(reported).containsExactly(error)
        assertThat(result).isEqualTo(HandledRootAction.ShowMessage("Something went wrong"))
    }

    private data object NoOpAppLauncher : AppLauncher {
        override fun open(intent: Intent) = true
        override fun installedApplicationFlags(packageName: String) = 0
    }
}
