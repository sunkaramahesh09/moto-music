package com.motomusic.app.data.repository

import com.motomusic.app.data.local.database.dao.PlaylistAggregate
import com.motomusic.app.data.local.database.dao.PlaylistDao
import com.motomusic.app.data.local.database.entity.PlaylistEntity
import com.motomusic.app.data.local.database.entity.toDomain
import com.motomusic.app.di.IoDispatcher
import com.motomusic.app.domain.model.Playlist
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.repository.PlaylistRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> =
        playlistDao.observePlaylists().map { rows -> rows.map { it.toDomain() } }

    override fun observePlaylist(id: Long): Flow<Playlist?> =
        playlistDao.observePlaylist(id).map { it?.toDomain() }

    override fun observeSongsInPlaylist(id: Long): Flow<List<Song>> =
        playlistDao.observeSongsInPlaylist(id).map { it.toDomain() }

    override suspend fun createPlaylist(name: String): Long = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        playlistDao.insertPlaylist(PlaylistEntity(name = name.trim(), createdAt = now, updatedAt = now))
    }

    override suspend fun renamePlaylist(id: Long, name: String) = withContext(ioDispatcher) {
        playlistDao.renamePlaylist(id, name.trim(), System.currentTimeMillis())
    }

    override suspend fun deletePlaylist(id: Long) = withContext(ioDispatcher) {
        playlistDao.deletePlaylist(id)
    }

    override suspend fun addSongs(playlistId: Long, songIds: List<Long>): Int = withContext(ioDispatcher) {
        if (songIds.isEmpty()) 0 else playlistDao.appendSongs(playlistId, songIds, System.currentTimeMillis())
    }

    override suspend fun removeSong(playlistId: Long, songId: Long) = withContext(ioDispatcher) {
        playlistDao.removeSong(playlistId, songId, System.currentTimeMillis())
    }

    override suspend fun moveSong(playlistId: Long, fromIndex: Int, toIndex: Int) = withContext(ioDispatcher) {
        val ordered = playlistDao.observeSongsInPlaylist(playlistId).first().map { it.id }.toMutableList()
        if (fromIndex !in ordered.indices || toIndex !in ordered.indices || fromIndex == toIndex) {
            return@withContext
        }
        ordered.add(toIndex, ordered.removeAt(fromIndex))
        playlistDao.rewriteOrder(playlistId, ordered, System.currentTimeMillis())
    }

    override suspend fun playlistNameExists(name: String): Boolean = withContext(ioDispatcher) {
        playlistDao.countByName(name.trim()) > 0
    }

    private fun PlaylistAggregate.toDomain() = Playlist(
        id = id,
        name = name,
        songCount = songCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        artworkAlbumId = artworkAlbumId,
        artworkSongUri = artworkSongUri,
    )
}
