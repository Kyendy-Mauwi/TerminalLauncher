package com.tty.launcher.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tty.launcher.data.database.AppAliasEntity
import com.tty.launcher.data.database.CommandHistoryEntity
import com.tty.launcher.data.database.FavoriteAppEntity
import com.tty.launcher.data.database.NoteEntity
import com.tty.launcher.data.database.TTYDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TTYDatabaseTest {

    private lateinit var database: TTYDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TTYDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun historyIsStoredNewestFirstAndTrimmed() = runBlocking {
        val dao = database.commandHistoryDao()
        repeat(5) { index ->
            dao.insert(CommandHistoryEntity(command = "cmd $index", executedAt = 1_000L + index))
        }

        assertEquals("cmd 4", dao.lastCommand())
        assertEquals(listOf("cmd 4", "cmd 3"), dao.recentCommands(2))

        dao.trim(2)
        assertEquals(2, dao.count())
        assertEquals("cmd 4", dao.lastCommand())

        dao.clear()
        assertEquals(0, dao.count())
    }

    @Test
    fun favoritesCanBeAddedRemovedAndQueried() = runBlocking {
        val dao = database.favoriteAppDao()
        dao.insert(FavoriteAppEntity("com.example.app", "Example", 10L))

        assertTrue(dao.isFavorite("com.example.app"))
        assertEquals("Example", dao.getAll().first().label)

        dao.delete("com.example.app")
        assertEquals(0, dao.getAll().size)
    }

    @Test
    fun aliasesPersistAndReplace() = runBlocking {
        val dao = database.appAliasDao()
        dao.insert(AppAliasEntity("com.android.chrome", "chrome", 1L))
        assertEquals("chrome", dao.aliasFor("com.android.chrome"))

        dao.insert(AppAliasEntity("com.android.chrome", "browser", 2L))
        assertEquals("browser", dao.aliasFor("com.android.chrome"))

        dao.delete("com.android.chrome")
        assertNull(dao.aliasFor("com.android.chrome"))
    }

    @Test
    fun notesArePersistedAndDeletable() = runBlocking {
        val dao = database.noteDao()
        val id = dao.insert(NoteEntity(content = "first note", createdAt = 1L, updatedAt = 1L))
        dao.insert(NoteEntity(content = "second note", createdAt = 2L, updatedAt = 2L))

        assertEquals(2, dao.getAll().size)

        dao.update(id, "edited note", 3L)
        assertEquals("edited note", dao.getAll().first { it.id == id }.content)

        dao.delete(id)
        assertEquals(1, dao.getAll().size)

        dao.clear()
        assertEquals(0, dao.getAll().size)
    }

    @Test
    fun usageTrackingUpserts() = runBlocking {
        val dao = database.appUsageDao()
        val entity = com.tty.launcher.data.database.AppUsageEntity(
            packageName = "com.example.app",
            label = "Example",
            lastUsedAt = 100L,
            launchCount = 1
        )
        dao.upsert(entity)
        dao.upsert(entity.copy(lastUsedAt = 200L, launchCount = 2))

        val stored = dao.find("com.example.app")
        assertEquals(2, stored?.launchCount)
        assertEquals(200L, stored?.lastUsedAt)
        assertEquals(1, dao.recent(10).size)
    }
}
