package com.tomasrepcik.voidlauncher.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.data.local.LauncherDatabase
import com.tomasrepcik.voidlauncher.data.model.AppKey
import com.tomasrepcik.voidlauncher.data.model.ShortcutSelection
import com.tomasrepcik.voidlauncher.data.model.ShortcutSlot
import com.tomasrepcik.voidlauncher.domain.schedule.AppSchedule
import java.time.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class RoomLauncherStorageWriteTest {
    @Test
    fun givenRoomStorage_whenEveryWriteRuns_thenAllStateChangesArePersisted() = runBlocking {
        // GIVEN
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // WHEN
        val result = try {
            exerciseEveryWrite(RoomLauncherStorage(database))
        } finally {
            database.close()
        }

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
        assertThat(result.saved.pinnedApps.map(StoredPinnedApp::key))
            .containsExactly(result.third, result.first)
            .inOrder()
        assertThat(result.saved.pinnedApps.single { it.key == result.first }.labelOverride)
            .isEqualTo("Renamed")
        assertThat(result.saved.shortcuts.single { it.slot == ShortcutSlot.LEFT }.selection)
            .isEqualTo(ShortcutSelection.AppShortcut(result.first))
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
