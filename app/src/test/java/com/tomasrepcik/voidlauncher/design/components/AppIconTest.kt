package com.tomasrepcik.voidlauncher.design.components

import com.google.common.truth.Truth.assertThat
import com.tomasrepcik.voidlauncher.testing.installedApp
import org.junit.Test

class AppIconTest {
    @Test
    fun givenReinstalledApp_whenIconCacheKeyIsCreated_thenPreviousIconIsNotReused() {
        // GIVEN
        val previousInstall = installedApp("Maps")
        val currentInstall = previousInstall.copy(packageRevision = 1)

        // WHEN
        val previousCacheKey = previousInstall.iconCacheKey()
        val currentCacheKey = currentInstall.iconCacheKey()

        // THEN
        assertThat(currentCacheKey).isNotEqualTo(previousCacheKey)
    }
}
