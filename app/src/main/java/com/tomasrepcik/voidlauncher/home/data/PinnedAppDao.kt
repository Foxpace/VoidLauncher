package com.tomasrepcik.voidlauncher.home.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedAppDao {
    @Query("SELECT * FROM pinned_apps WHERE section = :section ORDER BY position ASC")
    fun observeSection(section: String): Flow<List<PinnedAppEntity>>

    @Query("SELECT * FROM pinned_apps WHERE section = :section ORDER BY position ASC")
    suspend fun getSection(section: String): List<PinnedAppEntity>

    @Query("DELETE FROM pinned_apps WHERE section = :section")
    suspend fun deleteSection(section: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PinnedAppEntity>)

    @Query(
        "UPDATE pinned_apps SET labelOverride = :label " +
            "WHERE section = :section AND packageName = :packageName AND activityName = :activityName",
    )
    suspend fun updateLabelOverride(section: String, packageName: String, activityName: String, label: String?)
}
