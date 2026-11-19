package com.tty.launcher.data.repositories

import com.tty.launcher.data.database.CommandHistoryDao
import com.tty.launcher.data.database.CommandHistoryEntity
import com.tty.launcher.domain.models.CommandHistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dao: CommandHistoryDao
) {
    fun observe(limit: Int): Flow<List<CommandHistoryEntry>> =
        dao.observeChronological(limit).map { rows ->
            rows.map { CommandHistoryEntry(it.id, it.command, it.executedAt) }
        }

    suspend fun record(rawCommand: String, limit: Int) {
        val command = rawCommand.trim()
        if (command.isEmpty()) return
        // Avoid duplicate consecutive entries (e.g. pressing enter on the same command).
        if (dao.lastCommand() == command) return
        dao.insert(CommandHistoryEntity(command = command, executedAt = System.currentTimeMillis()))
        dao.trim(limit)
    }

    suspend fun recent(limit: Int): List<String> = dao.recentCommands(limit)

    suspend fun count(): Int = dao.count()

    suspend fun trimTo(limit: Int) = dao.trim(limit)

    suspend fun clear() = dao.clear()
}
