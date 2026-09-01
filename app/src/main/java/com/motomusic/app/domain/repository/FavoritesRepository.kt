package com.motomusic.app.domain.repository

import com.motomusic.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeFavorites(): Flow<List<Song>>
    fun observeFavoriteIds(): Flow<Set<Long>>
    fun observeIsFavorite(songId: Long): Flow<Boolean>
    /** Returns the new state: `true` when the song is now a favorite. */
    suspend fun toggleFavorite(songId: Long): Boolean
}
