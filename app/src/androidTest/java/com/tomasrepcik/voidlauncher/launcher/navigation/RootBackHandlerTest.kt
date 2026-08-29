package com.tomasrepcik.voidlauncher.launcher.navigation

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class RootBackHandlerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenRootDestination_whenBackIsPressed_thenNavigationIsBlockedUntilRootChanges() {
        // GIVEN
        var isAtRoot by mutableStateOf(true)
        var navigationBackPresses = 0
        lateinit var dispatcher: OnBackPressedDispatcher

        composeRule.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            BackHandler { navigationBackPresses++ }
            ConsumeBackAtRoot(isAtRoot)
        }

        // WHEN
        repeat(3) {
            composeRule.runOnIdle(dispatcher::onBackPressed)
        }

        // THEN
        assertThat(navigationBackPresses).isEqualTo(0)

        // WHEN
        composeRule.runOnIdle { isAtRoot = false }
        composeRule.runOnIdle(dispatcher::onBackPressed)

        // THEN
        assertThat(navigationBackPresses).isEqualTo(1)
    }
}
