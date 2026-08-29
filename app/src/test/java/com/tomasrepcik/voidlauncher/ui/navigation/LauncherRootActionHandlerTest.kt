package com.tomasrepcik.voidlauncher.ui.navigation

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.domain.action.LauncherActionExecutor
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
