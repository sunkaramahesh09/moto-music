package com.motomusic.app.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import kotlin.random.Random

/**
 * A shuffle order that respects "Play next" and "Add to queue".
 *
 * Media3's default order drops every inserted song at a random place in the shuffled sequence,
 * so with shuffle on, a song queued to play next might play tenth or last. Here a fresh queue is
 * shuffled as usual, but songs inserted into an existing one are placed in the shuffled sequence
 * directly after the song that precedes them in the playlist, in the order they were given.
 * [PlaybackConnection] picks that playlist position on purpose: right after the current song for
 * "Play next", right after the last song to be played for "Add to queue". Songs inserted at the
 * very start of the playlist go to the start of the shuffled sequence.
 *
 * With shuffle off the player ignores this order entirely.
 */
@OptIn(UnstableApi::class)
class QueueShuffleOrder private constructor(
    private val shuffled: IntArray,
    private val random: Random,
) : ShuffleOrder {

    constructor(random: Random = Random.Default) : this(IntArray(0), random)

    /** Where each playlist index sits in [shuffled]. */
    private val positionOf = IntArray(shuffled.size).also { positions ->
        shuffled.forEachIndexed { position, index -> positions[index] = position }
    }

    override fun getLength(): Int = shuffled.size

    override fun getNextIndex(index: Int): Int =
        positionOf[index].let { if (it + 1 < shuffled.size) shuffled[it + 1] else C.INDEX_UNSET }

    override fun getPreviousIndex(index: Int): Int =
        positionOf[index].let { if (it > 0) shuffled[it - 1] else C.INDEX_UNSET }

    override fun getLastIndex(): Int = shuffled.lastOrNull() ?: C.INDEX_UNSET

    override fun getFirstIndex(): Int = shuffled.firstOrNull() ?: C.INDEX_UNSET

    override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
        if (insertionCount == 0) return QueueShuffleOrder(shuffled.copyOf(), random)
        val inserted = IntArray(insertionCount) { insertionIndex + it }
        if (shuffled.isEmpty()) {
            inserted.shuffle(random)
            return QueueShuffleOrder(inserted, random)
        }
        val at = if (insertionIndex == 0) 0 else positionOf[insertionIndex - 1] + 1
        val shifted = shuffled.map { if (it >= insertionIndex) it + insertionCount else it }
        val result = shifted.subList(0, at) + inserted.asList() + shifted.subList(at, shifted.size)
        return QueueShuffleOrder(result.toIntArray(), random)
    }

    override fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder {
        val removed = indexToExclusive - indexFrom
        val result = shuffled
            .filter { it < indexFrom || it >= indexToExclusive }
            .map { if (it >= indexToExclusive) it - removed else it }
        return QueueShuffleOrder(result.toIntArray(), random)
    }

    override fun cloneAndClear(): ShuffleOrder = QueueShuffleOrder(random)
}
