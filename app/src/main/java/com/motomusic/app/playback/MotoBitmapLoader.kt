package com.motomusic.app.playback

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.motomusic.app.data.mediastore.ArtworkLoader
import com.motomusic.app.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Supplies artwork for the media notification and lock screen.
 *
 * Media3's default loader can only open the artwork URI. Reusing [ArtworkLoader] means the
 * notification also gets covers that are only embedded in the audio file, and it shares the
 * bitmap cache the library screens already filled.
 */
@OptIn(UnstableApi::class)
@Singleton
class MotoBitmapLoader @Inject constructor(
    private val artworkLoader: ArtworkLoader,
    @param:ApplicationScope private val scope: CoroutineScope,
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            val bitmap = runCatching { BitmapFactory.decodeByteArray(data, 0, data.size) }.getOrNull()
            if (bitmap != null) {
                future.set(bitmap)
            } else {
                future.setException(IllegalArgumentException("Artwork could not be decoded"))
            }
        }
        return future
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val albumId = uri.lastPathSegment?.toLongOrNull() ?: -1L
        return loadInternal(albumId, songUri = null)
    }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        val extras = metadata.extras
        val albumId = extras?.getLong(MediaItemMapper.EXTRA_ALBUM_ID) ?: 0L
        val songUri = extras?.getString(MediaItemMapper.EXTRA_SONG_URI)
        if (albumId <= 0L && songUri == null) {
            return metadata.artworkUri?.let { loadBitmap(it) }
        }
        return loadInternal(albumId, songUri)
    }

    private fun loadInternal(albumId: Long, songUri: String?): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            val bitmap = runCatching { artworkLoader.load(albumId, songUri, NOTIFICATION_ARTWORK_PX) }
                .getOrNull()
            if (bitmap != null) {
                future.set(bitmap)
            } else {
                // Media3 treats a failed future as "no artwork" and shows its own placeholder.
                // It logs a warning each time and caches this future against the metadata, so a
                // track with no cover produces a short burst of "Failed to load bitmap" warnings
                // and then none ever again -- it never asks a second time. Answering null instead
                // is not possible here: we only learn there is no artwork after the I/O.
                future.setException(NoSuchElementException("No artwork for album $albumId"))
            }
        }
        return future
    }

    private companion object {
        /** Large enough for the lock screen without decoding a full-resolution cover. */
        const val NOTIFICATION_ARTWORK_PX = 512
    }
}
