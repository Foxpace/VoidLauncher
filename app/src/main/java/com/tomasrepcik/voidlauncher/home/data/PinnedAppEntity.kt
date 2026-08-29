package com.tomasrepcik.voidlauncher.home.data

import androidx.room.Entity

@Entity(tableName = "pinned_apps", primaryKeys = ["section", "position"])
data class PinnedAppEntity(
    val section: String,
    val position: Int,
    val packageName: String,
    val activityName: String,
    val labelOverride: String? = null,
)
