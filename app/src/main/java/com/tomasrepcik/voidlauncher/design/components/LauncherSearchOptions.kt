package com.tomasrepcik.voidlauncher.design.components

import androidx.compose.ui.focus.FocusRequester

data class LauncherSearchOptions(
    val focusRequester: FocusRequester? = null,
    val testTag: String? = null,
    val onSubmit: (() -> Unit)? = null,
)
