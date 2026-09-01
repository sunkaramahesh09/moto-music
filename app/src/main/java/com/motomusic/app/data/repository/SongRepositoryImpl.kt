package com.motomusic.app.data.repository

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.motomusic.app.core.MediaPermission
import com.motomusic.app.data.local.database.dao.AlbumAggregate
import com.motomusic.app.data.local.database.dao.ArtistAggregate
import com.motomusic.app.data.local.database.dao.FolderAggregate
import com.motomusic.app.data.local.database.dao.SQLITE_VARIABLE_CHUNK
import com.motomusic.app.data.local.database.dao.SongDao
import com.motomusic.app.data.local.database.entity.toDomain
import com.motomusic.app.data.local.database.entity.toEntity
import com.motomusic.app.data.mediastore.ArtworkLoader
import com.motomusic.app.data.mediastore.MediaStoreScanner
import com.motomusic.app.di.ApplicationScope
import com.motomusic.app.di.IoDispatcher
import com.motomusic.app.domain.model.Album
import com.motomusic.app.domain.model.Artist
import com.motomusic.app.domain.model.Folder
import com.motomusic.app.domain.model.ScanState
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.model.SortOrder
import com.motomusic.app.domain.model.UserPreferences
import com.motomusic.app.domain.repository.SettingsRepository
import com.motomusic.app.domain.repository.SongRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
@Singleton
class SongRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val scanner: MediaStoreScanner,
    private val artworkLoader: ArtworkLoader,
    private val settingsRepository: SettingsRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:ApplicationScope private val appScope: CoroutineScope,
) : SongRepository {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val scanMutex = Mutex()

    private val mediaStoreChanges = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * MediaStore notifies on every write, including the many that arrive while another app
     * copies an album, so changes are debounced into a single rescan.
     */
    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            mediaStoreChanges.tryEmit(Unit)
        }
    }

    init {
        runCatching {
            context.contentResolver.registerContentObserver(
                scanner.audioCollectionUri(),
                /* notifyForDescendants = */ true,
                contentObserver,
            )
        }.onFailure { Log.w(TAG, "Could not observe MediaStore", it) }

        mediaStoreChanges
            .debounce(MEDIA_STORE_DEBOUNCE_MS)
            .onEach { if (MediaPermission.hasAudioAccess(context)) rescan() }
            .launchIn(appScope)

        // Hiding a song, or flipping the voice-recording switch, changes what the library
        // contains, so the cache is reconciled again as soon as either changes.
        settingsRepository.preferences
            .map { it.hideVoiceRecordings to it.hiddenSongKeys }
            .distinctUntilChanged()
            .drop(1)
            .onEach { if (MediaPermission.hasAudioAccess(context)) rescan() }
            .launchIn(appScope)
    }

    override fun observeSongs(sortOrder: SortOrder): Flow<List<Song>> =
        songDao.observeSongsRaw(SimpleSQLiteQuery("SELECT * FROM songs ORDER BY ${orderByClause(sortOrder)}"))
            .map { it.toDomain() }

    override fun observeSongCount(): Flow<Int> = songDao.observeSongCount().distinctUntilChanged()

    override fun observeRecentlyAdded(limit: Int): Flow<List<Song>> =
        songDao.observeRecentlyAdded(limit).map { it.toDomain() }

    override fun observeAlbums(): Flow<List<Album>> =
        songDao.observeAlbums().map { rows -> rows.map { it.toDomain() } }

    override fun observeArtists(): Flow<List<Artist>> =
        songDao.observeArtists().map { rows -> rows.map { it.toDomain() } }

    override fun observeFolders(): Flow<List<Folder>> =
        songDao.observeFolders().map { rows -> rows.map { it.toDomain() } }

    override fun search(query: String): Flow<List<Song>> =
        songDao.search(escapeLike(query)).map { it.toDomain() }

    override fun observeSongsInAlbum(albumId: Long): Flow<List<Song>> =
        songDao.observeSongsInAlbum(albumId).map { it.toDomain() }

    override fun observeSongsByArtist(artistId: Long): Flow<List<Song>> =
        songDao.observeSongsByArtist(artistId).map { it.toDomain() }

    override fun observeAlbumsByArtist(artistId: Long): Flow<List<Album>> =
        songDao.observeAlbumsByArtist(artistId).map { rows -> rows.map { it.toDomain() } }

    override fun observeSongsInFolder(path: String): Flow<List<Song>> =
        songDao.observeSongsInFolder(path).map { it.toDomain() }

    override fun observeAlbum(albumId: Long): Flow<Album?> =
        songDao.observeAlbum(albumId).map { it?.toDomain() }

    override fun observeArtist(artistId: Long): Flow<Artist?> =
        songDao.observeArtist(artistId).map { it?.toDomain() }

    override suspend fun getSong(id: Long): Song? =
        withContext(ioDispatcher) { songDao.getSong(id)?.toDomain() }

    override suspend fun getSongs(ids: List<Long>): List<Song> = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext emptyList()
        val byId = ids.chunked(SQLITE_VARIABLE_CHUNK)
            .flatMap { songDao.getSongs(it) }
            .associateBy { it.id }
        // Preserve the caller's ordering, which is the queue order.
        ids.mapNotNull { byId[it]?.toDomain() }
    }

    /**
     * Reconciles the local cache with MediaStore. Only rows whose `date_modified` actually
     * changed are rewritten, so a rescan of a large library stays cheap and does not churn
     * every Flow observing the songs table.
     */
    override suspend fun rescan(): ScanState = scanMutex.withLock {
        withContext(ioDispatcher) {
            if (!MediaPermission.hasAudioAccess(context)) {
                val failure = ScanState.Failed("Permission to read audio files was not granted.")
                _scanState.value = failure
                return@withContext failure
            }

            _scanState.value = ScanState.Scanning(processed = 0, total = 0)
            try {
                // Read per scan, so changing either takes effect on the next one.
                val prefs = runCatching { settingsRepository.preferences.first() }
                    .getOrDefault(UserPreferences())

                val deviceSongs = scanner.querySongs(
                    excludeVoiceRecordings = prefs.hideVoiceRecordings,
                    hiddenKeys = prefs.hiddenSongKeys,
                ) { processed, total ->
                    _scanState.value = ScanState.Scanning(processed, total)
                }

                val cached = songDao.getSyncRows().associate { it.id to it.dateModifiedSec }
                val deviceIds = HashSet<Long>(deviceSongs.size)

                val changed = ArrayList<Song>()
                var added = 0
                for (song in deviceSongs) {
                    deviceIds.add(song.id)
                    val knownModified = cached[song.id]
                    when {
                        knownModified == null -> {
                            added++
                            changed += song
                        }
                        knownModified != song.dateModifiedSec -> changed += song
                    }
                }
                val updated = changed.size - added
                val removed = cached.keys.filterNot { it in deviceIds }

                changed.chunked(UPSERT_CHUNK).forEach { chunk ->
                    songDao.upsertAll(chunk.map { it.toEntity() })
                }
                if (removed.isNotEmpty()) songDao.deleteSongsAndReferences(removed)
                if (changed.isNotEmpty() || removed.isNotEmpty()) artworkLoader.clear()

                ScanState.Finished(added = added, updated = updated, removed = removed.size)
                    .also { _scanState.value = it }
            } catch (e: Exception) {
                Log.e(TAG, "Library scan failed", e)
                ScanState.Failed("Could not read the music library. Please try again.")
                    .also { _scanState.value = it }
            }
        }
    }

    override suspend fun removeMissingSong(id: Long) = withContext(ioDispatcher) {
        songDao.deleteSongsAndReferences(listOf(id))
    }

    private fun AlbumAggregate.toDomain() = Album(
        id = albumId,
        name = album,
        artist = artist,
        artistId = artistId,
        songCount = songCount,
        year = year,
        artworkSongUri = artworkSongUri,
    )

    private fun ArtistAggregate.toDomain() = Artist(
        id = artistId,
        name = name,
        songCount = songCount,
        albumCount = albumCount,
        artworkAlbumId = artworkAlbumId,
        artworkSongUri = artworkSongUri,
    )

    private fun FolderAggregate.toDomain() = Folder(
        path = path,
        name = path.substringAfterLast('/', path).ifEmpty { path },
        songCount = songCount,
    )

    companion object {
        private const val TAG = "SongRepository"
        private const val MEDIA_STORE_DEBOUNCE_MS = 2_000L
        private const val UPSERT_CHUNK = 300

        /** Whitelisted ORDER BY fragments; the sort order never comes from user text. */
        fun orderByClause(sortOrder: SortOrder): String = when (sortOrder) {
            SortOrder.TITLE_ASC -> "title COLLATE NOCASE ASC"
            SortOrder.TITLE_DESC -> "title COLLATE NOCASE DESC"
            SortOrder.ARTIST_ASC -> "artist COLLATE NOCASE ASC, album COLLATE NOCASE ASC, track_number ASC"
            SortOrder.ALBUM_ASC -> "album COLLATE NOCASE ASC, track_number ASC, title COLLATE NOCASE ASC"
            SortOrder.DATE_ADDED_DESC -> "date_added_sec DESC, id DESC"
            SortOrder.DURATION_ASC -> "duration_ms ASC, title COLLATE NOCASE ASC"
            SortOrder.DURATION_DESC -> "duration_ms DESC, title COLLATE NOCASE ASC"
        }

        /** `%` and `_` are LIKE wildcards, so a search for "50_50" must not match everything. */
        fun escapeLike(query: String): String = query.trim()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }
}
