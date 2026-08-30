package com.tomasrepcik.voidlauncher.home

import com.tomasrepcik.voidlauncher.launcher.InstalledApp
import com.tomasrepcik.voidlauncher.launcher.ResolvedShortcut

data class HomeActions(
    val onQueryChange: (String) -> Unit,
    val onPrimarySearch: () -> Unit,
    val onBrowserSearch: () -> Unit,
    val onPlayStoreSearch: () -> Unit,
    val onMapsSearch: () -> Unit,
    val onAppClicked: (InstalledApp) -> Unit,
    val onShortcutClicked: (ResolvedShortcut) -> Unit,
    val onOpenDrawer: () -> Unit,
    val onOpenSchedules: () -> Unit,
    val onAddHomeApp: (InstalledApp) -> Unit,
    val onRemoveHomeApp: (InstalledApp) -> Unit,
    val onRenameHomeApp: (InstalledApp, String?) -> Unit,
    val onUninstallApp: (InstalledApp) -> Unit,
    val onReorderHomeApps: (Int, Int) -> Unit,
)
