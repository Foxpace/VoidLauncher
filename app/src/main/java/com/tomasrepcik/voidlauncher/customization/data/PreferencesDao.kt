package com.tomasrepcik.voidlauncher.customization.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM launcher_preferences WHERE id = 0")
    fun observe(): Flow<LauncherPreferencesEntity?>

    @Query("SELECT * FROM launcher_preferences WHERE id = 0")
    suspend fun get(): LauncherPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LauncherPreferencesEntity)
}
