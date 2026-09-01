package com.motomusic.app.domain.model

/**
 * A single audio track discovered through [android.provider.MediaStore].
 *
 * [id] is the MediaStore `_ID`, which is also the stable identity used by the database,
 * the playback queue and the media session, so no audio data is ever copied by the app.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNumber: Int,
    val year: Int,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val sizeBytes: Long,
    val mimeType: String,
    /** Folder the file lives in, as a display path such as `Music/Albums`. May be empty. */
    val folderPath: String,
    /** File name including extension, e.g. `03 - Song.mp3`. */
    val fileName: String,
    /** Absolute file path when the platform still exposes it, otherwise empty. */
    val filePath: String,
    val bitrateBps: Int,
    val uri: String,
) {
    val isValidDuration: Boolean get() = durationMs > 0

    /**
     * How a file is remembered when the user hides it. The path is preferred because it
     * survives MediaStore handing the same file a new id after a rescan; the URI is the
     * fallback for the platforms that no longer expose a path.
     */
    val hideKey: String get() = filePath.ifEmpty { uri }
}

const val UNKNOWN_ARTIST = "Unknown artist"
const val UNKNOWN_ALBUM = "Unknown album"
