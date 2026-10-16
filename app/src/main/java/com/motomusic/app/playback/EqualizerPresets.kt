package com.motomusic.app.playback

import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * A preset is a gain curve, not a list of band levels.
 *
 * Phones expose anything from five to ten equalizer bands at frequencies of the vendor's
 * choosing, so a preset is described at ten fixed reference frequencies and interpolated onto
 * whatever bands the device actually has (see [levelsFor]).
 */
enum class EqualizerPreset(val key: String, val label: String, private val gainsDb: FloatArray?) {
    FLAT("flat", "Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("bass", "Bass boost", floatArrayOf(6f, 5f, 4f, 2f, 0.5f, 0f, 0f, 0f, 0f, 0f)),
    TREBLE_BOOST("treble", "Treble boost", floatArrayOf(0f, 0f, 0f, 0f, 0f, 1f, 2.5f, 4f, 5f, 6f)),
    VOCAL("vocal", "Vocal", floatArrayOf(-2f, -2f, -1f, 1f, 3f, 4f, 3f, 1.5f, 0f, -1f)),
    ROCK("rock", "Rock", floatArrayOf(5f, 4f, 3f, 1f, -1f, -1f, 1f, 3f, 4f, 5f)),
    POP("pop", "Pop", floatArrayOf(-1f, 0f, 2f, 3.5f, 4f, 3f, 1f, 0f, -1f, -1f)),
    JAZZ("jazz", "Jazz", floatArrayOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f)),
    CLASSICAL("classical", "Classical", floatArrayOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f)),
    ELECTRONIC("electronic", "Electronic", floatArrayOf(5f, 4f, 1f, 0f, -2f, 1f, 0.5f, 2f, 4f, 5f)),
    HIP_HOP("hiphop", "Hip-hop", floatArrayOf(6f, 5f, 2f, 3f, -1f, -1f, 1f, 0f, 2f, 3f)),
    ACOUSTIC("acoustic", "Acoustic", floatArrayOf(4f, 3f, 2f, 1f, 1.5f, 1.5f, 3f, 3f, 2f, 1f)),
    LATE_NIGHT("night", "Late night", floatArrayOf(-3f, -2f, -1f, 0f, 1f, 2f, 2f, 1f, 0f, -1f)),

    /** The user's own curve, stored per band in the settings. */
    CUSTOM("custom", "Custom", null);

    /** Band levels in millibels for bands centred on [centersHz], clamped to the device range. */
    fun levelsFor(centersHz: List<Int>, minMb: Int, maxMb: Int): List<Int> {
        val gains = gainsDb ?: return centersHz.map { 0 }
        return centersHz.map { hz ->
            (interpolateDb(gains, hz) * 100f).roundToInt().coerceIn(minMb, maxMb)
        }
    }

    companion object {
        /** The presets offered as chips; [CUSTOM] is reached by moving a slider. */
        val builtIn: List<EqualizerPreset> = entries.filter { it != CUSTOM }

        fun fromKey(key: String?): EqualizerPreset = entries.firstOrNull { it.key == key } ?: FLAT

        private val REFERENCE_HZ = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f, 16_000f)

        /** Straight-line interpolation on a log-frequency axis, which is how hearing spaces pitch. */
        private fun interpolateDb(gains: FloatArray, hz: Int): Float {
            val x = log2(hz.coerceAtLeast(1).toFloat())
            val first = log2(REFERENCE_HZ.first())
            val last = log2(REFERENCE_HZ.last())
            if (x <= first) return gains.first()
            if (x >= last) return gains.last()
            for (i in 0 until REFERENCE_HZ.lastIndex) {
                val lo = log2(REFERENCE_HZ[i])
                val hi = log2(REFERENCE_HZ[i + 1])
                if (x <= hi) {
                    val t = (x - lo) / (hi - lo)
                    return gains[i] + (gains[i + 1] - gains[i]) * t
                }
            }
            return gains.last()
        }
    }
}
