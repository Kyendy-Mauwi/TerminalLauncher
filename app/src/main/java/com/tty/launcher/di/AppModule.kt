package com.tty.launcher.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.tty.launcher.data.database.AppAliasDao
import com.tty.launcher.data.database.AppUsageDao
import com.tty.launcher.data.database.CommandHistoryDao
import com.tty.launcher.data.database.FavoriteAppDao
import com.tty.launcher.data.database.NoteDao
import com.tty.launcher.data.database.TTYDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TTYDatabase = TTYDatabase.build(context)

    @Provides
    fun provideHistoryDao(db: TTYDatabase): CommandHistoryDao = db.commandHistoryDao()

    @Provides
    fun provideFavoriteDao(db: TTYDatabase): FavoriteAppDao = db.favoriteAppDao()

    @Provides
    fun provideAliasDao(db: TTYDatabase): AppAliasDao = db.appAliasDao()

    @Provides
    fun provideNoteDao(db: TTYDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideUsageDao(db: TTYDatabase): AppUsageDao = db.appUsageDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile("tty_settings") }
        )

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
