package com.tomasrepcik.voidlauncher.schedule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.tomasrepcik.voidlauncher.testing.appSchedule
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.schedule.editor.ScheduleEditorAction
import com.tomasrepcik.voidlauncher.schedule.editor.ScheduleEditorScreen
import com.tomasrepcik.voidlauncher.schedule.editor.ScheduleEditorUiState
import com.tomasrepcik.voidlauncher.schedule.list.ScheduleListAction
import com.tomasrepcik.voidlauncher.schedule.list.ScheduleListScreen
import com.tomasrepcik.voidlauncher.schedule.list.ScheduleListUiState
import com.tomasrepcik.voidlauncher.design.theme.VoidLauncherTheme
import java.time.DayOfWeek
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail

class ScheduleScreenRobotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenEmptyScheduleList_whenAddActionIsUsed_thenFallbackIsExplainedAndAddIsRequested() {
        // GIVEN
        var addRequests = 0

        // WHEN
        composeRule.setContent {
            VoidLauncherTheme {
                ScheduleListScreen(
                    state = ScheduleListUiState(isLoading = false),
                    onBack = {},
                    onAdd = { addRequests += 1 },
                    onEdit = {},
                    onAction = {},
                )
            }
        }

        // THEN
        composeRule.onNodeWithTag("schedule_empty_state").assertIsDisplayed()
        composeRule.onNodeWithText("No app schedules yet").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Create one to choose which apps appear on Home by weekday and time. " +
                "Your regular Home apps stay visible until a schedule is active.",
        )
            .assertIsDisplayed()

        // WHEN
        composeRule.onNodeWithTag("schedule_add_button").performClick()

        // THEN
        assertEquals(1, addRequests)
    }

    @Test
    fun givenEnabledScheduleCard_whenToggleIsClicked_thenDisableIntentIsSentWithoutOpeningEditor() {
        // GIVEN
        val schedule = appSchedule(apps = listOf(installedApp("Mail")))
        var receivedAction: ScheduleListAction? = null

        // WHEN
        composeRule.setContent {
            VoidLauncherTheme {
                ScheduleListScreen(
                    state = ScheduleListUiState(listOf(schedule), isLoading = false),
                    onBack = {},
                    onAdd = {},
                    onEdit = {},
                    onAction = { receivedAction = it },
                )
            }
        }

        // THEN
        composeRule.onNodeWithText("Work").assertIsDisplayed()

        // WHEN
        composeRule.onNodeWithTag("schedule_enabled_work").performClick()

        // THEN
        when (val action = receivedAction) {
            is ScheduleListAction.SetScheduleEnabled -> {
                assertEquals(schedule, action.schedule)
                assertFalse(action.enabled)
            }
            else -> fail("Expected SetScheduleEnabled, but received $action")
        }
    }

    @Test
    fun givenScheduleEditor_whenAppPickerAndPresetAreUsed_thenSelectedAppsAndIntentsAreShown() {
        // GIVEN
        val mail = installedApp("Mail")
        val calendar = installedApp("Calendar")
        var receivedAction: ScheduleEditorAction? = null
        var editorState by mutableStateOf(
            ScheduleEditorUiState(
                selectedAppKeys = setOf(mail.key, calendar.key),
                installedApps = listOf(mail, calendar),
                isLoading = false,
            ),
        )

        // WHEN
        composeRule.setContent {
            VoidLauncherTheme {
                ScheduleEditorScreen(
                    state = editorState,
                    onBack = {},
                    onAction = { receivedAction = it },
                )
            }
        }

        // THEN
        composeRule.onNodeWithText("Weekdays").assertIsDisplayed()
        DayOfWeek.entries.forEach { day ->
            composeRule.onNodeWithTag("schedule_day_${day.name}").assertIsDisplayed()
        }
        composeRule.onNodeWithTag("schedule_start_time_button").assertHasClickAction()
        composeRule.onNodeWithTag("schedule_end_time_button").assertHasClickAction()
        val enabledControls = composeRule.onAllNodesWithText("Use this schedule")
        assertEquals(0, enabledControls.fetchSemanticsNodes().size)
        val appSearchFields = composeRule.onAllNodesWithTag("schedule_app_search")
        assertEquals(0, appSearchFields.fetchSemanticsNodes().size)
        listOf(mail, calendar).forEach { app ->
            val tag = "schedule_selected_app_${app.key.packageName}_${app.key.activityName}"
            composeRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithTag("schedule_save_button").assertIsDisplayed()

        // WHEN
        composeRule.onNodeWithTag("schedule_choose_apps_button").performScrollTo().performClick()

        // THEN
        assertEquals(ScheduleEditorAction.OpenAppPicker, receivedAction)

        // WHEN
        composeRule.onNodeWithTag("schedule_preset_Every day").performClick()

        // THEN
        when (val action = receivedAction) {
            is ScheduleEditorAction.ChangeDays ->
                assertEquals(DayOfWeek.entries.toSet(), action.days)
            else -> fail("Expected ChangeDays, but received $action")
        }

        // WHEN
        composeRule.runOnIdle {
            editorState = editorState.copy(isAppPickerOpen = true)
        }

        // THEN
        composeRule.onNodeWithText("Choose apps").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_app_search").assertIsDisplayed()

        // WHEN
        composeRule.onNodeWithTag("schedule_app_picker_done").performClick()

        // THEN
        assertEquals(ScheduleEditorAction.CloseAppPicker, receivedAction)
    }
}
