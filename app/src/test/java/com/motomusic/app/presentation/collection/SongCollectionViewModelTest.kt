package com.motomusic.app.presentation.collection

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.motomusic.app.domain.repository.FavoritesRepository
import com.motomusic.app.domain.repository.PlaybackHistoryRepository
import com.motomusic.app.domain.repository.SongRepository
import com.motomusic.app.navigation.Routes
import com.motomusic.app.util.MainDispatcherRule
import com.motomusic.app.util.testSong
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The four home shortcuts share one screen, so the route argument has to pick the right query. */
@OptIn(ExperimentalCoroutinesApi::class)
class SongCollectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val songRepository = mockk<SongRepository>(relaxed = true) {
        every { observeRecentlyAdded(any()) } returns flowOf(listOf(testSong(4)))
    }
    private val favoritesRepository = mockk<FavoritesRepository>(relaxed = true) {
        every { observeFavorites() } returns flowOf(listOf(testSong(1)))
    }
    private val historyRepository = mockk<PlaybackHistoryRepository>(relaxed = true) {
        every { observeRecentlyPlayed(any()) } returns flowOf(listOf(testSong(2)))
        every { observeMostPlayed(any()) } returns flowOf(listOf(testSong(3)))
    }

    private fun viewModelFor(type: String) = SongCollectionViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_COLLECTION_TYPE to type)),
        songRepository = songRepository,
        favoritesRepository = favoritesRepository,
        historyRepository = historyRepository,
    )

    @Test
    fun `each collection reads from its own source`() = runTest(mainDispatcherRule.testDispatcher) {
        val expected = mapOf(
            "favorites" to 1L,
            "recently_played" to 2L,
            "most_played" to 3L,
            "recently_added" to 4L,
        )

        expected.forEach { (type, songId) ->
            viewModelFor(type).state.test {
                // The first value is the empty placeholder emitted before the query lands.
                val loaded = awaitItem().takeIf { !it.isLoading } ?: awaitItem()
                assertEquals(listOf(songId), loaded.songs.map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `an unknown collection type falls back to favourites`() {
        assertEquals(SongCollection.FAVORITES, viewModelFor("nonsense").collection)
    }
}
