package com.tty.launcher.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandHistoryDao {

    @Query("SELECT * FROM command_history ORDER BY executedAt DESC, id DESC LIMIT :limit")
    fun observe(limit: Int): Flow<List<CommandHistoryEntity>>

    @Query("SELECT * FROM command_history ORDER BY executedAt ASC, id ASC LIMIT :limit")
    fun observeChronological(limit: Int): Flow<List<CommandHistoryEntity>>

    @Query("SELECT command FROM command_history ORDER BY executedAt DESC, id DESC LIMIT 1")
    suspend fun lastCommand(): String?

    @Query("SELECT command FROM command_history ORDER BY executedAt DESC, id DESC LIMIT :limit")
    suspend fun recentCommands(limit: Int): List<String>

    @Query("SELECT COUNT(*) FROM command_history")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: CommandHistoryEntity): Long

    @Query("DELETE FROM command_history WHERE id NOT IN (SELECT id FROM command_history ORDER BY executedAt DESC, id DESC LIMIT :keep)")
    suspend fun trim(keep: Int)

    @Query("DELETE FROM command_history")
    suspend fun clear()

    @Query("DELETE FROM command_history WHERE command = :command")
    suspend fun deleteCommand(command: String)
}

@Dao
interface FavoriteAppDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observe(): Flow<List<FavoriteAppEntity>>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun getAll(): List<FavoriteAppEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE packageName = :packageName)")
    suspend fun isFavorite(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteAppEntity)

    @Query("DELETE FROM favorites WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()
}

@Dao
interface AppAliasDao {

    @Query("SELECT * FROM app_aliases ORDER BY alias COLLATE NOCASE ASC")
    fun observe(): Flow<List<AppAliasEntity>>

    @Query("SELECT * FROM app_aliases")
    suspend fun getAll(): List<AppAliasEntity>

    @Query("SELECT alias FROM app_aliases WHERE packageName = :packageName")
    suspend fun aliasFor(packageName: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AppAliasEntity)

    @Query("DELETE FROM app_aliases WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM app_aliases")
    suspend fun clear()
}

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observe(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<NoteEntity>

    @Insert
    suspend fun insert(entity: NoteEntity): Long

    @Query("UPDATE notes SET content = :content, updatedAt = :updatedAt WHERE id = :id")
    suspend fun update(id: Long, content: String, updatedAt: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notes")
    suspend fun clear()
}

@Dao
interface AppUsageDao {

    @Query("SELECT * FROM app_usage ORDER BY lastUsedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<AppUsageEntity>

    @Query("SELECT * FROM app_usage WHERE packageName = :packageName")
    suspend fun find(packageName: String): AppUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppUsageEntity)

    @Query("DELETE FROM app_usage")
    suspend fun clear()
}
