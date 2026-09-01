package com.motomusic.app.domain.repository

import com.motomusic.app.domain.model.Playlist
import com.motomusic.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    fun observePlaylist(id: Long): Flow<Playlist?>
    fun observeSongsInPlaylist(id: Long): Flow<List<Song>>

    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(id: Long, name: String)
    suspend fun deletePlaylist(id: Long)
    suspend fun addSongs(playlistId: Long, songIds: List<Long>): Int
    suspend fun removeSong(playlistId: Long, songId: Long)
    suspend fun moveSong(playlistId: Long, fromIndex: Int, toIndex: Int)
    suspend fun playlistNameExists(name: String): Boolean
}
