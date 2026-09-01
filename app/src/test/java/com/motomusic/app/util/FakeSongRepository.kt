package com.motomusic.app.util

import com.motomusic.app.domain.model.Album
import com.motomusic.app.domain.model.Artist
import com.motomusic.app.domain.model.Folder
import com.motomusic.app.domain.model.ScanState
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.model.SortOrder
import com.motomusic.app.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory library. Every query records its argument, so a test can assert not just
 * *what* the ViewModel showed but *how many times* it went back to the repository —
 * which is the whole point of the search debounce.
 */
class FakeSongRepository(
    songs: List<Song> = emptyList(),
) : SongRepository {

    val library = MutableStateFlow(songs)

    val searchQueries = mutableListOf<String>()
    val requestedSortOrders = mutableListOf<SortOrder>()
    var rescanCount = 0
        private set

    override val scanState = MutableStateFlow<ScanState>(ScanState.Idle)

    override fun observeSongs(sortOrder: SortOrder): Flow<List<Song>> {
        requestedSortOrders += sortOrder
        return library.map { songs -> songs.sortedWith(comparatorFor(sortOrder)) }
    }

    override fun observeSongCount(): Flow<Int> = library.map { it.size }

    override fun observeAlbums(): Flow<List<Album>> = library.map { emptyList() }

    override fun observeArtists(): Flow<List<Artist>> = library.map { emptyList() }

    override fun observeFolders(): Flow<List<Folder>> = library.map { emptyList() }

    override fun observeRecentlyAdded(limit: Int): Flow<List<Song>> =
        library.map { songs -> songs.sortedByDescending { it.dateAddedSec }.take(limit) }

    override fun search(query: String): Flow<List<Song>> {
        searchQueries += query
        return library.map { songs ->
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
            }
        }
    }

    override fun observeSongsInAlbum(albumId: Long): Flow<List<Song>> =
        library.map { songs -> songs.filter { it.albumId == albumId } }

    override fun observeSongsByArtist(artistId: Long): Flow<List<Song>> =
        library.map { songs -> songs.filter { it.artistId == artistId } }

    override fun observeAlbumsByArtist(artistId: Long): Flow<List<Album>> = library.map { emptyList() }

    override fun observeSongsInFolder(path: String): Flow<List<Song>> =
        library.map { songs -> songs.filter { it.folderPath == path } }

    override fun observeAlbum(albumId: Long): Flow<Album?> = library.map { null }

    override fun observeArtist(artistId: Long): Flow<Artist?> = library.map { null }

    override suspend fun getSong(id: Long): Song? = library.value.firstOrNull { it.id == id }

    override suspend fun getSongs(ids: List<Long>): List<Song> {
        val byId = library.value.associateBy { it.id }
        return ids.mapNotNull(byId::get)
    }

    override suspend fun rescan(): ScanState {
        rescanCount++
        return ScanState.Finished(added = 0, updated = 0, removed = 0)
    }

    override suspend fun removeMissingSong(id: Long) {
        library.value = library.value.filterNot { it.id == id }
    }

    private fun comparatorFor(order: SortOrder): Comparator<Song> = when (order) {
        SortOrder.TITLE_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER, Song::title)
        SortOrder.TITLE_DESC -> compareByDescending(String.CASE_INSENSITIVE_ORDER, Song::title)
        SortOrder.ARTIST_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER, Song::artist)
        SortOrder.ALBUM_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER, Song::album)
        SortOrder.DATE_ADDED_DESC -> compareByDescending(Song::dateAddedSec)
        SortOrder.DURATION_ASC -> compareBy(Song::durationMs)
        SortOrder.DURATION_DESC -> compareByDescending(Song::durationMs)
    }
}
