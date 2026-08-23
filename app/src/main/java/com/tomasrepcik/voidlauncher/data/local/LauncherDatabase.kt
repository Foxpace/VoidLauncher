package com.tomasrepcik.voidlauncher.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pinned_apps", primaryKeys = ["section", "position"])
data class PinnedAppEntity(
    val section: String,
    val position: Int,
    val packageName: String,
    val activityName: String,
    val labelOverride: String? = null,
)

@Entity(tableName = "shortcut_items")
data class ShortcutEntity(
    @PrimaryKey
    val slot: String,
    val position: Int,
    val shortcutType: String,
    val packageName: String? = null,
    val activityName: String? = null,
    val customLabel: String? = null,
)

@Entity(tableName = "launcher_preferences")
data class LauncherPreferencesEntity(
    @PrimaryKey
    val id: Int = 0,
    val homeAppCount: Int,
    val hasSeenNavigationTutorial: Boolean = false,
)

@Entity(tableName = "installed_apps", primaryKeys = ["packageName", "activityName"])
data class InstalledAppEntity(
    val packageName: String,
    val activityName: String,
    val label: String,
    val sortLabel: String,
)

@Entity(tableName = "app_schedules")
data class AppScheduleEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val days: String,
    val startMinute: Int,
    val endMinute: Int,
    val appKeys: String,
    val enabled: Boolean,
)

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
            "WHERE section = :section AND packageName = :packageName AND activityName = :activityName"
    )
    suspend fun updateLabelOverride(section: String, packageName: String, activityName: String, label: String?)
}

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcut_items ORDER BY position ASC")
    fun observeAll(): Flow<List<ShortcutEntity>>

    @Query("SELECT COUNT(*) FROM shortcut_items")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ShortcutEntity)
}

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM launcher_preferences WHERE id = 0")
    fun observe(): Flow<LauncherPreferencesEntity?>

    @Query("SELECT * FROM launcher_preferences WHERE id = 0")
    suspend fun get(): LauncherPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LauncherPreferencesEntity)
}

@Dao
interface InstalledAppDao {
    @Query("SELECT * FROM installed_apps ORDER BY sortLabel ASC")
    fun observeAll(): Flow<List<InstalledAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<InstalledAppEntity>)

    @Query("DELETE FROM installed_apps")
    suspend fun deleteAll()
}

@Dao
interface AppScheduleDao {
    @Query("SELECT * FROM app_schedules ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AppScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppScheduleEntity)

    @Query("DELETE FROM app_schedules WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(
    entities = [
        PinnedAppEntity::class,
        ShortcutEntity::class,
        LauncherPreferencesEntity::class,
        InstalledAppEntity::class,
        AppScheduleEntity::class,
    ],
    version = 6,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun pinnedAppDao(): PinnedAppDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun installedAppDao(): InstalledAppDao
    abstract fun appScheduleDao(): AppScheduleDao
}
