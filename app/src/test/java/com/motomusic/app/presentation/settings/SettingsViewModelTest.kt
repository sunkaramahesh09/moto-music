package com.motomusic.app.presentation.settings

import app.cash.turbine.test
import com.motomusic.app.domain.model.ScanState
import com.motomusic.app.domain.model.ThemeMode
import com.motomusic.app.domain.repository.PlaybackHistoryRepository
import com.motomusic.app.util.FakeSettingsRepository
import com.motomusic.app.util.FakeSongRepository
import com.motomusic.app.util.MainDispatcherRule
import com.motomusic.app.util.testSong
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository()
    private val songRepository = FakeSongRepository(listOf(testSong(1), testSong(2)))
    private val historyRepository = mockk<PlaybackHistoryRepository>(relaxed = true)

    // See SongsViewModelTest: the rule is applied after the class is constructed.
    private val viewModel by lazy {
        SettingsViewModel(settingsRepository, songRepository, historyRepository)
    }

    @Test
    fun `state reports the stored preferences and the library size`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.state.test {
                advanceUntilIdle()
                val state = expectMostRecentItem()

                assertEquals(ThemeMode.SYSTEM, state.preferences.themeMode)
                assertEquals(2, state.songCount)
                assertFalse(state.isScanning)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a scan in progress is reflected in the state`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.state.test {
            advanceUntilIdle()
            songRepository.scanState.value = ScanState.Scanning(processed = 3, total = 10)
            advanceUntilIdle()

            assertTrue(expectMostRecentItem().isScanning)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggles are written straight through to settings`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.setThemeMode(ThemeMode.DARK)
            viewModel.setDynamicColor(false)
            viewModel.setResumeLastSession(false)
            viewModel.setSkipSilence(true)
            viewModel.setPauseOnHeadphonesDisconnect(false)
            viewModel.setFadeOnPlayPause(false)
            viewModel.setHideVoiceRecordings(false)
            advanceUntilIdle()

            val stored = settingsRepository.state.value
            assertEquals(ThemeMode.DARK, stored.themeMode)
            assertFalse(stored.useDynamicColor)
            assertFalse(stored.resumeLastSession)
            assertTrue(stored.skipSilence)
            assertFalse(stored.pauseOnHeadphonesDisconnect)
            assertFalse(stored.fadeOnPlayPause)
            assertFalse(stored.hideVoiceRecordings)
        }

    @Test
    fun `rescan and clear history reach their repositories`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.rescan()
            viewModel.clearHistory()
            advanceUntilIdle()

            assertEquals(1, songRepository.rescanCount)
            coVerify(exactly = 1) { historyRepository.clearHistory() }
        }
}
