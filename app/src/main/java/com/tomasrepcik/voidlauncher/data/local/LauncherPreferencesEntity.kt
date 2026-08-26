package com.tomasrepcik.voidlauncher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launcher_preferences")
data class LauncherPreferencesEntity(
    @PrimaryKey val id: Int = 0,
    // Retained only to keep the existing Room schema compatible; the dead setting is not exposed.
    val homeAppCount: Int = LEGACY_HOME_APP_COUNT,
    val hasSeenNavigationTutorial: Boolean = false,
    val homeBackgroundUri: String? = null,
    val useBackgroundColors: Boolean = false,
)

private const val LEGACY_HOME_APP_COUNT = 8
