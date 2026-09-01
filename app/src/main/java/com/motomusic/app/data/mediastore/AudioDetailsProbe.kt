package com.motomusic.app.data.mediastore

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Technical details that MediaStore does not index, read on demand for one song at a time. */
data class AudioDetails(
    val sampleRateHz: Int?,
    val channelCount: Int?,
    val bitrateBps: Int?,
)

@Singleton
class AudioDetailsProbe @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** Never throws: a corrupt or deleted file simply yields no extra details. */
    fun probe(uri: String): AudioDetails {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, uri.toUri(), null)
            val audioTrack = (0 until extractor.trackCount)
                .map { extractor.getTrackFormat(it) }
                .firstOrNull { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?: return AudioDetails(null, null, null)

            AudioDetails(
                sampleRateHz = audioTrack.intOrNull(MediaFormat.KEY_SAMPLE_RATE),
                channelCount = audioTrack.intOrNull(MediaFormat.KEY_CHANNEL_COUNT),
                bitrateBps = audioTrack.intOrNull(MediaFormat.KEY_BIT_RATE),
            )
        } catch (e: Exception) {
            Log.d(TAG, "Could not probe $uri: ${e.message}")
            AudioDetails(null, null, null)
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun MediaFormat.intOrNull(key: String): Int? =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null

    private companion object {
        const val TAG = "AudioDetailsProbe"
    }
}
