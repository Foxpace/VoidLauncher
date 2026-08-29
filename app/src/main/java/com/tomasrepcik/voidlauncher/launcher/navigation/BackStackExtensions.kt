package com.tomasrepcik.voidlauncher.launcher.navigation

import androidx.navigation3.runtime.NavKey

internal fun MutableList<NavKey>.pushSingleTop(route: NavKey) {
    if (lastOrNull() != route) {
        add(route)
    }
}

internal fun MutableList<NavKey>.popIfNotRoot() {
    if (size > 1) {
        removeLastOrNull()
    }
}
