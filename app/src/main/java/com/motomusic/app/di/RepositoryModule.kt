package com.motomusic.app.di

import com.motomusic.app.data.repository.FavoritesRepositoryImpl
import com.motomusic.app.data.repository.PlaybackHistoryRepositoryImpl
import com.motomusic.app.data.repository.PlaylistRepositoryImpl
import com.motomusic.app.data.repository.SettingsRepositoryImpl
import com.motomusic.app.data.repository.SongRepositoryImpl
import com.motomusic.app.domain.repository.FavoritesRepository
import com.motomusic.app.domain.repository.PlaybackHistoryRepository
import com.motomusic.app.domain.repository.PlaylistRepository
import com.motomusic.app.domain.repository.SettingsRepository
import com.motomusic.app.domain.repository.SongRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSongRepository(impl: SongRepositoryImpl): SongRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackHistoryRepository(impl: PlaybackHistoryRepositoryImpl): PlaybackHistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
