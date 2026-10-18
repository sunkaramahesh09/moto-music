package com.motomusic.app.playback

import androidx.media3.common.C
import androidx.media3.exoplayer.source.ShuffleOrder
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueShuffleOrderTest {

    /** The playlist indices in the order they would play. */
    private fun ShuffleOrder.sequence(): List<Int> {
        val result = mutableListOf<Int>()
        var index = firstIndex
        while (index != C.INDEX_UNSET) {
            result += index
            index = getNextIndex(index)
        }
        return result
    }

    private fun shuffledOf(count: Int) = QueueShuffleOrder(Random(42)).cloneAndInsert(0, count)

    @Test
    fun `a fresh queue is shuffled and contains every song once`() {
        val sequence = shuffledOf(20).sequence()
        assertEquals((0 until 20).toList(), sequence.sorted())
        assert(sequence != (0 until 20).toList()) { "expected a shuffled order" }
    }

    @Test
    fun `play next goes straight after the current song in the shuffled sequence`() {
        val order = shuffledOf(10)
        val current = order.sequence()[3]
        // PlaybackConnection.playNext inserts at current + 1 in the playlist.
        val after = order.cloneAndInsert(current + 1, 1)
        assertEquals(current + 1, after.getNextIndex(current))
    }

    @Test
    fun `add to queue goes after the last song to play`() {
        val order = shuffledOf(10)
        val last = order.lastIndex
        // PlaybackConnection.addToQueue inserts after the last song in the shuffled sequence.
        val after = order.cloneAndInsert(last + 1, 3)
        assertEquals(listOf(last + 1, last + 2, last + 3), after.sequence().takeLast(3))
    }

    @Test
    fun `inserting at the start of the playlist puts songs first`() {
        val after = shuffledOf(5).cloneAndInsert(0, 1)
        assertEquals(0, after.firstIndex)
    }

    @Test
    fun `songs already queued keep their relative order around an insertion`() {
        val order = shuffledOf(8)
        val before = order.sequence()
        val after = order.cloneAndInsert(4, 2).sequence()
            .filter { it !in 4..5 }
            .map { if (it > 5) it - 2 else it }
        assertEquals(before, after)
    }

    @Test
    fun `removing songs keeps the rest in order`() {
        val order = shuffledOf(8)
        val expected = order.sequence().filter { it !in 2..4 }.map { if (it > 4) it - 3 else it }
        assertEquals(expected, order.cloneAndRemove(2, 5).sequence())
        assertEquals(C.INDEX_UNSET, order.cloneAndRemove(0, 8).firstIndex)
    }
}
