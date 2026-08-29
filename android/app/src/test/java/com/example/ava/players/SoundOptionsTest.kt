package com.example.ava.players

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SoundOptionsTest {
    @Test
    fun buildsBundledSoundOptionsFromAssetFolders() {
        assertEquals(
            listOf(
                SoundOptions.Option("Alexa", "asset:///sounds/alexa.mp3"),
                SoundOptions.Option("Wake Word Triggered", "asset:///sounds/wake_word_triggered.wav"),
                SoundOptions.Option("Stop Sound", "asset:///stopWords/stop_sound.wav"),
            ),
            SoundOptions.bundledOptions { path ->
                when (path) {
                    "sounds" -> listOf("alexa.mp3", "wake_word_triggered.wav", "ignored.ogg")
                    "stopWords" -> listOf("stop_sound.wav")
                    else -> emptyList()
                }
            }
        )
    }

    @Test
    fun listsBundledAndExternalSoundsWithFriendlyLabels() {
        val dir = Files.createTempDirectory("ava-sounds").toFile()
        File(dir, "wake_word_triggered.wav").writeBytes(byteArrayOf(1))
        File(dir, "custom-chime.mp3").writeBytes(byteArrayOf(1))
        File(dir, "ignored.ogg").writeBytes(byteArrayOf(1))

        val bundled = SoundOptions.bundledOptions { path ->
            when (path) {
                "sounds" -> listOf("wake_word_triggered.wav", "timer_finished.wav")
                "stopWords" -> listOf("stop_sound.wav")
                else -> emptyList()
            }
        }
        val external = SoundOptions.externalOptions(dir)
        val options = (listOf(SoundOptions.Option(SoundOptions.NO_SOUND_LABEL, SoundOptions.NO_SOUND_URI)) + bundled + external)
            .distinctBy { it.uri }
            .sortedBy { it.label }

        assertTrue(options.any { it.label == "No Sound" && it.uri == "no-sound" })
        assertTrue(options.any { it.label == "Wake Word Triggered" && it.uri == "asset:///sounds/wake_word_triggered.wav" })
        assertTrue(options.any { it.label == "Wake Word Triggered (external)" && it.uri == "file://${File(dir, "wake_word_triggered.wav").absolutePath}" })
        assertTrue(options.any { it.label == "Custom Chime (external)" && it.uri == "file://${File(dir, "custom-chime.mp3").absolutePath}" })
        assertEquals(false, options.any { it.label.contains("ignored", ignoreCase = true) })
    }

    @Test
    fun formatsMissingUriLabels() {
        assertTrue(SoundOptions.isNoSound("no-sound"))
        assertEquals("Timer Finished", SoundOptions.labelForMissingUri("asset:///sounds/timer_finished.wav"))
        assertEquals("Bell (external)", SoundOptions.labelForMissingUri("file:///sdcard/ava/sounds/bell.wav"))
    }
}
