package com.motomusic.app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)
