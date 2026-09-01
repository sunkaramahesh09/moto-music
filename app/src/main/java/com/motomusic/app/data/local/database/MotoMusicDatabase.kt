package com.motomusic.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.motomusic.app.data.local.database.dao.FavoriteDao
import com.motomusic.app.data.local.database.dao.PlaybackHistoryDao
import com.motomusic.app.data.local.database.dao.PlaylistDao
import com.motomusic.app.data.local.database.dao.SongDao
import com.motomusic.app.data.local.database.entity.FavoriteEntity
import com.motomusic.app.data.local.database.entity.PlaybackHistoryEntity
import com.motomusic.app.data.local.database.entity.PlaybackStatsEntity
import com.motomusic.app.data.local.database.entity.PlaylistEntity
import com.motomusic.app.data.local.database.entity.PlaylistSongCrossRef
import com.motomusic.app.data.local.database.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        FavoriteEntity::class,
        PlaybackHistoryEntity::class,
        PlaybackStatsEntity::class,
    ],
    version = MotoMusicDatabase.VERSION,
    exportSchema = true,
)
abstract class MotoMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    companion object {
        const val VERSION = 1
        const val NAME = "moto_music.db"
    }
}
