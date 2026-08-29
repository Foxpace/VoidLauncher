package com.tomasrepcik.voidlauncher.shortcuts.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shortcut_items")
data class ShortcutEntity(
    @PrimaryKey val slot: String,
    val position: Int,
    val shortcutType: String,
    val packageName: String? = null,
    val activityName: String? = null,
    val customLabel: String? = null,
)
