package com.tomasrepcik.voidlauncher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleEditorIntent
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleEditorScreen
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleEditorUiState
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleListIntent
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleListScreen
import com.tomasrepcik.voidlauncher.ui.schedule.ScheduleListUiState
import com.tomasrepcik.voidlauncher.ui.theme.VoidLauncherTheme
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
    fun emptyListExplainsFallbackAndOffersOneAddAction() {
        var addRequests = 0
        composeRule.setContent {
            VoidLauncherTheme {
                ScheduleListScreen(
                    state = ScheduleListUiState(isLoading = false),
                    onBack = {},
                    onAdd = { addRequests += 1 },
                    onEdit = {},
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText("Your regular Home apps stay visible whenever no schedule is active.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_add_button").performClick()
        assertEquals(1, addRequests)
    }

    @Test
    fun alarmCardCanBeDisabledWithoutOpeningTheEditor() {
        val schedule = schedule()
        var receivedIntent: ScheduleListIntent? = null
        composeRule.setContent {
            VoidLauncherTheme {
                ScheduleListScreen(
                    state = ScheduleListUiState(listOf(schedule), isLoading = false),
                    onBack = {},
                    onAdd = {},
                    onEdit = {},
                    onIntent = { receivedIntent = it },
                )
            }
        }

        composeRule.onNodeWithText("Work").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_enabled_work").performClick()

        when (val intent = receivedIntent) {
            is ScheduleListIntent.SetEnabled -> {
                assertEquals(schedule, intent.schedule)
                assertFalse(intent.enabled)
            }
            else -> fail("Expected a SetEnabled intent, but received $intent")
        }
    }

    @Test
    fun editorShowsSelectedAppsAndOpensPickerOnASeparateScreen() {
        val mail = app("Mail")
        val calendar = app("Calendar")
        var receivedIntent: ScheduleEditorIntent? = null
        composeRule.setContent {
            VoidLauncherTheme {
                ScheduleEditorScreen(
                    state = ScheduleEditorUiState(
                        selectedAppKeys = setOf(mail.key, calendar.key),
                        installedApps = listOf(mail, calendar),
                        isLoading = false,
                    ),
                    onBack = {},
                    onIntent = { receivedIntent = it },
                )
            }
        }

        composeRule.onNodeWithText("Weekdays").assertIsDisplayed()
        DayOfWeek.entries.forEach { day ->
            composeRule.onNodeWithTag("schedule_day_${day.name}").assertIsDisplayed()
        }
        val enabledControls = composeRule.onAllNodesWithText("Use this schedule")
        assertEquals(0, enabledControls.fetchSemanticsNodes().size)
        val appSearchFields = composeRule.onAllNodesWithTag("schedule_app_search")
        assertEquals(0, appSearchFields.fetchSemanticsNodes().size)
        listOf(mail, calendar).forEach { app ->
            val tag = "schedule_selected_app_${app.key.packageName}_${app.key.activityName}"
            composeRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithTag("schedule_save_button").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_choose_apps_button").performScrollTo().performClick()
        assertEquals(ScheduleEditorIntent.OpenAppPicker, receivedIntent)

        composeRule.onNodeWithTag("schedule_preset_Every day").performClick()

        when (val intent = receivedIntent) {
            is ScheduleEditorIntent.DaysChanged ->
                assertEquals(DayOfWeek.entries.toSet(), intent.days)
            else -> fail("Expected a DaysChanged intent, but received $intent")
        }

        composeRule.setContent {
            VoidLauncherTheme {
                ScheduleEditorScreen(
                    state = ScheduleEditorUiState(
                        selectedAppKeys = setOf(mail.key, calendar.key),
                        installedApps = listOf(mail, calendar),
                        isAppPickerOpen = true,
                        isLoading = false,
                    ),
                    onBack = {},
                    onIntent = { receivedIntent = it },
                )
            }
        }

        composeRule.onNodeWithText("Choose apps").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_app_search").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_app_picker_done").performClick()
        assertEquals(ScheduleEditorIntent.CloseAppPicker, receivedIntent)
    }

    private fun schedule() = AppSchedule(
        id = "work",
        name = "Work",
        days = setOf(DayOfWeek.MONDAY),
        startMinute = 9 * 60,
        endMinute = 17 * 60,
        appKeys = setOf(app("Mail").key),
    )

    private fun app(label: String) = InstalledApp(
        key = AppKey("pkg.${label.lowercase()}", "Activity$label"),
        label = label,
        sortLabel = label.lowercase(),
    )
}
