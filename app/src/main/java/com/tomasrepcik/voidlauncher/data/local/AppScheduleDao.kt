package com.tomasrepcik.voidlauncher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppScheduleDao {
    @Query("SELECT * FROM app_schedules ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AppScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppScheduleEntity)

    @Query("DELETE FROM app_schedules WHERE id = :id")
    suspend fun delete(id: String)
}
