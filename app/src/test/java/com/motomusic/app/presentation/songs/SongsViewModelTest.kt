package com.motomusic.app.presentation.songs

import app.cash.turbine.test
import com.motomusic.app.domain.model.SortOrder
import com.motomusic.app.util.FakeSettingsRepository
import com.motomusic.app.util.FakeSongRepository
import com.motomusic.app.util.MainDispatcherRule
import com.motomusic.app.util.testSong
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The Songs screen is the one place where typing hits the database on every keystroke,
 * so these tests are mostly about the debounce doing its job.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SongsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val songs = listOf(
        testSong(1, title = "Glass Avenue", artist = "Neon Harbour", durationMs = 200_000),
        testSong(2, title = "Rainhold", artist = "Ilse Vega", durationMs = 100_000),
        testSong(3, title = "Desert Radio", artist = "Kaveh Roy", durationMs = 300_000),
    )

    private val songRepository = FakeSongRepository(songs)
    private val settingsRepository = FakeSettingsRepository()

    // Built lazily: constructing it eagerly would capture the real main dispatcher,
    // because JUnit applies the rule after the test class is instantiated.
    private val viewModel by lazy { SongsViewModel(songRepository, settingsRepository) }

    @Test
    fun `library is sorted by the stored sort order`() = runTest(mainDispatcherRule.testDispatcher) {
        settingsRepository.state.value = settingsRepository.state.value
            .copy(songSortOrder = SortOrder.DURATION_ASC)

        viewModel.state.test {
            val loaded = awaitLoaded()
            assertEquals(listOf(2L, 1L, 3L), loaded.songs.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state starts as loading and settles once the query lands`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.state.test {
                assertTrue(awaitItem().isLoading)
                assertFalse(awaitLoaded().isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `rapid typing only searches once`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.state.test {
            awaitLoaded()

            "rain".forEachIndexed { index, _ ->
                viewModel.onQueryChange("rain".take(index + 1))
                advanceTimeBy(50)
            }
            advanceUntilIdle()

            assertEquals(listOf("rain"), songRepository.searchQueries)
            assertEquals(listOf(2L), expectMostRecentItem().songs.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a pause between words searches for each of them`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.state.test {
            awaitLoaded()

            viewModel.onQueryChange("neon")
            advanceTimeBy(300)
            viewModel.onQueryChange("kaveh")
            advanceUntilIdle()

            assertEquals(listOf("neon", "kaveh"), songRepository.searchQueries)
            assertEquals(listOf(3L), expectMostRecentItem().songs.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing the query returns to the library without waiting`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.state.test {
                awaitLoaded()
                viewModel.onQueryChange("rain")
                advanceUntilIdle()
                assertEquals(listOf(2L), expectMostRecentItem().songs.map { it.id })

                viewModel.clearQuery()
                // No advanceTimeBy: a blank query is not debounced.
                advanceUntilIdle()

                val restored = expectMostRecentItem()
                assertEquals(listOf(3L, 1L, 2L), restored.songs.map { it.id })
                assertFalse(restored.isSearching)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `changing the sort order persists it and re-queries`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.state.test {
                awaitLoaded()
                viewModel.setSortOrder(SortOrder.TITLE_DESC)
                advanceUntilIdle()

                assertEquals(SortOrder.TITLE_DESC, settingsRepository.state.value.songSortOrder)
                assertEquals(listOf(2L, 1L, 3L), expectMostRecentItem().songs.map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `rescan is delegated to the repository`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.rescan()
        advanceUntilIdle()

        assertEquals(1, songRepository.rescanCount)
    }

    /**
     * Skips the placeholder emissions made before the first query returns. There can be more
     * than one of them: the stored sort order arrives after the flow has already started.
     */
    private suspend fun app.cash.turbine.ReceiveTurbine<SongsUiState>.awaitLoaded(): SongsUiState {
        while (true) {
            val state = awaitItem()
            if (!state.isLoading) return state
        }
    }
}
