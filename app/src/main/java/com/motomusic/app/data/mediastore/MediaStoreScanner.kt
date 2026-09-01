package com.motomusic.app.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.model.UNKNOWN_ALBUM
import com.motomusic.app.domain.model.UNKNOWN_ARTIST
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the device's audio library out of [MediaStore].
 *
 * MediaStore is the only supported way to see the user's music without asking for broad
 * filesystem access, and it already contains the tags the scanner needs, so no file is
 * opened or parsed here. Per-file work (sample rate, embedded artwork) happens lazily and
 * only for the one song the user is looking at.
 */
@Singleton
class MediaStoreScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun audioCollectionUri(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    /**
     * Streams every music track on the device. [onProgress] is invoked as rows are read so
     * callers can show scan progress without waiting for the whole library.
     *
     * With [excludeVoiceRecordings] set, files that live in a messaging or recorder folder are
     * dropped as they are read — MediaStore marks WhatsApp voice notes as music, so without
     * this the library fills up with them. [hiddenKeys] drops the individual files the user
     * hid by hand, which is the only way to catch a recording that was moved somewhere its
     * folder no longer gives it away.
     */
    fun querySongs(
        excludeVoiceRecordings: Boolean = false,
        hiddenKeys: Set<String> = emptySet(),
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> },
    ): List<Song> {
        val collection = audioCollectionUri()
        val projection = buildProjection()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val songs = ArrayList<Song>(256)
        val cursor: Cursor? = try {
            context.contentResolver.query(collection, projection, selection, null, null)
        } catch (e: SecurityException) {
            // Permission was revoked between the check and the query.
            Log.w(TAG, "Media permission missing while scanning", e)
            return emptyList()
        } catch (e: RuntimeException) {
            Log.w(TAG, "MediaStore query failed", e)
            return emptyList()
        }

        cursor?.use { c ->
            val total = c.count
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val artistIdIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val albumIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val addedIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val modifiedIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val sizeIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val displayNameIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataIdx = c.getColumnIndex(MEDIA_DATA_COLUMN)
            val relativePathIdx = c.getColumnIndex(RELATIVE_PATH_COLUMN)
            val bitrateIdx = c.getColumnIndex(BITRATE_COLUMN)
            val recordingIdx = c.getColumnIndex(IS_RECORDING_COLUMN)

            var processed = 0
            var skipped = 0
            while (c.moveToNext()) {
                processed++
                val row = runCatching {
                    val id = c.getLong(idIdx)
                    val fileName = c.getStringOrEmpty(displayNameIdx)
                    val filePath = if (dataIdx >= 0) c.getStringOrEmpty(dataIdx) else ""
                    val folder = resolveFolder(
                        relativePath = if (relativePathIdx >= 0) c.getStringOrNull(relativePathIdx) else null,
                        filePath = filePath,
                    )
                    if (excludeVoiceRecordings &&
                        isVoiceRecording(
                            filePath = filePath,
                            folderPath = folder,
                            flaggedByMediaStore = recordingIdx >= 0 && c.getInt(recordingIdx) != 0,
                        )
                    ) {
                        skipped++
                        return@runCatching null
                    }
                    val songUri = ContentUris.withAppendedId(collection, id).toString()
                    if (filePath.ifEmpty { songUri } in hiddenKeys) {
                        skipped++
                        return@runCatching null
                    }
                    Song(
                        id = id,
                        title = resolveTitle(c.getStringOrNull(titleIdx), fileName, filePath),
                        artist = normaliseTag(c.getStringOrNull(artistIdx), UNKNOWN_ARTIST),
                        artistId = c.getLong(artistIdIdx),
                        album = normaliseTag(c.getStringOrNull(albumIdx), UNKNOWN_ALBUM),
                        albumId = c.getLong(albumIdIdx),
                        durationMs = c.getLong(durationIdx).coerceAtLeast(0L),
                        trackNumber = normaliseTrackNumber(c.getInt(trackIdx)),
                        year = c.getInt(yearIdx),
                        dateAddedSec = c.getLong(addedIdx),
                        dateModifiedSec = c.getLong(modifiedIdx),
                        sizeBytes = c.getLong(sizeIdx),
                        mimeType = c.getStringOrEmpty(mimeIdx),
                        folderPath = folder,
                        fileName = fileName.ifEmpty { filePath.substringAfterLast('/') },
                        filePath = filePath,
                        bitrateBps = if (bitrateIdx >= 0) c.getInt(bitrateIdx) else 0,
                        uri = songUri,
                    )
                }.onFailure { Log.w(TAG, "Skipping unreadable MediaStore row", it) }

                row.getOrNull()?.let(songs::add)

                if (processed % PROGRESS_INTERVAL == 0) onProgress(processed, total)
            }
            onProgress(total, total)
            if (skipped > 0) Log.d(TAG, "Left out $skipped hidden or voice-recording file(s)")
        }
        return songs
    }

    private fun buildProjection(): Array<String> = buildList {
        add(MediaStore.Audio.Media._ID)
        add(MediaStore.Audio.Media.TITLE)
        add(MediaStore.Audio.Media.ARTIST)
        add(MediaStore.Audio.Media.ARTIST_ID)
        add(MediaStore.Audio.Media.ALBUM)
        add(MediaStore.Audio.Media.ALBUM_ID)
        add(MediaStore.Audio.Media.DURATION)
        add(MediaStore.Audio.Media.TRACK)
        add(MediaStore.Audio.Media.YEAR)
        add(MediaStore.Audio.Media.DATE_ADDED)
        add(MediaStore.Audio.Media.DATE_MODIFIED)
        add(MediaStore.Audio.Media.SIZE)
        add(MediaStore.Audio.Media.MIME_TYPE)
        add(MediaStore.Audio.Media.DISPLAY_NAME)
        add(MEDIA_DATA_COLUMN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(RELATIVE_PATH_COLUMN)
            add(BITRATE_COLUMN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) add(IS_RECORDING_COLUMN)
    }.toTypedArray()

    private fun Cursor.getStringOrNull(index: Int): String? =
        if (index >= 0 && !isNull(index)) getString(index) else null

    private fun Cursor.getStringOrEmpty(index: Int): String = getStringOrNull(index).orEmpty()

    companion object {
        private const val TAG = "MediaStoreScanner"
        private const val PROGRESS_INTERVAL = 100

        // Referenced by name so the deprecated DATA constant is not used directly.
        private const val MEDIA_DATA_COLUMN = "_data"
        private const val RELATIVE_PATH_COLUMN = "relative_path"
        private const val BITRATE_COLUMN = "bitrate"
        private const val IS_RECORDING_COLUMN = "is_recording"

        /**
         * Directories that hold speech rather than music. WhatsApp files are the reason this
         * exists: MediaStore sets `is_music` on every voice note and every audio clip received
         * in a chat, so they arrive looking exactly like songs.
         */
        private val VOICE_PATH_MARKERS = listOf(
            "/android/media/com.whatsapp",
            "/whatsapp/media/whatsapp audio/",
            "/whatsapp/media/whatsapp voice notes/",
            "/recordings/",
            "/recorder/",
            "/records/",
            "/voice notes/",
            "/voice recorder/",
            "/voicerecorder/",
            "/sound recorder/",
            "/soundrecorder/",
            "/call recordings/",
            "/callrecordings/",
        )

        /** MediaStore writes this literal when a file carries no artist/album tag. */
        private const val MEDIASTORE_UNKNOWN = "<unknown>"

        fun normaliseTag(raw: String?, fallback: String): String {
            val value = raw?.trim().orEmpty()
            return if (value.isEmpty() || value == MEDIASTORE_UNKNOWN) fallback else value
        }

        /**
         * Falls back to a readable version of the file name when the file carries no title tag,
         * stripping the extension and a leading track number such as `03 - `.
         */
        fun resolveTitle(rawTitle: String?, fileName: String, filePath: String): String {
            val title = rawTitle?.trim().orEmpty()
            if (title.isNotEmpty() && title != MEDIASTORE_UNKNOWN) return title

            val name = fileName.ifEmpty { filePath.substringAfterLast('/') }
            val withoutExtension = name.substringBeforeLast('.', name)
            val cleaned = withoutExtension
                .replace(Regex("^\\d{1,3}\\s*[-._)]\\s*"), "")
                .replace('_', ' ')
                .trim()
            return cleaned.ifEmpty { "Unknown title" }
        }

        /**
         * MediaStore sometimes encodes the track as `disc * 1000 + track` (e.g. 1005 for
         * disc 1, track 5); reduce it to the plain track number used for album ordering.
         */
        fun normaliseTrackNumber(raw: Int): Int = when {
            raw <= 0 -> 0
            raw > 1000 -> raw % 1000
            else -> raw
        }

        /**
         * Whether a file is speech rather than music, judged by where it lives plus
         * MediaStore's own `is_recording` flag (Android 11+, and only ever set for the
         * system Recordings directory).
         */
        fun isVoiceRecording(
            filePath: String,
            folderPath: String,
            flaggedByMediaStore: Boolean,
        ): Boolean {
            if (flaggedByMediaStore) return true
            val path = filePath.ifEmpty { folderPath }
            if (path.isEmpty()) return false
            val haystack = "/" + path.lowercase().trim('/') + "/"
            return VOICE_PATH_MARKERS.any { it in haystack }
        }

        /** Prefers the Q+ `relative_path` column and falls back to the parent directory. */
        fun resolveFolder(relativePath: String?, filePath: String): String {
            val relative = relativePath?.trim()?.trim('/').orEmpty()
            if (relative.isNotEmpty()) return relative
            if (filePath.isEmpty()) return ""
            return filePath.substringBeforeLast('/', "").trim('/')
        }
    }
}
