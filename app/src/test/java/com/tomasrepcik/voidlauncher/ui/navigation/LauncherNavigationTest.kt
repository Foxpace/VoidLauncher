package com.tomasrepcik.voidlauncher.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import org.junit.Test

class LauncherNavigationTest {
    @Test
    fun givenHomeBackStack_whenDestinationsOpenAndClose_thenNavigatorMaintainsRouteOrder() {
        // GIVEN
        val backStack = mutableListOf<NavKey>(HomeRoute)
        val navigator = LauncherNavigator(backStack)

        // WHEN
        navigator.open(AppListRoute)
        navigator.open(CustomizationRoute)
        navigator.open(ShortcutPickerRoute(ShortcutSlot.LEFT))

        // THEN
        assertThat(backStack).containsExactly(
            HomeRoute,
            AppListRoute,
            CustomizationRoute,
            ShortcutPickerRoute(ShortcutSlot.LEFT),
        ).inOrder()

        // WHEN
        navigator.goBack()
        navigator.open(ScheduleListRoute)
        navigator.open(ScheduleEditorRoute("weekday"))

        // THEN
        assertThat(backStack.last()).isEqualTo(ScheduleEditorRoute("weekday"))

        // WHEN
        navigator.goBack()

        // THEN
        assertThat(backStack.last()).isEqualTo(ScheduleListRoute)
    }

    @Test
    fun givenHomeOnlyBackStack_whenBackIsRequested_thenHomeDestinationRemains() {
        // GIVEN
        val backStack = mutableListOf<NavKey>(HomeRoute)
        val navigator = LauncherNavigator(backStack)

        // WHEN
        navigator.goBack()
        navigator.goBack()

        // THEN
        assertThat(backStack).containsExactly(HomeRoute)
    }
}
