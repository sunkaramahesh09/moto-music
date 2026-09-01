package com.motomusic.app.presentation.player

import com.motomusic.app.domain.repository.FavoritesRepository
import com.motomusic.app.domain.repository.LastSession
import com.motomusic.app.domain.repository.SettingsRepository
import com.motomusic.app.playback.PlaybackConnection
import com.motomusic.app.playback.PlaybackEventBus
import com.motomusic.app.playback.PlaybackPosition
import com.motomusic.app.playback.PlaybackUiState
import com.motomusic.app.playback.SleepTimerController
import com.motomusic.app.playback.SleepTimerState
import com.motomusic.app.playback.TrackInfo
import com.motomusic.app.util.FakePlaylistRepository
import com.motomusic.app.util.FakeSongRepository
import com.motomusic.app.util.MainDispatcherRule
import app.cash.turbine.test
import com.motomusic.app.util.testSong
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The player is created once at the root of the app, so the launch path matters most:
 * it must put the previous queue back without ever starting audio by itself.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playbackState = MutableStateFlow(PlaybackUiState())

    private val playbackConnection = mockk<PlaybackConnection>(relaxed = true) {
        every { state } returns playbackState
        every { position } returns MutableStateFlow(PlaybackPosition())
    }
    private val favoritesRepository = mockk<FavoritesRepository>(relaxed = true) {
        every { observeFavoriteIds() } returns flowOf(setOf(2L))
    }
    private val playlistRepository = FakePlaylistRepository()
    private val songRepository = FakeSongRepository(listOf(testSong(1), testSong(2), testSong(3)))
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val sleepTimerController = mockk<SleepTimerController>(relaxed = true) {
        every { state } returns MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    }
    private val eventBus = PlaybackEventBus()

    // See SongsViewModelTest: the rule is applied after the class is constructed.
    private val viewModel by lazy {
        PlayerViewModel(
            playbackConnection = playbackConnection,
            favoritesRepository = favoritesRepository,
            playlistRepository = playlistRepository,
            songRepository = songRepository,
            settingsRepository = settingsRepository,
            sleepTimerController = sleepTimerController,
            eventBus = eventBus,
        )
    }

    @Test
    fun `the previous queue is restored, never played`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { settingsRepository.loadLastSession() } returns
            LastSession(songIds = listOf(3L, 1L), index = 1, positionMs = 42_000L)

        viewModel
        advanceUntilIdle()

        verify { playbackConnection.connect() }
        coVerify(exactly = 1) {
            playbackConnection.restoreQueue(
                songs = match { it.map(com.motomusic.app.domain.model.Song::id) == listOf(3L, 1L) },
                index = 1,
                positionMs = 42_000L,
            )
        }
        verify(exactly = 0) { playbackConnection.playSongs(any(), any()) }
    }

    @Test
    fun `an index past the end of the restored queue is clamped`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { settingsRepository.loadLastSession() } returns
                LastSession(songIds = listOf(1L), index = 7, positionMs = 0L)

            viewModel
            advanceUntilIdle()

            coVerify { playbackConnection.restoreQueue(any(), index = 0, positionMs = 0L) }
        }

    @Test
    fun `songs that have since disappeared leave the queue alone`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { settingsRepository.loadLastSession() } returns
                LastSession(songIds = listOf(404L), index = 0, positionMs = 0L)

            viewModel
            advanceUntilIdle()

            coVerify(exactly = 0) { playbackConnection.restoreQueue(any(), any(), any()) }
        }

    @Test
    fun `a settings read that blows up does not take the player down with it`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { settingsRepository.loadLastSession() } throws IllegalStateException("corrupt")

            viewModel
            advanceUntilIdle()

            coVerify(exactly = 0) { playbackConnection.restoreQueue(any(), any(), any()) }
        }

    @Test
    fun `the menu for the current track is read back from the library`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { settingsRepository.loadLastSession() } returns null
            playbackState.value = PlaybackUiState(current = trackInfo(songId = 2L))

            viewModel.openMenuForCurrentSong()
            advanceUntilIdle()

            assertEquals(2L, viewModel.menuTarget.value?.song?.id)
            assertNull(viewModel.menuTarget.value?.playlistId)
        }

    @Test
    fun `opening the menu inside a playlist remembers which playlist`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { settingsRepository.loadLastSession() } returns null

            viewModel.openMenuInPlaylist(testSong(1), playlistId = 9L)

            assertEquals(9L, viewModel.menuTarget.value?.playlistId)
            viewModel.dismissMenu()
            assertNull(viewModel.menuTarget.value)
        }

    @Test
    fun `a blank playlist name creates nothing`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { settingsRepository.loadLastSession() } returns null

        viewModel.createPlaylistWith("   ", listOf(1L))
        advanceUntilIdle()

        assertEquals(emptyList<Long>(), playlistRepository.names())
    }

    @Test
    fun `creating a playlist from the sheet fills it in one step`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { settingsRepository.loadLastSession() } returns null

            viewModel.createPlaylistWith("  Road trip  ", listOf(1L, 3L))
            advanceUntilIdle()

            assertEquals(listOf("Road trip"), playlistRepository.names())
            assertEquals(listOf(1L, 3L), playlistRepository.songIdsIn(1L))
        }

    @Test
    fun `favouriting the current track uses the session's song id`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { settingsRepository.loadLastSession() } returns null
            playbackState.value = PlaybackUiState(current = trackInfo(songId = 3L))

            viewModel.toggleFavoriteForCurrent()
            advanceUntilIdle()

            coVerify(exactly = 1) { favoritesRepository.toggleFavorite(3L) }
        }

    @Test
    fun `with nothing playing there is no favourite to toggle`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { settingsRepository.loadLastSession() } returns null

            viewModel.toggleFavoriteForCurrent()
            advanceUntilIdle()

            coVerify(exactly = 0) { favoritesRepository.toggleFavorite(any()) }
        }

    private fun trackInfo(songId: Long) = TrackInfo(
        songId = songId,
        title = "Song $songId",
        artist = "Artist",
        album = "Album",
        albumId = 1L,
        songUri = null,
        durationMs = 180_000L,
    )

    @Test
    fun `hiding a song stores its path and says so`() = runTest(mainDispatcherRule.testDispatcher) {
        val song = testSong(7).copy(filePath = "/storage/emulated/0/Download/a recording.m4a")

        eventBus.messages.test {
            viewModel.hideFromLibrary(song)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                settingsRepository.setSongHidden(song.filePath, hidden = true)
            }
            verify(exactly = 1) { playbackConnection.removeSongFromQueue(song.id) }
            assertTrue(awaitItem().contains(song.title))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
