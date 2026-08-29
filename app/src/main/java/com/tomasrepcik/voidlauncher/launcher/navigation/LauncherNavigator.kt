package com.tomasrepcik.voidlauncher.launcher.navigation

import androidx.navigation3.runtime.NavKey
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object AppListRoute : NavKey

@Serializable
data object CustomizationRoute : NavKey

@Serializable
data class ShortcutPickerRoute(val slot: ShortcutSlot) : NavKey

@Serializable
data object ScheduleListRoute : NavKey

@Serializable
data class ScheduleEditorRoute(val scheduleId: String? = null) : NavKey

internal class LauncherNavigator(
    private val backStack: MutableList<NavKey>,
) {
    val isAtHome: Boolean
        get() = backStack.size == 1

    fun open(route: NavKey) {
        backStack.pushSingleTop(route)
    }

    fun goBack() {
        backStack.popIfNotRoot()
    }
}
