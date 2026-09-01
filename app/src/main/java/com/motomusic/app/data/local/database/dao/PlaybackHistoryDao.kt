package com.motomusic.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.motomusic.app.data.local.database.entity.MAX_HISTORY_ROWS
import com.motomusic.app.data.local.database.entity.PlaybackHistoryEntity
import com.motomusic.app.data.local.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {

    /** One row per song, ordered by its most recent listen. */
    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN (
            SELECT song_id, MAX(played_at) AS last_played
            FROM playback_history GROUP BY song_id
        ) h ON s.id = h.song_id
        ORDER BY h.last_played DESC
        LIMIT :limit
        """
    )
    fun observeRecentlyPlayed(limit: Int): Flow<List<SongEntity>>

    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN playback_stats st ON st.song_id = s.id
        WHERE st.play_count > 0
        ORDER BY st.play_count DESC, st.last_played_at DESC
        LIMIT :limit
        """
    )
    fun observeMostPlayed(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT COALESCE((SELECT play_count FROM playback_stats WHERE song_id = :songId), 0)")
    fun observePlayCount(songId: Long): Flow<Int>

    @Insert
    suspend fun insertHistory(entry: PlaybackHistoryEntity)

    @Query(
        """
        INSERT INTO playback_stats (song_id, play_count, last_played_at)
        VALUES (:songId, 1, :playedAt)
        ON CONFLICT(song_id) DO UPDATE SET
            play_count = play_count + 1,
            last_played_at = :playedAt
        """
    )
    suspend fun incrementPlayCount(songId: Long, playedAt: Long)

    @Query(
        """
        DELETE FROM playback_history WHERE id NOT IN (
            SELECT id FROM playback_history ORDER BY played_at DESC LIMIT :keep
        )
        """
    )
    suspend fun trimHistory(keep: Int)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("DELETE FROM playback_stats")
    suspend fun clearStats()

    @Transaction
    suspend fun recordPlay(songId: Long, playedAt: Long) {
        insertHistory(PlaybackHistoryEntity(songId = songId, playedAt = playedAt))
        incrementPlayCount(songId, playedAt)
        trimHistory(MAX_HISTORY_ROWS)
    }

    @Transaction
    suspend fun clearAll() {
        clearHistory()
        clearStats()
    }
}
