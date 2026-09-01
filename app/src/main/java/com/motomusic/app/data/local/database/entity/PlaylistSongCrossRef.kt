package com.motomusic.app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Membership of a song in a playlist. [position] keeps the user's manual ordering;
 * the same song may legitimately appear twice, so the primary key includes it.
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlist_id", "song_id", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlist_id"), Index("song_id")],
)
data class PlaylistSongCrossRef(
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    @ColumnInfo(name = "song_id") val songId: Long,
    val position: Int,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)
