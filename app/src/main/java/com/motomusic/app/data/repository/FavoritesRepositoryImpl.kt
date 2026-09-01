package com.motomusic.app.data.repository

import com.motomusic.app.data.local.database.dao.FavoriteDao
import com.motomusic.app.data.local.database.entity.toDomain
import com.motomusic.app.di.IoDispatcher
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.repository.FavoritesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FavoritesRepository {

    override fun observeFavorites(): Flow<List<Song>> =
        favoriteDao.observeFavoriteSongs().map { it.toDomain() }

    override fun observeFavoriteIds(): Flow<Set<Long>> =
        favoriteDao.observeFavoriteIds().map { it.toSet() }.distinctUntilChanged()

    override fun observeIsFavorite(songId: Long): Flow<Boolean> =
        favoriteDao.observeIsFavorite(songId).distinctUntilChanged()

    override suspend fun toggleFavorite(songId: Long): Boolean = withContext(ioDispatcher) {
        favoriteDao.toggle(songId, System.currentTimeMillis())
    }
}
