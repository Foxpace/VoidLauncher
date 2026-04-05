package com.tomasrepcik.voidlauncher.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import org.junit.Test

class BackStackExtensionsTest {
    @Test
    fun popIfNotRoot_keepsRootDestination() {
        val backStack = mutableStateListOf<NavKey>(TestHomeRoute)

        backStack.popIfNotRoot()

        assertThat(backStack).containsExactly(TestHomeRoute)
    }

    @Test
    fun pushSingleTop_skipsDuplicateTopDestination() {
        val backStack = mutableStateListOf<NavKey>(TestHomeRoute, TestSettingsRoute)

        backStack.pushSingleTop(TestSettingsRoute)

        assertThat(backStack).containsExactly(TestHomeRoute, TestSettingsRoute).inOrder()
    }

    @Test
    fun pushSingleTop_addsDifferentDestination() {
        val backStack = mutableStateListOf<NavKey>(TestHomeRoute, TestAppListRoute)

        backStack.pushSingleTop(TestSettingsRoute)

        assertThat(backStack)
            .containsExactly(TestHomeRoute, TestAppListRoute, TestSettingsRoute)
            .inOrder()
    }
}

@Serializable
private data object TestHomeRoute : NavKey

@Serializable
private data object TestAppListRoute : NavKey

@Serializable
private data object TestSettingsRoute : NavKey
