package com.motomusic.app.di

import android.content.Context
import androidx.room.Room
import com.motomusic.app.data.local.database.MOTO_MUSIC_MIGRATIONS
import com.motomusic.app.data.local.database.MotoMusicDatabase
import com.motomusic.app.data.local.database.dao.FavoriteDao
import com.motomusic.app.data.local.database.dao.PlaybackHistoryDao
import com.motomusic.app.data.local.database.dao.PlaylistDao
import com.motomusic.app.data.local.database.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MotoMusicDatabase =
        Room.databaseBuilder(context, MotoMusicDatabase::class.java, MotoMusicDatabase.NAME)
            .addMigrations(*MOTO_MUSIC_MIGRATIONS)
            .build()

    @Provides
    fun provideSongDao(database: MotoMusicDatabase): SongDao = database.songDao()

    @Provides
    fun providePlaylistDao(database: MotoMusicDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideFavoriteDao(database: MotoMusicDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun providePlaybackHistoryDao(database: MotoMusicDatabase): PlaybackHistoryDao =
        database.playbackHistoryDao()
}
