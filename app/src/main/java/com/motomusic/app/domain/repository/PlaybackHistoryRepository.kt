package com.motomusic.app.domain.repository

import com.motomusic.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaybackHistoryRepository {
    fun observeRecentlyPlayed(limit: Int): Flow<List<Song>>
    fun observeMostPlayed(limit: Int): Flow<List<Song>>
    fun observePlayCount(songId: Long): Flow<Int>

    /** Records one completed listen; called only once a song passes the play threshold. */
    suspend fun recordPlay(songId: Long, playedAtMs: Long = System.currentTimeMillis())
    suspend fun clearHistory()
}
