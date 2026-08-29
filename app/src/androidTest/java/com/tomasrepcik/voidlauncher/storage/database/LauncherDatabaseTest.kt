package com.tomasrepcik.voidlauncher.storage.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var database: LauncherDatabase? = null

    @After
    fun closeDatabase() {
        database?.close()
        context.deleteDatabase(LAUNCHER_DATABASE_NAME)
    }

    @Test
    fun givenNoLauncherDatabase_whenCurrentSchemaIsOpened_thenWritableDatabaseUsesCurrentVersion() {
        // GIVEN
        context.deleteDatabase(LAUNCHER_DATABASE_NAME)

        // WHEN
        database = openLauncherDatabase(context)
        val writableDatabase = requireNotNull(database).openHelper.writableDatabase

        // THEN
        assertEquals(LAUNCHER_DATABASE_VERSION, writableDatabase.version)
        assertTrue(writableDatabase.isOpen)
    }
}
