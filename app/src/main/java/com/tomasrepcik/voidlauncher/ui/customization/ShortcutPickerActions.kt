package com.tomasrepcik.voidlauncher.ui.customization

import com.tomasrepcik.voidlauncher.data.model.InstalledApp

data class ShortcutPickerActions(
    val onBack: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onContactsSelected: () -> Unit,
    val onCameraSelected: () -> Unit,
    val onAppSelected: (InstalledApp) -> Unit,
)
