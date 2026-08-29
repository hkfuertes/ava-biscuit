package com.example.ava.players

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ExternalSoundResolverTest {
    @Test
    fun usesMatchingExternalSoundForBundledSound() {
        val dir = Files.createTempDirectory("ava-sounds").toFile()
        File(dir, "wake_word_triggered.wav").writeBytes(byteArrayOf(1))
        File(dir, "ding.mp3").writeBytes(byteArrayOf(1))

        assertEquals(
            "file://${File(dir, "wake_word_triggered.wav").absolutePath}",
            ExternalSoundResolver.resolve("asset:///sounds/wake_word_triggered.wav", dir)
        )
        assertEquals(
            "file://${File(dir, "ding.mp3").absolutePath}",
            ExternalSoundResolver.resolve("asset:///sounds/ding.mp3", dir)
        )
    }

    @Test
    fun keepsBundledSoundWhenExternalFileIsMissingOrUnsafe() {
        val dir = Files.createTempDirectory("ava-sounds").toFile()

        assertEquals("asset:///sounds/missing.wav", ExternalSoundResolver.resolve("asset:///sounds/missing.wav", dir))
        assertEquals("asset:///sounds/not_supported.ogg", ExternalSoundResolver.resolve("asset:///sounds/not_supported.ogg", dir))
        assertEquals("http://example/sound.wav", ExternalSoundResolver.resolve("http://example/sound.wav", dir))
        assertTrue(ExternalSoundResolver.isSafeSoundName("stop_sound.wav"))
        assertTrue(ExternalSoundResolver.isSafeSoundName("ding.mp3"))
        assertEquals(false, ExternalSoundResolver.isSafeSoundName("../stop_sound.wav"))
        assertEquals(false, ExternalSoundResolver.isSafeSoundName(".hidden.wav"))
    }
}
