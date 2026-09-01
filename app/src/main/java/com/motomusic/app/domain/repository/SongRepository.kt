package com.motomusic.app.domain.repository

import com.motomusic.app.domain.model.Album
import com.motomusic.app.domain.model.Artist
import com.motomusic.app.domain.model.Folder
import com.motomusic.app.domain.model.ScanState
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SongRepository {
    val scanState: StateFlow<ScanState>

    fun observeSongs(sortOrder: SortOrder): Flow<List<Song>>
    fun observeSongCount(): Flow<Int>
    fun observeAlbums(): Flow<List<Album>>
    fun observeArtists(): Flow<List<Artist>>
    fun observeFolders(): Flow<List<Folder>>
    fun observeRecentlyAdded(limit: Int): Flow<List<Song>>

    fun search(query: String): Flow<List<Song>>

    fun observeSongsInAlbum(albumId: Long): Flow<List<Song>>
    fun observeSongsByArtist(artistId: Long): Flow<List<Song>>
    fun observeAlbumsByArtist(artistId: Long): Flow<List<Album>>
    fun observeSongsInFolder(path: String): Flow<List<Song>>
    fun observeAlbum(albumId: Long): Flow<Album?>
    fun observeArtist(artistId: Long): Flow<Artist?>

    suspend fun getSong(id: Long): Song?
    suspend fun getSongs(ids: List<Long>): List<Song>

    /** Queries MediaStore and reconciles the local database. Safe to call repeatedly. */
    suspend fun rescan(): ScanState

    /** Drops a song that can no longer be opened, together with its playlist/history rows. */
    suspend fun removeMissingSong(id: Long)
}
