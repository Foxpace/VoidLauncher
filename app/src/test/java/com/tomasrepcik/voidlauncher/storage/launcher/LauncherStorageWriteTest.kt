package com.tomasrepcik.voidlauncher.storage.launcher

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.launcher.AppKey
import com.tomasrepcik.voidlauncher.launcher.ShortcutSelection
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot
import com.tomasrepcik.voidlauncher.schedule.data.AppSchedule
import com.tomasrepcik.voidlauncher.storage.database.LauncherStorageSnapshot
import com.tomasrepcik.voidlauncher.storage.database.StoredPinnedApp
import com.tomasrepcik.voidlauncher.testing.InMemoryLauncherStorage
import java.time.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LauncherStorageWriteTest {
    @Test
    fun givenInMemoryStorage_whenEveryWriteRuns_thenAllStateChangesArePersisted() = runTest {
        // GIVEN
        val storage = InMemoryLauncherStorage()

        // WHEN
        val result = exerciseEveryWrite(storage)

        // THEN
        assertEveryWriteWasPersisted(result)
    }

    private suspend fun exerciseEveryWrite(storage: LauncherStorage): StorageWriteResult {
        val first = AppKey("one", "OneActivity")
        val second = AppKey("two", "TwoActivity")
        val third = AppKey("three", "ThreeActivity")
        val schedule = AppSchedule(
            id = "weekday",
            name = "Weekday",
            days = setOf(DayOfWeek.MONDAY),
            startMinute = 480,
            endMinute = 1_020,
            appKeys = setOf(first),
        )

        storage.initialize()
        storage.saveHomeApps(listOf(first, second))
        storage.addHomeApp(third)
        storage.reorderHomeApps(fromIndex = 2, toIndex = 0)
        storage.renameHomeApp(first, "Renamed")
        storage.removeHomeApp(second)
        storage.saveShortcut(ShortcutSlot.LEFT, ShortcutSelection.AppShortcut(first))
        storage.saveSchedule(schedule)

        val saved = storage.snapshots.first()
        storage.deleteSchedule(schedule.id)
        val afterDelete = storage.snapshots.first()

        return StorageWriteResult(
            first = first,
            third = third,
            schedule = schedule,
            saved = saved,
            afterDelete = afterDelete,
        )
    }

    private fun assertEveryWriteWasPersisted(result: StorageWriteResult) {
        val renamedApp = result.saved.pinnedApps.single { it.key == result.first }
        val savedShortcut = result.saved.shortcuts.single { it.slot == ShortcutSlot.LEFT }
        assertThat(result.saved.pinnedApps.map(StoredPinnedApp::key))
            .containsExactly(result.third, result.first)
            .inOrder()
        assertThat(renamedApp.labelOverride).isEqualTo("Renamed")
        assertThat(savedShortcut.selection).isEqualTo(ShortcutSelection.AppShortcut(result.first))
        assertThat(result.saved.schedules).containsExactly(result.schedule)
        assertThat(result.afterDelete.schedules).isEmpty()
    }
}

private data class StorageWriteResult(
    val first: AppKey,
    val third: AppKey,
    val schedule: AppSchedule,
    val saved: LauncherStorageSnapshot,
    val afterDelete: LauncherStorageSnapshot,
)
