package com.motomusic.app.playback

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.motomusic.app.domain.model.Song

/**
 * Translation between library rows and the media session's [MediaItem]s.
 *
 * Media3 strips a MediaItem's playback URI when it crosses the session boundary, so the URI is
 * also carried in [MediaItem.RequestMetadata] and restored by the service in
 * `MediaSession.Callback.onAddMediaItems`.
 */
object MediaItemMapper {

    const val EXTRA_ALBUM_ID = "moto.album_id"
    const val EXTRA_DURATION_MS = "moto.duration_ms"
    const val EXTRA_SONG_URI = "moto.song_uri"

    private val ALBUM_ART_BASE_URI: Uri = "content://media/external/audio/albumart".toUri()

    fun albumArtUri(albumId: Long): Uri? =
        if (albumId > 0) ContentUris.withAppendedId(ALBUM_ART_BASE_URI, albumId) else null

    fun toMediaItem(song: Song): MediaItem {
        val songUri = song.uri.toUri()
        val extras = Bundle().apply {
            putLong(EXTRA_ALBUM_ID, song.albumId)
            putLong(EXTRA_DURATION_MS, song.durationMs)
            putString(EXTRA_SONG_URI, song.uri)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setAlbumArtist(song.artist)
            .setTrackNumber(song.trackNumber.takeIf { it > 0 })
            .setRecordingYear(song.year.takeIf { it > 0 })
            .setArtworkUri(albumArtUri(song.albumId))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(songUri)
            .setMediaMetadata(metadata)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(songUri)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    fun toMediaItems(songs: List<Song>): List<MediaItem> = songs.map(::toMediaItem)

    /** Everything the player UI needs about one queue entry, without touching the database. */
    fun toTrackInfo(item: MediaItem): TrackInfo {
        val metadata = item.mediaMetadata
        val extras = metadata.extras ?: item.requestMetadata.extras
        return TrackInfo(
            songId = item.mediaId.toLongOrNull() ?: -1L,
            title = metadata.title?.toString().orEmpty().ifEmpty { "Unknown title" },
            artist = metadata.artist?.toString().orEmpty(),
            album = metadata.albumTitle?.toString().orEmpty(),
            albumId = extras?.getLong(EXTRA_ALBUM_ID) ?: 0L,
            songUri = extras?.getString(EXTRA_SONG_URI)
                ?: item.requestMetadata.mediaUri?.toString(),
            durationMs = extras?.getLong(EXTRA_DURATION_MS) ?: 0L,
        )
    }
}

/** Immutable snapshot of a queue entry, safe to hold in Compose state. */
data class TrackInfo(
    val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val songUri: String?,
    val durationMs: Long,
)
