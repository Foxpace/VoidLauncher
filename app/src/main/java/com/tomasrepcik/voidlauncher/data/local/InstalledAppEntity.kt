package com.tomasrepcik.voidlauncher.data.local

import androidx.room.Entity

@Entity(tableName = "installed_apps", primaryKeys = ["packageName", "activityName"])
data class InstalledAppEntity(
    val packageName: String,
    val activityName: String,
    val label: String,
    val sortLabel: String,
)
