package com.motomusic.app.data.repository

import com.motomusic.app.data.local.database.dao.PlaybackHistoryDao
import com.motomusic.app.data.local.database.entity.toDomain
import com.motomusic.app.di.IoDispatcher
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.repository.PlaybackHistoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class PlaybackHistoryRepositoryImpl @Inject constructor(
    private val historyDao: PlaybackHistoryDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PlaybackHistoryRepository {

    override fun observeRecentlyPlayed(limit: Int): Flow<List<Song>> =
        historyDao.observeRecentlyPlayed(limit).map { it.toDomain() }

    override fun observeMostPlayed(limit: Int): Flow<List<Song>> =
        historyDao.observeMostPlayed(limit).map { it.toDomain() }

    override fun observePlayCount(songId: Long): Flow<Int> = historyDao.observePlayCount(songId)

    override suspend fun recordPlay(songId: Long, playedAtMs: Long) = withContext(ioDispatcher) {
        historyDao.recordPlay(songId, playedAtMs)
    }

    override suspend fun clearHistory() = withContext(ioDispatcher) {
        historyDao.clearAll()
    }
}
