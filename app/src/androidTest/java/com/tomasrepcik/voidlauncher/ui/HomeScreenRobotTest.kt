package com.tomasrepcik.voidlauncher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.ui.drawer.AppDrawerScreen
import com.tomasrepcik.voidlauncher.ui.drawer.DrawerUiState
import com.tomasrepcik.voidlauncher.ui.home.HomeScreen
import com.tomasrepcik.voidlauncher.ui.home.HomeUiState
import com.tomasrepcik.voidlauncher.ui.theme.VoidLauncherTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenRobotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeRobot_rendersPinnedAppsAndSearchActions() {
        val robot = HomeRobot(composeRule)
        robot.launch()
        robot.assertHomeVisible()
        robot.enterSearch("spotify")
        robot.assertSearchActionsVisible()
        robot.tapBrowser()
    }

    @Test
    fun drawerRobot_rendersSearchAndAlphabetList() {
        val robot = DrawerRobot(composeRule)
        robot.launch()
        robot.assertDrawerVisible()
        robot.filter("cam")
    }
}

private class HomeRobot(
    private val composeRule: androidx.compose.ui.test.junit4.ComposeContentTestRule,
) {
    fun launch() {
        composeRule.setContent {
            VoidLauncherTheme {
                var query by remember { mutableStateOf("") }
                HomeScreen(
                    state = HomeUiState(
                        query = query,
                        homeApps = listOf(
                            app("Signal"),
                            app("Spotify"),
                        ),
                        shortcuts = listOf(
                            ResolvedShortcut(
                                slot = ShortcutSlot.LEFT,
                                label = "Contacts",
                                selection = ShortcutSelection.SystemContacts,
                            ),
                            ResolvedShortcut(
                                slot = ShortcutSlot.RIGHT,
                                label = "Camera",
                                selection = ShortcutSelection.SystemCamera,
                            ),
                        ),
                    ),
                    onQueryChange = { query = it },
                    onPrimarySearch = {},
                    onBrowserSearch = {},
                    onPlayStoreSearch = {},
                    onMapsSearch = {},
                    onAppHint = {},
                    onAppClicked = {},
                    onShortcutClicked = {},
                    onOpenDrawer = {},
                    onRemoveHomeApp = {},
                    onReorderHomeApps = { _, _ -> },
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
        composeRule.onNodeWithTag("home_hint_button").assertIsDisplayed()
    }

    fun enterSearch(text: String) {
        composeRule.onNodeWithTag("home_search_field").performTextInput(text)
    }

    fun tapBrowser() {
        composeRule.onNodeWithTag("home_browser_button").performClick()
    }
}

private class DrawerRobot(
    private val composeRule: androidx.compose.ui.test.junit4.ComposeContentTestRule,
) {
    fun launch() {
        composeRule.setContent {
            VoidLauncherTheme {
                AppDrawerScreen(
                    state = DrawerUiState(
                        apps = listOf(
                            app("Camera"),
                            app("Chrome"),
                            app("Signal"),
                        ),
                    ),
                    onBack = {},
                    onOpenSettings = {},
                    onQueryChange = {},
                    onAppClicked = {},
                    onAddHomeApp = {},
                    onRemoveHomeApp = {},
                    onUninstallApp = {},
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
}

private fun app(label: String): InstalledApp = InstalledApp(
    key = AppKey(
        packageName = "pkg.${label.lowercase()}",
        activityName = "Activity${label.lowercase()}",
    ),
    label = label,
    sortLabel = label.lowercase(),
)
