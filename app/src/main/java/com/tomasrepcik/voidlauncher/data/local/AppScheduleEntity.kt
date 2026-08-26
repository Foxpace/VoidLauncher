package com.tomasrepcik.voidlauncher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_schedules")
data class AppScheduleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val days: String,
    val startMinute: Int,
    val endMinute: Int,
    val appKeys: String,
    val enabled: Boolean,
)
