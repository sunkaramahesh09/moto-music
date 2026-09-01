package com.motomusic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `durations under an hour are minutes and seconds`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:07", formatDuration(7_000))
        assertEquals("3:45", formatDuration(225_000))
        assertEquals("59:59", formatDuration(3_599_000))
    }

    @Test
    fun `durations of an hour or more gain an hours field`() {
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("2:05:03", formatDuration(7_503_000))
    }

    @Test
    fun `negative and zero durations do not produce nonsense`() {
        assertEquals("0:00", formatDuration(-1))
    }

    @Test
    fun `file sizes step through units`() {
        assertEquals("Unknown", formatFileSize(0))
        assertEquals("512 B", formatFileSize(512))
        assertEquals("1.0 KB", formatFileSize(1024))
        assertEquals("4.8 MB", formatFileSize(5_033_165))
    }

    @Test
    fun `bitrate is reported in kbps and skipped when unknown`() {
        assertEquals("320 kbps", formatBitrate(320_000))
        assertNull(formatBitrate(0))
        assertNull(formatBitrate(null))
    }

    @Test
    fun `sample rate is reported in kHz`() {
        assertEquals("44.1 kHz", formatSampleRate(44_100))
        assertNull(formatSampleRate(null))
    }

    @Test
    fun `total duration reads as words`() {
        assertEquals("under a minute", formatTotalDuration(30_000))
        assertEquals("48 min", formatTotalDuration(48 * 60_000L))
        assertEquals("1 hr", formatTotalDuration(60 * 60_000L))
        assertEquals("1 hr 12 min", formatTotalDuration(72 * 60_000L))
        assertEquals("2 hrs 1 min", formatTotalDuration(121 * 60_000L))
    }

    @Test
    fun `pluralise only adds an s when it should`() {
        assertEquals("1 song", pluralise(1, "song"))
        assertEquals("0 songs", pluralise(0, "song"))
        assertEquals("3 songs", pluralise(3, "song"))
    }
}
