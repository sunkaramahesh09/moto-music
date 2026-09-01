package com.motomusic.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import com.motomusic.app.data.local.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    /**
     * Sorting is user-selectable, so the ORDER BY clause is built at runtime.
     * [observedEntities] keeps the Flow reactive even though the SQL is dynamic.
     */
    @RawQuery(observedEntities = [SongEntity::class])
    fun observeSongsRaw(query: SupportSQLiteQuery): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM songs")
    fun observeSongCount(): Flow<Int>

    @Query("SELECT * FROM songs ORDER BY date_added_sec DESC, id DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSong(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getSongs(ids: List<Long>): List<SongEntity>

    @Query("SELECT id, date_modified_sec FROM songs")
    suspend fun getSyncRows(): List<SongSyncRow>

    @Query(
        """
        SELECT * FROM songs
        WHERE title LIKE '%' || :query || '%' ESCAPE '\'
           OR artist LIKE '%' || :query || '%' ESCAPE '\'
           OR album LIKE '%' || :query || '%' ESCAPE '\'
        ORDER BY
            CASE
                WHEN title LIKE :query || '%' ESCAPE '\' THEN 0
                WHEN artist LIKE :query || '%' ESCAPE '\' THEN 1
                WHEN album LIKE :query || '%' ESCAPE '\' THEN 2
                ELSE 3
            END,
            title COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE album_id = :albumId ORDER BY track_number ASC, title COLLATE NOCASE ASC")
    fun observeSongsInAlbum(albumId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artist_id = :artistId ORDER BY album COLLATE NOCASE ASC, track_number ASC, title COLLATE NOCASE ASC")
    fun observeSongsByArtist(artistId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE folder_path = :path ORDER BY file_name COLLATE NOCASE ASC")
    fun observeSongsInFolder(path: String): Flow<List<SongEntity>>

    @Query(
        """
        SELECT album_id AS albumId, MIN(album) AS album, MIN(artist) AS artist, MIN(artist_id) AS artistId,
               COUNT(*) AS songCount, MAX(year) AS year, MIN(uri) AS artworkSongUri
        FROM songs GROUP BY album_id ORDER BY album COLLATE NOCASE ASC
        """
    )
    fun observeAlbums(): Flow<List<AlbumAggregate>>

    @Query(
        """
        SELECT album_id AS albumId, MIN(album) AS album, MIN(artist) AS artist, MIN(artist_id) AS artistId,
               COUNT(*) AS songCount, MAX(year) AS year, MIN(uri) AS artworkSongUri
        FROM songs WHERE album_id = :albumId GROUP BY album_id
        """
    )
    fun observeAlbum(albumId: Long): Flow<AlbumAggregate?>

    @Query(
        """
        SELECT album_id AS albumId, MIN(album) AS album, MIN(artist) AS artist, MIN(artist_id) AS artistId,
               COUNT(*) AS songCount, MAX(year) AS year, MIN(uri) AS artworkSongUri
        FROM songs WHERE artist_id = :artistId GROUP BY album_id ORDER BY year DESC, album COLLATE NOCASE ASC
        """
    )
    fun observeAlbumsByArtist(artistId: Long): Flow<List<AlbumAggregate>>

    @Query(
        """
        SELECT artist_id AS artistId, MIN(artist) AS name, COUNT(*) AS songCount,
               COUNT(DISTINCT album_id) AS albumCount, MIN(album_id) AS artworkAlbumId, MIN(uri) AS artworkSongUri
        FROM songs GROUP BY artist_id ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeArtists(): Flow<List<ArtistAggregate>>

    @Query(
        """
        SELECT artist_id AS artistId, MIN(artist) AS name, COUNT(*) AS songCount,
               COUNT(DISTINCT album_id) AS albumCount, MIN(album_id) AS artworkAlbumId, MIN(uri) AS artworkSongUri
        FROM songs WHERE artist_id = :artistId GROUP BY artist_id
        """
    )
    fun observeArtist(artistId: Long): Flow<ArtistAggregate?>

    @Query(
        """
        SELECT folder_path AS path, COUNT(*) AS songCount FROM songs
        WHERE folder_path != '' GROUP BY folder_path ORDER BY folder_path COLLATE NOCASE ASC
        """
    )
    fun observeFolders(): Flow<List<FolderAggregate>>

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * Removes songs that vanished from MediaStore along with every reference to them.
     * `playlist_songs` deliberately has no foreign key to `songs` (a rescan rewrites song rows),
     * so orphans are cleaned up here instead of by cascade.
     */
    @Transaction
    suspend fun deleteSongsAndReferences(ids: List<Long>) {
        ids.chunked(SQLITE_VARIABLE_CHUNK).forEach { chunk ->
            deleteByIds(chunk)
            deleteFavoritesFor(chunk)
            deleteHistoryFor(chunk)
            deleteStatsFor(chunk)
            deletePlaylistRefsFor(chunk)
        }
    }

    @Query("DELETE FROM favorites WHERE song_id IN (:ids)")
    suspend fun deleteFavoritesFor(ids: List<Long>)

    @Query("DELETE FROM playback_history WHERE song_id IN (:ids)")
    suspend fun deleteHistoryFor(ids: List<Long>)

    @Query("DELETE FROM playback_stats WHERE song_id IN (:ids)")
    suspend fun deleteStatsFor(ids: List<Long>)

    @Query("DELETE FROM playlist_songs WHERE song_id IN (:ids)")
    suspend fun deletePlaylistRefsFor(ids: List<Long>)
}

/** SQLite allows a limited number of bound variables per statement; stay well under it. */
const val SQLITE_VARIABLE_CHUNK = 400
