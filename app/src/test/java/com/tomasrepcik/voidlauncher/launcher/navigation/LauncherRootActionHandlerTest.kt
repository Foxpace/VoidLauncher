package com.tomasrepcik.voidlauncher.launcher.navigation

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.launcher.action.LauncherActionExecutor
import com.tomasrepcik.voidlauncher.launcher.error.AppError
import com.tomasrepcik.voidlauncher.launcher.error.AppErrorKind
import com.tomasrepcik.voidlauncher.launcher.error.AppOperation
import com.tomasrepcik.voidlauncher.launcher.error.ErrorRecovery
import com.tomasrepcik.voidlauncher.launcher.LauncherRootAction
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
            actionExecutor = LauncherActionExecutor(
                openApp = { true },
                installedApplicationFlags = { 0 },
            ),
            reportUnexpectedError = reported::add,
            errorMessage = { "Something went wrong" },
            recoveryMessage = { null },
            appAddedToHomeMessage = { "Done" },
        )

        // WHEN
        val result = handler.handle(LauncherRootAction.ShowError(error))

        // THEN
        assertThat(reported).containsExactly(error)
        assertThat(result).isEqualTo(HandledRootAction.ShowMessage("Something went wrong"))
    }
}
