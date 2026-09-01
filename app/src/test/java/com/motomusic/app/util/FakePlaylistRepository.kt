package com.motomusic.app.util

import com.motomusic.app.domain.model.Playlist
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** An in-memory stand-in for the Room-backed playlist repository. */
class FakePlaylistRepository : PlaylistRepository {

    private val playlists = MutableStateFlow<List<Playlist>>(emptyList())
    private val songsByPlaylist = MutableStateFlow<Map<Long, List<Song>>>(emptyMap())
    private var nextId = 1L

    override fun observePlaylists(): Flow<List<Playlist>> = playlists

    override fun observePlaylist(id: Long): Flow<Playlist?> =
        playlists.map { list -> list.firstOrNull { it.id == id } }

    override fun observeSongsInPlaylist(id: Long): Flow<List<Song>> =
        songsByPlaylist.map { it[id].orEmpty() }

    override suspend fun createPlaylist(name: String): Long {
        val id = nextId++
        playlists.value = playlists.value + Playlist(
            id = id,
            name = name,
            songCount = 0,
            createdAt = id,
            updatedAt = id,
            artworkAlbumId = null,
            artworkSongUri = null,
        )
        return id
    }

    override suspend fun renamePlaylist(id: Long, name: String) {
        playlists.value = playlists.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun deletePlaylist(id: Long) {
        playlists.value = playlists.value.filterNot { it.id == id }
        songsByPlaylist.value = songsByPlaylist.value - id
    }

    override suspend fun addSongs(playlistId: Long, songIds: List<Long>): Int {
        val existing = songsByPlaylist.value[playlistId].orEmpty()
        val added = songIds.filterNot { id -> existing.any { it.id == id } }.map { testSong(it) }
        songsByPlaylist.value = songsByPlaylist.value + (playlistId to existing + added)
        updateCount(playlistId)
        return added.size
    }

    override suspend fun removeSong(playlistId: Long, songId: Long) {
        val existing = songsByPlaylist.value[playlistId].orEmpty()
        songsByPlaylist.value = songsByPlaylist.value + (playlistId to existing.filterNot { it.id == songId })
        updateCount(playlistId)
    }

    override suspend fun moveSong(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val existing = songsByPlaylist.value[playlistId].orEmpty().toMutableList()
        if (fromIndex !in existing.indices || toIndex !in existing.indices) return
        existing.add(toIndex, existing.removeAt(fromIndex))
        songsByPlaylist.value = songsByPlaylist.value + (playlistId to existing.toList())
    }

    override suspend fun playlistNameExists(name: String): Boolean =
        playlists.value.any { it.name.equals(name, ignoreCase = true) }

    fun names(): List<String> = playlists.value.map { it.name }

    fun songIdsIn(playlistId: Long): List<Long> =
        songsByPlaylist.value[playlistId].orEmpty().map { it.id }

    private fun updateCount(playlistId: Long) {
        val count = songsByPlaylist.value[playlistId].orEmpty().size
        playlists.value = playlists.value.map {
            if (it.id == playlistId) it.copy(songCount = count) else it
        }
    }
}
