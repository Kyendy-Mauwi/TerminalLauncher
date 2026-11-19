package com.tty.launcher.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CommandHistoryEntity::class,
        FavoriteAppEntity::class,
        AppAliasEntity::class,
        NoteEntity::class,
        AppUsageEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class TTYDatabase : RoomDatabase() {

    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun favoriteAppDao(): FavoriteAppDao
    abstract fun appAliasDao(): AppAliasDao
    abstract fun noteDao(): NoteDao
    abstract fun appUsageDao(): AppUsageDao

    companion object {
        const val NAME = "tty.db"

        /**
         * Version 1 shipped only command history. Version 2 adds favourites, aliases, notes and
         * usage tracking; existing history rows are preserved.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites` (" +
                        "`packageName` TEXT NOT NULL, `label` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`packageName`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_aliases` (" +
                        "`packageName` TEXT NOT NULL, `alias` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`packageName`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_usage` (" +
                        "`packageName` TEXT NOT NULL, `label` TEXT NOT NULL, `lastUsedAt` INTEGER NOT NULL, " +
                        "`launchCount` INTEGER NOT NULL, PRIMARY KEY(`packageName`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_updatedAt` ON `notes` (`updatedAt`)")
            }
        }

        fun build(context: Context): TTYDatabase =
            Room.databaseBuilder(context.applicationContext, TTYDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
