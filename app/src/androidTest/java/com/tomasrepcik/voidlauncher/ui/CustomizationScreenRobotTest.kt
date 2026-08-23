package com.tomasrepcik.voidlauncher.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationScreen
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationUiState
import com.tomasrepcik.voidlauncher.ui.theme.VoidLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CustomizationScreenRobotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsExposeSchedulingAsAButtonAndKeepBackNavigationClickable() {
        var backRequests = 0
        var scheduleRequests = 0
        composeRule.setContent {
            VoidLauncherTheme {
                CustomizationScreen(
                    state = CustomizationUiState(),
                    onBack = { backRequests += 1 },
                    onEditShortcut = {},
                    onOpenSchedules = { scheduleRequests += 1 },
                )
            }
        }

        composeRule.onNodeWithTag("customization_back_button")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag("open_schedules_button")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertEquals(1, backRequests)
        assertEquals(1, scheduleRequests)
    }
}
