package com.tomasrepcik.voidlauncher.home.content

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchActionBarsTest {
    @Test
    fun givenActiveSearch_whenKeyboardIsDismissed_thenKeyboardSearchActionsAreHidden() {
        // GIVEN
        val query = "spotify"
        val actionsVisible = keyboardSearchActionsAreVisible(query, isKeyboardVisible = true)

        // WHEN
        val actionsVisibleAfterDismiss = keyboardSearchActionsAreVisible(
            query,
            isKeyboardVisible = false,
        )

        // THEN
        assertTrue(actionsVisible)
        assertFalse(actionsVisibleAfterDismiss)
    }
}
