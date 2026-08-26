package com.tomasrepcik.voidlauncher.data.repository

import com.tomasrepcik.voidlauncher.data.model.ResolvedShortcut
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.error.AppOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ShortcutRepository internal constructor(
    launcher: LauncherRepository,
    private val storage: ShortcutStorage,
) {
    val shortcuts: Flow<List<ResolvedShortcut>?> = launcher.readyLauncherState()
        .map { state -> state?.bottomShortcuts }
        .distinctUntilChanged()

    suspend fun save(slot: ShortcutSlot, selection: ShortcutSelection) =
        writeToStorage(AppOperation.SAVE_SHORTCUT) {
            storage.saveShortcut(slot, selection)
        }
}
