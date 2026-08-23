package com.tomasrepcik.voidlauncher.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tomasrepcik.voidlauncher.ui.onboarding.NavigationTutorial
import com.tomasrepcik.voidlauncher.ui.theme.VoidLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val ANIMATION_MIDPOINT_MILLIS = 120L

class NavigationTutorialTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tutorialExplainsNavigationShortcutsAndScheduleLocationBeforeFinishing() {
        var finishRequests = 0
        composeRule.setContent {
            VoidLauncherTheme {
                NavigationTutorial(onFinish = { finishRequests += 1 })
            }
        }

        composeRule.onNodeWithTag("navigation_tutorial_title")
            .assertTextEquals("Move around with swipes")
        composeRule.onNodeWithText("On Home, swipe left to open all apps.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Swipe right to go back.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Swipe up from the bottom edge to focus Search.")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("navigation_tutorial_next").performClick()
        composeRule.onNodeWithTag("navigation_tutorial_title")
            .assertTextEquals("Shortcuts stay at the bottom")
        composeRule.onNodeWithText("Tap either bottom shortcut to open it.")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("navigation_tutorial_next").performClick()
        composeRule.onNodeWithTag("navigation_tutorial_title")
            .assertTextEquals("Schedules are in Settings")
        composeRule.onNodeWithText("Swipe left → Settings → App schedules")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("navigation_tutorial_next").performClick()
        composeRule.runOnIdle { assertEquals(1, finishRequests) }
    }

    @Test
    fun changingPageKeepsOneDialogWhileOldAndNewContentAnimateTogether() {
        composeRule.setContent {
            VoidLauncherTheme {
                NavigationTutorial(onFinish = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithTag("navigation_tutorial_next").performClick()
        composeRule.mainClock.advanceTimeBy(ANIMATION_MIDPOINT_MILLIS)

        composeRule.onAllNodesWithTag("navigation_tutorial").assertCountEquals(1)
        composeRule.onAllNodesWithTag("navigation_tutorial_title").assertCountEquals(2)

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("navigation_tutorial_title").assertCountEquals(1)
        composeRule.onNodeWithTag("navigation_tutorial_title")
            .assertTextEquals("Shortcuts stay at the bottom")
    }
}
