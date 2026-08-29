package com.tomasrepcik.voidlauncher.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.tomasrepcik.voidlauncher.domain.action.LauncherActionExecutor
import com.tomasrepcik.voidlauncher.ui.LauncherRootAction
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LauncherRootActionHandlerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenRootActionStream_whenMessageIsEmitted_thenSnackbarShowsFeedback() {
        // GIVEN
        val actions = MutableSharedFlow<LauncherRootAction>(extraBufferCapacity = 1)
        val snackbar = SnackbarHostState()
        val handler = LauncherRootActionHandler(
            actionExecutor = LauncherActionExecutor(
                openApp = { true },
                installedApplicationFlags = { 0 },
            ),
            reportUnexpectedError = {},
            errorMessage = { "Something went wrong" },
            recoveryMessage = { null },
            appAddedToHomeMessage = { "Done" },
        )

        composeRule.setContent {
            HandleRootActions(
                actions = actions,
                snackbarHostState = snackbar,
                handler = handler,
            )
        }
        composeRule.waitForIdle()

        // WHEN
        composeRule.runOnIdle {
            assertTrue(actions.tryEmit(LauncherRootAction.ShowMessage("Saved")))
        }
        composeRule.waitUntil {
            snackbar.currentSnackbarData?.visuals?.message == "Saved"
        }

        // THEN
        assertEquals("Saved", snackbar.currentSnackbarData?.visuals?.message)
    }
}
