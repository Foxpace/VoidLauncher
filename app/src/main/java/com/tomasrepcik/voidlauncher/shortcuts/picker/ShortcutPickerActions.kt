package com.tomasrepcik.voidlauncher.shortcuts.picker

import com.tomasrepcik.voidlauncher.launcher.InstalledApp

data class ShortcutPickerActions(
    val onBack: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onContactsSelected: () -> Unit,
    val onCameraSelected: () -> Unit,
    val onAppSelected: (InstalledApp) -> Unit,
)
