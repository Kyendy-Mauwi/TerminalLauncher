package com.tty.launcher.data.repositories

import com.tty.launcher.data.database.AppAliasDao
import com.tty.launcher.data.database.AppAliasEntity
import com.tty.launcher.data.database.AppUsageDao
import com.tty.launcher.data.database.AppUsageEntity
import com.tty.launcher.data.database.FavoriteAppDao
import com.tty.launcher.data.database.FavoriteAppEntity
import com.tty.launcher.data.database.NoteDao
import com.tty.launcher.data.database.NoteEntity
import com.tty.launcher.domain.models.AppInfo
import com.tty.launcher.domain.models.FavoriteApp
import com.tty.launcher.domain.models.Note
import com.tty.launcher.domain.models.RecentApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val dao: FavoriteAppDao
) {
    val favorites: Flow<List<FavoriteApp>> = dao.observe().map { rows ->
        rows.map { FavoriteApp(it.packageName, it.label, it.addedAt) }
    }

    suspend fun list(): List<FavoriteApp> = dao.getAll().map { FavoriteApp(it.packageName, it.label, it.addedAt) }

    suspend fun add(app: AppInfo) {
        dao.insert(FavoriteAppEntity(app.packageName, app.label, System.currentTimeMillis()))
    }

    suspend fun remove(packageName: String) = dao.delete(packageName)

    suspend fun isFavorite(packageName: String): Boolean = dao.isFavorite(packageName)
}

@Singleton
class AliasRepository @Inject constructor(
    private val dao: AppAliasDao
) {
    val aliases: Flow<Map<String, String>> = dao.observe().map { rows ->
        rows.associate { it.packageName to it.alias }
    }

    suspend fun snapshot(): Map<String, String> = dao.getAll().associate { it.packageName to it.alias }

    suspend fun aliasFor(packageName: String): String? = dao.aliasFor(packageName)

    suspend fun set(packageName: String, alias: String) {
        dao.insert(AppAliasEntity(packageName, alias.trim().lowercase(), System.currentTimeMillis()))
    }

    suspend fun remove(packageName: String) = dao.delete(packageName)
}

@Singleton
class NotesRepository @Inject constructor(
    private val dao: NoteDao
) {
    val notes: Flow<List<Note>> = dao.observe().map { rows ->
        rows.map { Note(it.id, it.content, it.createdAt, it.updatedAt) }
    }

    suspend fun list(): List<Note> = dao.getAll().map { Note(it.id, it.content, it.createdAt, it.updatedAt) }

    suspend fun add(content: String): Long {
        val now = System.currentTimeMillis()
        return dao.insert(NoteEntity(content = content.trim(), createdAt = now, updatedAt = now))
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun clear() = dao.clear()
}

@Singleton
class RecentAppsRepository @Inject constructor(
    private val dao: AppUsageDao
) {
    suspend fun record(app: AppInfo) {
        val existing = dao.find(app.packageName)
        dao.upsert(
            AppUsageEntity(
                packageName = app.packageName,
                label = app.label,
                lastUsedAt = System.currentTimeMillis(),
                launchCount = (existing?.launchCount ?: 0) + 1
            )
        )
    }

    suspend fun recent(limit: Int = 8): List<RecentApp> = dao.recent(limit).map {
        RecentApp(it.packageName, it.label, it.lastUsedAt, it.launchCount)
    }

    suspend fun clear() = dao.clear()
}
