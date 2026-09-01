package com.motomusic.app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_stats")
data class PlaybackStatsEntity(
    @PrimaryKey @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "play_count") val playCount: Int,
    @ColumnInfo(name = "last_played_at") val lastPlayedAt: Long,
)
