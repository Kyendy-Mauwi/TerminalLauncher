package com.tty.launcher.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val executedAt: Long
)

@Entity(tableName = "favorites")
data class FavoriteAppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val addedAt: Long
)

@Entity(tableName = "app_aliases")
data class AppAliasEntity(
    @PrimaryKey val packageName: String,
    val alias: String,
    val createdAt: Long
)

@Entity(tableName = "notes", indices = [Index(value = ["updatedAt"])])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val lastUsedAt: Long,
    val launchCount: Int
)
