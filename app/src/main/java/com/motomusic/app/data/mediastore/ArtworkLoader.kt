package com.motomusic.app.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Loads and caches album artwork.
 *
 * Lists ask for small bitmaps and the player asks for a large one, so decoding is bucketed by
 * size: a scrolling list of 10,000 songs shares at most a handful of decoded albums. Bitmaps are
 * always subsampled while decoding, so a full-resolution cover is never held in memory for a row
 * that is 56dp tall. Albums with no artwork are remembered too, otherwise every rebind would pay
 * for another failed decode.
 */
@Singleton
class ArtworkLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val cache = object : LruCache<String, Bitmap>(cacheSizeBytes()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val missing = LruCache<String, Boolean>(MISS_CACHE_ENTRIES)
    private val inFlight = HashMap<String, Deferred<Bitmap?>>()
    private val lock = Mutex()

    fun cached(albumId: Long, songUri: String?, sizePx: Int): Bitmap? =
        cache.get(key(albumId, songUri, bucket(sizePx)))

    /** Returns the artwork for [albumId], decoding it at most once per size bucket. */
    suspend fun load(albumId: Long, songUri: String?, sizePx: Int): Bitmap? {
        val size = bucket(sizePx)
        val key = key(albumId, songUri, size)
        cache.get(key)?.let { return it }
        if (missing.get(key) == true) return null

        val deferred = lock.withLock {
            inFlight[key] ?: scope.async {
                val bitmap = decode(albumId, songUri, size)
                if (bitmap != null) cache.put(key, bitmap) else missing.put(key, true)
                lock.withLock { inFlight.remove(key) }
                bitmap
            }.also { inFlight[key] = it }
        }
        return runCatching { deferred.await() }.getOrNull()
    }

    private fun decode(albumId: Long, songUri: String?, size: Int): Bitmap? {
        if (albumId > 0) {
            decodeFromAlbumArt(albumId, size)?.let { return it }
        }
        // Files whose album has no MediaStore thumbnail can still carry an embedded cover.
        return songUri?.let { decodeEmbedded(it, size) }
    }

    private fun decodeFromAlbumArt(albumId: Long, size: Int): Bitmap? {
        val uri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, size)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            openStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: Exception) {
            Log.d(TAG, "No MediaStore artwork for album $albumId: ${e.message}")
            null
        }
    }

    private fun decodeEmbedded(songUri: String, size: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, songUri.toUri())
            val bytes = retriever.embeddedPicture ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, size)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            Log.d(TAG, "No embedded artwork in $songUri: ${e.message}")
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun openStream(uri: Uri): InputStream? = context.contentResolver.openInputStream(uri)

    /** Drops cached bitmaps; used after a rescan so replaced files do not show stale covers. */
    fun clear() {
        cache.evictAll()
        missing.evictAll()
    }

    companion object {
        private const val TAG = "ArtworkLoader"
        private const val MISS_CACHE_ENTRIES = 512

        /** MediaStore's album thumbnail provider; still the cheapest source of cover art. */
        private val ALBUM_ART_URI: Uri = "content://media/external/audio/albumart".toUri()

        /** Decoding is rounded up to one of these widths so caches are shared between rows. */
        private val SIZE_BUCKETS = intArrayOf(128, 256, 512, 1024)

        private fun cacheSizeBytes(): Int {
            val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            return (maxMemory / 6) * 1024
        }

        fun bucket(sizePx: Int): Int = SIZE_BUCKETS.firstOrNull { it >= sizePx } ?: SIZE_BUCKETS.last()

        /**
         * Album id is the sharing key, so every track of an album decodes one cover. Files that
         * MediaStore gave no album fall back to their own URI: keying those on the album id would
         * make every album-less song on the device share one cache entry, so the first one to be
         * decoded would supply the artwork for all of them.
         */
        private fun key(albumId: Long, songUri: String?, size: Int) =
            if (albumId > 0) "album:$albumId@$size" else "song:${songUri.orEmpty()}@$size"

        fun sampleSize(width: Int, height: Int, target: Int): Int {
            var sample = 1
            var halfWidth = width / 2
            var halfHeight = height / 2
            while (halfWidth / sample >= target && halfHeight / sample >= target) sample *= 2
            return sample
        }
    }
}
