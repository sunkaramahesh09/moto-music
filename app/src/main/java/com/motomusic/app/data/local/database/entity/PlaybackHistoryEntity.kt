package com.motomusic.app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One row per completed listen. Trimmed to [MAX_HISTORY_ROWS] so history never grows unbounded. */
@Entity(tableName = "playback_history", indices = [Index("song_id"), Index("played_at")])
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "played_at") val playedAt: Long,
)

const val MAX_HISTORY_ROWS = 500
