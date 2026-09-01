package com.motomusic.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SortOrderTest {

    @Test
    fun `every sort order round-trips through its storage key`() {
        SortOrder.entries.forEach { order ->
            assertEquals(order, SortOrder.fromStorageKey(order.storageKey))
        }
    }

    @Test
    fun `unknown and missing keys fall back to title`() {
        assertEquals(SortOrder.TITLE_ASC, SortOrder.fromStorageKey(null))
        assertEquals(SortOrder.TITLE_ASC, SortOrder.fromStorageKey("who_knows"))
    }

    @Test
    fun `theme modes round-trip too`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStorageKey(mode.storageKey))
        }
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey("neon"))
    }
}
