package com.example.ava.players

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ExternalSoundResolverTest {
    @Test
    fun usesMatchingExternalWavForBundledSound() {
        val dir = Files.createTempDirectory("ava-sounds").toFile()
        File(dir, "wake_word_triggered.wav").writeBytes(byteArrayOf(1))

        assertEquals(
            "file://${File(dir, "wake_word_triggered.wav").absolutePath}",
            ExternalSoundResolver.resolve("asset:///sounds/wake_word_triggered.wav", dir)
        )
    }

    @Test
    fun keepsBundledSoundWhenExternalFileIsMissingOrUnsafe() {
        val dir = Files.createTempDirectory("ava-sounds").toFile()

        assertEquals("asset:///sounds/missing.wav", ExternalSoundResolver.resolve("asset:///sounds/missing.wav", dir))
        assertEquals("asset:///sounds/not_wav.mp3", ExternalSoundResolver.resolve("asset:///sounds/not_wav.mp3", dir))
        assertEquals("http://example/sound.wav", ExternalSoundResolver.resolve("http://example/sound.wav", dir))
        assertTrue(ExternalSoundResolver.isSafeWavName("stop_sound.wav"))
        assertEquals(false, ExternalSoundResolver.isSafeWavName("../stop_sound.wav"))
        assertEquals(false, ExternalSoundResolver.isSafeWavName(".hidden.wav"))
    }
}
