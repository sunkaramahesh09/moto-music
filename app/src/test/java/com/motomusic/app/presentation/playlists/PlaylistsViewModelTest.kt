package com.motomusic.app.presentation.playlists

import app.cash.turbine.test
import com.motomusic.app.util.FakePlaylistRepository
import com.motomusic.app.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePlaylistRepository()

    // Built lazily so that `viewModelScope` picks up the test dispatcher the rule installs,
    // which happens after this class is constructed.
    private val viewModel by lazy { PlaylistsViewModel(repository) }

    @Test
    fun `creating a playlist trims the name`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.createPlaylist("  Road trip  ")
        advanceUntilIdle()

        assertEquals(listOf("Road trip"), repository.names())
    }

    @Test
    fun `a blank name creates nothing`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.createPlaylist("   ")
        advanceUntilIdle()

        assertTrue(repository.names().isEmpty())
    }

    @Test
    fun `a duplicate name is refused with a message instead of a second playlist`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.messages.test {
            viewModel.createPlaylist("Road trip")
            advanceUntilIdle()
            viewModel.createPlaylist("road trip")
            advanceUntilIdle()

            assertEquals("A playlist called \"road trip\" already exists", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf("Road trip"), repository.names())
    }

    @Test
    fun `renaming and deleting reach the repository`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.createPlaylist("Road trip")
        advanceUntilIdle()
        // The fake hands out ids from one, so the first playlist created is always id 1.
        val id = 1L

        viewModel.renamePlaylist(id, " Commute ")
        advanceUntilIdle()
        assertEquals(listOf("Commute"), repository.names())

        viewModel.deletePlaylist(id)
        advanceUntilIdle()
        assertTrue(repository.names().isEmpty())
    }

    @Test
    fun `the list of playlists is published to the ui`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.state.test {
            assertTrue(awaitItem().isLoading)

            viewModel.createPlaylist("Road trip")
            advanceUntilIdle()

            val loaded = expectMostRecentItem()
            assertEquals(listOf("Road trip"), loaded.playlists.map { it.name })
            assertTrue(!loaded.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
