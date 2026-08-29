package com.tomasrepcik.voidlauncher.launcher

import android.content.Context
import android.content.pm.PackageManager
import com.tomasrepcik.voidlauncher.launcher.ShortcutSlot
import com.tomasrepcik.voidlauncher.schedule.editor.ScheduleEditorArgs
import org.koin.core.annotation.KoinExperimentalAPI
import org.junit.Test
import org.junit.Assert.assertNull
import org.koin.test.verify.verify

class LauncherModuleTest {
    @Test
    @OptIn(KoinExperimentalAPI::class)
    fun givenLauncherModule_whenDependencyGraphIsVerified_thenNoMissingDefinitionIsReported() {
        // GIVEN
        val requiredTypes = listOf(
            Context::class,
            PackageManager::class,
            Function0::class,
            Function1::class,
            Function2::class,
            ShortcutSlot::class,
            ScheduleEditorArgs::class,
        )

        // WHEN
        val failure = runCatching {
            launcherModule.verify(extraTypes = requiredTypes)
        }.exceptionOrNull()

        // THEN
        assertNull(failure?.message, failure)
    }
}
