package com.tomasrepcik.voidlauncher.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot
import com.tomasrepcik.voidlauncher.testing.installedApp
import com.tomasrepcik.voidlauncher.testing.resolvedShortcut
import com.tomasrepcik.voidlauncher.drawer.AppDrawerScreen
import com.tomasrepcik.voidlauncher.drawer.AppDrawerActions
import com.tomasrepcik.voidlauncher.drawer.DrawerUiState
import com.tomasrepcik.voidlauncher.home.HomeScreen
import com.tomasrepcik.voidlauncher.home.HomeActions
import com.tomasrepcik.voidlauncher.home.HomeUiState
import com.tomasrepcik.voidlauncher.appearance.HomeAppearanceState
import com.tomasrepcik.voidlauncher.design.theme.VoidLauncherTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class HomeScreenRobotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenHomeScreen_whenSearchingAndOpeningBrowser_thenPinnedAppsAndSearchActionsAreDisplayed() {
        // GIVEN
        val robot = HomeRobot(composeRule)
        robot.launch()

        // WHEN
        robot.enterSearch("spotify")
        robot.tapBrowser()

        // THEN
        robot.assertHomeVisible()
        robot.assertSearchActionsVisible()
        assertEquals(1, robot.browserSearchRequests)
    }

    @Test
    fun givenAppDrawer_whenFilteringApps_thenSearchAndMatchingAppAreDisplayed() {
        // GIVEN
        val robot = DrawerRobot(composeRule)
        robot.launch()

        // WHEN
        robot.filter("cam")

        // THEN
        robot.assertDrawerVisible()
    }

    @Test
    fun givenHomeWithoutApps_whenEmptyStateActionsAreTapped_thenNavigationIsRequested() {
        // GIVEN
        val robot = HomeRobot(composeRule)
        robot.launch(homeApps = emptyList())

        // WHEN
        robot.assertEmptyStateVisible()
        robot.tapAddApp()
        robot.tapEditSchedules()

        // THEN
        assertEquals(1, robot.drawerOpenRequests)
        assertEquals(1, robot.scheduleOpenRequests)
    }

    @Test
    fun givenActiveScheduleWithoutApps_whenEmptyStateIsShown_thenScheduleMessageIsDisplayed() {
        // GIVEN
        val robot = HomeRobot(composeRule)

        // WHEN
        robot.launch(homeApps = emptyList(), isScheduleActive = true)

        // THEN
        robot.assertScheduledEmptyStateVisible()
    }

    @Test
    fun givenPinnedHomeApp_whenAppIsTapped_thenAppOpenIsRequested() {
        // GIVEN
        val robot = HomeRobot(composeRule)
        robot.launch()

        // WHEN
        robot.tapHomeApp("Signal")

        // THEN
        assertEquals("Signal", robot.openedAppLabel)
    }

    @Test
    fun givenBottomShortcut_whenShortcutIsTapped_thenShortcutOpenIsRequested() {
        // GIVEN
        val robot = HomeRobot(composeRule)
        robot.launch()

        // WHEN
        robot.tapLeftShortcut()

        // THEN
        assertEquals(ShortcutSlot.LEFT, robot.openedShortcutSlot)
    }

    @Test
    fun givenAppDrawer_whenAppIsTapped_thenAppOpenIsRequested() {
        // GIVEN
        val robot = DrawerRobot(composeRule)
        robot.launch()

        // WHEN
        robot.tapApp("Camera")

        // THEN
        assertEquals("Camera", robot.openedAppLabel)
    }
}

private class HomeRobot(
    private val composeRule: androidx.compose.ui.test.junit4.ComposeContentTestRule,
) {
    var drawerOpenRequests = 0
        private set
    var scheduleOpenRequests = 0
        private set
    var browserSearchRequests = 0
        private set
    var openedAppLabel: String? = null
        private set
    var openedShortcutSlot: ShortcutSlot? = null
        private set

    fun launch(
        homeApps: List<InstalledApp> = listOf(installedApp("Signal"), installedApp("Spotify")),
        isScheduleActive: Boolean = false,
    ) {
        composeRule.setContent {
            VoidLauncherTheme {
                var query by remember { mutableStateOf("") }
                HomeScreen(
                    state = HomeUiState(
                        query = query,
                        homeApps = homeApps,
                        isScheduleActive = isScheduleActive,
                        isLoading = false,
                        shortcuts = listOf(
                            resolvedShortcut(ShortcutSlot.LEFT),
                            resolvedShortcut(ShortcutSlot.RIGHT),
                        ),
                    ),
                    appearance = HomeAppearanceState(),
                    actions = HomeActions(
                        onQueryChange = { query = it },
                        onPrimarySearch = {},
                        onBrowserSearch = { browserSearchRequests += 1 },
                        onPlayStoreSearch = {},
                        onMapsSearch = {},
                        onAppClicked = { openedAppLabel = it.label },
                        onShortcutClicked = { openedShortcutSlot = it.slot },
                        onOpenDrawer = { drawerOpenRequests += 1 },
                        onOpenSchedules = { scheduleOpenRequests += 1 },
                        onRemoveHomeApp = {},
                        onRenameHomeApp = { _, _ -> },
                        onUninstallApp = {},
                        onReorderHomeApps = { _, _ -> },
                    ),
                )
            }
        }
    }

    fun assertHomeVisible() {
        composeRule.onNodeWithTag("home_root").assertIsDisplayed()
        composeRule.onNodeWithTag("home_primary_apps").assertIsDisplayed()
    }

    fun assertSearchActionsVisible() {
        composeRule.onNodeWithTag("home_play_store_button").assertIsDisplayed()
        composeRule.onNodeWithTag("home_maps_button").assertIsDisplayed()
        composeRule.onNodeWithTag("home_browser_button").assertIsDisplayed()
        composeRule.onNodeWithTag("home_keyboard_play_store_button").assertIsDisplayed()
        composeRule.onNodeWithTag("home_keyboard_maps_button").assertIsDisplayed()
        composeRule.onNodeWithTag("home_keyboard_browser_button").assertIsDisplayed()
    }

    fun enterSearch(text: String) {
        composeRule.onNodeWithTag("home_search_field").performTextInput(text)
    }

    fun tapBrowser() {
        composeRule.onNodeWithTag("home_browser_button").performClick()
    }

    fun tapAddApp() {
        composeRule.onNodeWithTag("home_add_app_button").performClick()
    }

    fun assertEmptyStateVisible() {
        composeRule.onNodeWithTag("home_empty_state").assertIsDisplayed()
        composeRule.onNodeWithText("No apps to show").assertIsDisplayed()
        composeRule.onNodeWithText("Open app list").assertIsDisplayed()
        composeRule.onNodeWithText("Edit app schedules").assertIsDisplayed()
    }

    fun tapEditSchedules() {
        composeRule.onNodeWithTag("home_edit_schedules_button").performClick()
    }

    fun assertScheduledEmptyStateVisible() {
        composeRule.onNodeWithTag("home_empty_state").assertIsDisplayed()
        composeRule.onNodeWithText("This schedule has no available apps right now.")
            .assertIsDisplayed()
    }

    fun tapHomeApp(label: String) {
        composeRule.onNodeWithTag("home_app_$label").performClick()
    }

    fun tapLeftShortcut() {
        composeRule.onNodeWithTag("shortcut_LEFT").performClick()
    }
}

private class DrawerRobot(
    private val composeRule: androidx.compose.ui.test.junit4.ComposeContentTestRule,
) {
    var openedAppLabel: String? = null
        private set

    fun launch() {
        composeRule.setContent {
            VoidLauncherTheme {
                val apps = listOf(
                    installedApp("Camera"),
                    installedApp("Chrome"),
                    installedApp("Signal"),
                )
                AppDrawerScreen(
                    state = DrawerUiState(
                        apps = apps,
                        sectionLetters = apps.associate { app ->
                            app.key to app.label.first()
                        },
                        alphabetIndex = mapOf('C' to 0, 'S' to 2),
                    ),
                    actions = AppDrawerActions(
                        onBack = {},
                        onOpenSettings = {},
                        onQueryChange = {},
                        onAppClicked = { openedAppLabel = it.label },
                        onAddHomeApp = {},
                        onRemoveHomeApp = {},
                        onUninstallApp = {},
                    ),
                )
            }
        }
    }

    fun assertDrawerVisible() {
        composeRule.onNodeWithTag("drawer_root").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_search_field").assertIsDisplayed()
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
    }

    fun filter(text: String) {
        composeRule.onNodeWithTag("drawer_search_field").performTextInput(text)
    }

    fun tapApp(label: String) {
        composeRule.onNodeWithText(label).performClick()
    }
}
