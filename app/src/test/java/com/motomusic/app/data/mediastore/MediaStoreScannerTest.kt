package com.motomusic.app.data.mediastore

import com.motomusic.app.data.mediastore.MediaStoreScanner.Companion.isVoiceRecording
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The paths here are real ones taken off the test phone, which is the only way to be sure the
 * matcher covers what WhatsApp and the stock recorder actually write.
 */
class MediaStoreScannerTest {

    @Test
    fun `whatsapp voice notes and chat audio are recordings`() {
        assertTrue(
            isVoiceRecording(
                filePath = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/" +
                    "WhatsApp Voice Notes/202608/PTT-20260706-WA0000.opus",
                folderPath = "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes/202608",
                flaggedByMediaStore = false,
            )
        )
        assertTrue(
            isVoiceRecording(
                filePath = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/" +
                    "WhatsApp Audio/AUD-20260706-WA0000.m4a",
                folderPath = "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio",
                flaggedByMediaStore = false,
            )
        )
    }

    @Test
    fun `recorder folders are recordings whatever the file is called`() {
        assertTrue(
            isVoiceRecording(
                filePath = "/storage/emulated/0/Music/Recorder/records/Wednesday, 8h18m pm.m4a",
                folderPath = "Music/Recorder/records",
                flaggedByMediaStore = false,
            )
        )
    }

    @Test
    fun `MediaStore's own recording flag is trusted`() {
        assertTrue(isVoiceRecording(filePath = "/sdcard/Music/Song.mp3", folderPath = "Music", flaggedByMediaStore = true))
    }

    @Test
    fun `ordinary music is left alone`() {
        assertFalse(
            isVoiceRecording(
                filePath = "/storage/emulated/0/Download/Oorum Blood.mp3",
                folderPath = "Download",
                flaggedByMediaStore = false,
            )
        )
        assertFalse(
            isVoiceRecording(
                filePath = "/storage/emulated/0/Music/Neon Harbour/01 Glass Avenue.m4a",
                folderPath = "Music/Neon Harbour",
                flaggedByMediaStore = false,
            )
        )
    }

    @Test
    fun `a folder path is enough when MediaStore hides the file path`() {
        assertTrue(isVoiceRecording(filePath = "", folderPath = "Recordings/Call", flaggedByMediaStore = false))
        assertFalse(isVoiceRecording(filePath = "", folderPath = "", flaggedByMediaStore = false))
    }
}
