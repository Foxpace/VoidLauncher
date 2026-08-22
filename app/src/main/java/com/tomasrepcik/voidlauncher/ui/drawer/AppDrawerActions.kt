package com.tomasrepcik.voidlauncher.ui.drawer

import com.tomasrepcik.voidlauncher.data.model.InstalledApp

data class AppDrawerActions(
    val onBack: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onAppClicked: (InstalledApp) -> Unit,
    val onAddHomeApp: (InstalledApp) -> Unit,
    val onRemoveHomeApp: (InstalledApp) -> Unit,
    val onUninstallApp: (InstalledApp) -> Unit,
)
