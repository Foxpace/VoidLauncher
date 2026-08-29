package com.tomasrepcik.voidlauncher.shortcuts.data

import com.tomasrepcik.voidlauncher.launcher.ResolvedShortcut
import com.tomasrepcik.voidlauncher.launcher.ShortcutSelection
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot
import com.tomasrepcik.voidlauncher.launcher.error.AppOperation
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherRepository
import com.tomasrepcik.voidlauncher.storage.launcher.LauncherStorage
import com.tomasrepcik.voidlauncher.storage.launcher.readyLauncherState
import com.tomasrepcik.voidlauncher.storage.launcher.writeToStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ShortcutRepository internal constructor(
    launcher: LauncherRepository,
    private val saveShortcut: suspend (ShortcutSlot, ShortcutSelection) -> Unit,
) {
    internal constructor(
        launcher: LauncherRepository,
        storage: LauncherStorage,
    ) : this(launcher, storage::saveShortcut)

    val shortcuts: Flow<List<ResolvedShortcut>?> = launcher.readyLauncherState()
        .map { state -> state?.bottomShortcuts }
        .distinctUntilChanged()

    suspend fun save(slot: ShortcutSlot, selection: ShortcutSelection) =
        writeToStorage(AppOperation.SAVE_SHORTCUT) {
            saveShortcut(slot, selection)
        }
}
