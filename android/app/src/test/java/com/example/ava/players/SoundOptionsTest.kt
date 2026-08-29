package com.example.ava.players

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SoundOptionsTest {
    @Test
    fun exposesDefaultBundledSoundOptions() {
        assertEquals(
            listOf(
                "No Sound",
                "Alexa",
                "Bubble",
                "Continuous Prompt",
                "Ding",
                "Home Assistant",
                "Start Listening Button",
                "Timer Finished",
                "Wake Word Triggered",
                "Stop Sound",
                "Stop Word",
            ),
            SoundOptions.DEFAULT_BUNDLED_SOUNDS.map { it.label }
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
        val options = (SoundOptions.DEFAULT_BUNDLED_SOUNDS + bundled + external)
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
        assertEquals("Bell (external)", SoundOptions.labelForMissingUri("file:///sdcard/sounds/bell.wav"))
    }
}
