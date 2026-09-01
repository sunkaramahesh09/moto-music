package com.motomusic.app.presentation.common

import com.motomusic.app.util.testSong
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionSummaryTest {

    @Test
    fun `an empty collection says so instead of showing a zero`() {
        assertEquals("No songs", songsSummary(emptyList()))
    }

    @Test
    fun `the summary counts songs and adds their total time`() {
        val songs = listOf(
            testSong(1, durationMs = 4 * 60_000L),
            testSong(2, durationMs = 6 * 60_000L),
        )
        assertEquals("2 songs · 10 min", songsSummary(songs))
    }

    @Test
    fun `a single song is not pluralised`() {
        assertEquals("1 song · 3 min", songsSummary(listOf(testSong(1))))
    }
}
