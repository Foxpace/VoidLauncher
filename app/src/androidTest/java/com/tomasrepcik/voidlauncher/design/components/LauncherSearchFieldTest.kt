package com.tomasrepcik.voidlauncher.design.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tomasrepcik.voidlauncher.design.theme.VoidLauncherTheme
import org.junit.Rule
import org.junit.Test

class LauncherSearchFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenFocusedSearch_whenScreenPauses_thenFocusIsCleared() {
        // GIVEN
        val lifecycleOwner = SearchFieldLifecycleOwner()
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                VoidLauncherTheme {
                    var query by remember { mutableStateOf("") }
                    LauncherSearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholderText = "Search",
                        options = LauncherSearchOptions(testTag = SearchFieldTag),
                    )
                }
            }
        }
        composeRule.runOnIdle(lifecycleOwner::resume)
        composeRule.onNodeWithTag(SearchFieldTag).performTextInput("signal")
        composeRule.onNodeWithTag(SearchFieldTag).assertIsFocused()

        // WHEN
        composeRule.runOnIdle(lifecycleOwner::pause)

        // THEN
        composeRule.onNodeWithTag(SearchFieldTag).assertIsNotFocused()
    }
}

private class SearchFieldLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle = registry

    fun resume() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun pause() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }
}

private const val SearchFieldTag = "search_field"
