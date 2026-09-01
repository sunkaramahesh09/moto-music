package com.motomusic.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.motomusic.app.data.local.database.entity.FavoriteEntity
import com.motomusic.app.data.local.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query(
        """
        SELECT s.* FROM favorites f
        INNER JOIN songs s ON s.id = f.song_id
        ORDER BY f.added_at DESC
        """
    )
    fun observeFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT song_id FROM favorites")
    fun observeFavoriteIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE song_id = :songId)")
    fun observeIsFavorite(songId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE song_id = :songId)")
    suspend fun isFavorite(songId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE song_id = :songId")
    suspend fun delete(songId: Long)

    @Transaction
    suspend fun toggle(songId: Long, now: Long): Boolean {
        return if (isFavorite(songId)) {
            delete(songId)
            false
        } else {
            insert(FavoriteEntity(songId, now))
            true
        }
    }
}
