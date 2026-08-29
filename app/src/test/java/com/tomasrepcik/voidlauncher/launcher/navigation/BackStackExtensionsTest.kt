package com.tomasrepcik.voidlauncher.launcher.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import org.junit.Test

class BackStackExtensionsTest {
    @Test
    fun givenRootBackStack_whenPopIfNotRoot_thenRootDestinationIsKept() {
        // GIVEN
        val backStack = mutableStateListOf<NavKey>(TestHomeRoute)

        // WHEN
        backStack.popIfNotRoot()

        // THEN
        assertThat(backStack).containsExactly(TestHomeRoute)
    }

    @Test
    fun givenDestinationAtTop_whenPushSingleTop_thenDuplicateDestinationIsSkipped() {
        // GIVEN
        val backStack = mutableStateListOf<NavKey>(TestHomeRoute, TestSettingsRoute)

        // WHEN
        backStack.pushSingleTop(TestSettingsRoute)

        // THEN
        assertThat(backStack).containsExactly(TestHomeRoute, TestSettingsRoute).inOrder()
    }

    @Test
    fun givenDifferentTopDestination_whenPushSingleTop_thenDestinationIsAdded() {
        // GIVEN
        val backStack = mutableStateListOf<NavKey>(TestHomeRoute, TestAppListRoute)

        // WHEN
        backStack.pushSingleTop(TestSettingsRoute)

        // THEN
        assertThat(backStack)
            .containsExactly(TestHomeRoute, TestAppListRoute, TestSettingsRoute)
            .inOrder()
    }

    @Test
    fun givenNestedBackStack_whenPopIfNotRoot_thenTopDestinationIsRemoved() {
        // GIVEN
        val backStack = mutableStateListOf<NavKey>(TestHomeRoute, TestSettingsRoute)

        // WHEN
        backStack.popIfNotRoot()

        // THEN
        assertThat(backStack).containsExactly(TestHomeRoute)
    }

    @Test
    fun givenDestinationBelowTop_whenPushSingleTop_thenDestinationIsAddedAgain() {
        // GIVEN
        val backStack = mutableStateListOf<NavKey>(TestHomeRoute, TestSettingsRoute)

        // WHEN
        backStack.pushSingleTop(TestHomeRoute)

        // THEN
        assertThat(backStack)
            .containsExactly(TestHomeRoute, TestSettingsRoute, TestHomeRoute)
            .inOrder()
    }
}

@Serializable
private data object TestHomeRoute : NavKey

@Serializable
private data object TestAppListRoute : NavKey

@Serializable
private data object TestSettingsRoute : NavKey
