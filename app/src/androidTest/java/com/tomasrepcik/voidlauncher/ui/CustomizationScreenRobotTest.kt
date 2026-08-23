package com.tomasrepcik.voidlauncher.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationScreen
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationActions
import com.tomasrepcik.voidlauncher.ui.customization.CustomizationUiState
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceActions
import com.tomasrepcik.voidlauncher.ui.home.appearance.HomeAppearanceState
import com.tomasrepcik.voidlauncher.ui.theme.VoidLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CustomizationScreenRobotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun givenCustomizationScreen_whenSettingsActionsAreUsed_thenNavigationAndLicensesRemainAvailable() {
        // GIVEN
        var backRequests = 0
        var scheduleRequests = 0
        var tutorialRequests = 0
        composeRule.setContent {
            VoidLauncherTheme {
                CustomizationScreen(
                    state = CustomizationUiState(),
                    appearance = HomeAppearanceState(),
                    appearanceActions = HomeAppearanceActions(),
                    actions = CustomizationActions(
                        onBack = { backRequests += 1 },
                        onEditShortcut = {},
                        onOpenSchedules = { scheduleRequests += 1 },
                        onShowNavigationTutorial = { tutorialRequests += 1 },
                    ),
                )
            }
        }

        // WHEN
        composeRule.onNodeWithTag("customization_back_button")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag("open_schedules_button")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithTag("show_navigation_tutorial_button")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithTag("app_version")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("1.0 (1)")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("open_source_licenses_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithTag("open_source_licenses_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("VoidLauncher").assertIsDisplayed()
        composeRule.onNodeWithText("MIT License").assertIsDisplayed()
        composeRule.onNodeWithText("AndroidX").assertIsDisplayed()
        composeRule.onAllNodesWithText("Apache License 2.0").assertCountEquals(3)
        composeRule.onNodeWithText("Close").performClick()

        // THEN
        assertEquals(1, backRequests)
        assertEquals(1, scheduleRequests)
        assertEquals(1, tutorialRequests)
    }

    @Test
    fun givenCustomBackground_whenColorsAreEnabledAndDefaultIsRestored_thenAppearanceChangesAreRequested() {
        // GIVEN
        var backgroundUri: String? = "unchanged"
        var useBackgroundColors = false
        composeRule.setContent {
            VoidLauncherTheme {
                CustomizationScreen(
                    state = CustomizationUiState(),
                    appearance = HomeAppearanceState(
                        backgroundUri = "content://images/background",
                    ),
                    appearanceActions = HomeAppearanceActions(
                        onRestoreDefault = { backgroundUri = null },
                        onUseBackgroundColorsChange = { useBackgroundColors = it },
                    ),
                    actions = CustomizationActions(
                        onBack = {},
                        onEditShortcut = {},
                        onOpenSchedules = {},
                        onShowNavigationTutorial = {},
                    ),
                )
            }
        }

        // WHEN
        composeRule.onNodeWithTag("use_background_colors_switch")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeRule.onNodeWithTag("restore_default_background_button")
            .performScrollTo()
            .performClick()

        // THEN
        assertEquals(null, backgroundUri)
        assertEquals(true, useBackgroundColors)
    }
}
