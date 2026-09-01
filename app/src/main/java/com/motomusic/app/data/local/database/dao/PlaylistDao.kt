package com.motomusic.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.motomusic.app.data.local.database.entity.PlaylistEntity
import com.motomusic.app.data.local.database.entity.PlaylistSongCrossRef
import com.motomusic.app.data.local.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT p.id AS id, p.name AS name, p.created_at AS createdAt, p.updated_at AS updatedAt,
               (SELECT COUNT(*) FROM playlist_songs ps WHERE ps.playlist_id = p.id) AS songCount,
               (SELECT s.album_id FROM playlist_songs ps JOIN songs s ON s.id = ps.song_id
                 WHERE ps.playlist_id = p.id ORDER BY ps.position ASC LIMIT 1) AS artworkAlbumId,
               (SELECT s.uri FROM playlist_songs ps JOIN songs s ON s.id = ps.song_id
                 WHERE ps.playlist_id = p.id ORDER BY ps.position ASC LIMIT 1) AS artworkSongUri
        FROM playlists p
        ORDER BY p.name COLLATE NOCASE ASC
        """
    )
    fun observePlaylists(): Flow<List<PlaylistAggregate>>

    @Query(
        """
        SELECT p.id AS id, p.name AS name, p.created_at AS createdAt, p.updated_at AS updatedAt,
               (SELECT COUNT(*) FROM playlist_songs ps WHERE ps.playlist_id = p.id) AS songCount,
               (SELECT s.album_id FROM playlist_songs ps JOIN songs s ON s.id = ps.song_id
                 WHERE ps.playlist_id = p.id ORDER BY ps.position ASC LIMIT 1) AS artworkAlbumId,
               (SELECT s.uri FROM playlist_songs ps JOIN songs s ON s.id = ps.song_id
                 WHERE ps.playlist_id = p.id ORDER BY ps.position ASC LIMIT 1) AS artworkSongUri
        FROM playlists p WHERE p.id = :id
        """
    )
    fun observePlaylist(id: Long): Flow<PlaylistAggregate?>

    @Query(
        """
        SELECT s.* FROM playlist_songs ps
        INNER JOIN songs s ON s.id = ps.song_id
        WHERE ps.playlist_id = :playlistId
        ORDER BY ps.position ASC
        """
    )
    fun observeSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updated_at = :now WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String, now: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("SELECT COUNT(*) FROM playlists WHERE name = :name COLLATE NOCASE")
    suspend fun countByName(name: String): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<PlaylistSongCrossRef>)

    @Query("SELECT * FROM playlist_songs WHERE playlist_id = :playlistId ORDER BY position ASC")
    suspend fun crossRefs(playlistId: Long): List<PlaylistSongCrossRef>

    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Query("UPDATE playlists SET updated_at = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long)

    /** Only songs that still exist in the library are added, so playlists never gain dead entries. */
    @Query("SELECT id FROM songs WHERE id IN (:ids)")
    suspend fun filterExistingSongIds(ids: List<Long>): List<Long>

    @Transaction
    suspend fun appendSongs(playlistId: Long, songIds: List<Long>, now: Long): Int {
        val existing = songIds.chunked(SQLITE_VARIABLE_CHUNK)
            .flatMap { filterExistingSongIds(it) }
            .toSet()
        val toAdd = songIds.filter { it in existing }
        if (toAdd.isEmpty()) return 0
        var position = maxPosition(playlistId) + 1
        insertCrossRefs(toAdd.map { PlaylistSongCrossRef(playlistId, it, position++, now) })
        touch(playlistId, now)
        return toAdd.size
    }

    /**
     * Positions are part of the primary key, so the whole membership list is rewritten
     * inside one transaction rather than updated row by row.
     */
    @Transaction
    suspend fun rewriteOrder(playlistId: Long, orderedSongIds: List<Long>, now: Long) {
        val previous = crossRefs(playlistId).associateBy { it.songId }
        clearPlaylistSongs(playlistId)
        insertCrossRefs(
            orderedSongIds.mapIndexed { index, songId ->
                PlaylistSongCrossRef(playlistId, songId, index, previous[songId]?.addedAt ?: now)
            }
        )
        touch(playlistId, now)
    }

    @Transaction
    suspend fun removeSong(playlistId: Long, songId: Long, now: Long) {
        val remaining = crossRefs(playlistId).map { it.songId }.toMutableList()
        if (!remaining.remove(songId)) return
        rewriteOrder(playlistId, remaining, now)
    }
}
