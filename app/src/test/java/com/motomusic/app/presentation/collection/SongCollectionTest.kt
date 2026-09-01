package com.motomusic.app.presentation.collection

import org.junit.Assert.assertEquals
import org.junit.Test

class SongCollectionTest {

    @Test
    fun `route keys map to their collection`() {
        SongCollection.entries.forEach { collection ->
            assertEquals(collection, SongCollection.fromKey(collection.key))
        }
    }

    @Test
    fun `an unknown key falls back to favourites rather than crashing`() {
        assertEquals(SongCollection.FAVORITES, SongCollection.fromKey(null))
        assertEquals(SongCollection.FAVORITES, SongCollection.fromKey("nonsense"))
    }
}
