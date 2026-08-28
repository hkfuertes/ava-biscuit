package com.example.ava.microwakeword

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files

class FileWakeWordProviderTest {
    @Test
    fun loadsFlatWakeWordsFromDirectory() {
        val dir = tempDir()
        File(dir, "custom.tflite").writeBytes(byteArrayOf(1, 2, 3))
        File(dir, "custom.json").writeText(wakeWordJson("custom.tflite", "Custom Wake"))

        val provider = FileWakeWordProvider(dir)
        val wakeWord = provider.getWakeWords().single()

        assertEquals("custom", wakeWord.id)
        assertEquals("Custom Wake (external)", wakeWord.wakeWord.wake_word)
        assertEquals(3, provider.loadWakeWordModel(wakeWord.wakeWord.model).remaining())
    }

    @Test
    fun ignoresMissingModelsAndUnsafeNames() {
        val dir = tempDir()
        File(dir, "missing.json").writeText(wakeWordJson("missing.tflite"))
        File(dir, "unsafe.json").writeText(wakeWordJson("../escape.tflite"))
        File(dir, "nested").mkdir()
        File(dir, "nested/nested.json").writeText(wakeWordJson("nested.tflite"))

        assertTrue(FileWakeWordProvider(dir).getWakeWords().isEmpty())
    }

    @Test
    fun compositeProviderKeepsBundledIdsFirstAndFallsBackToFileModels() {
        val bundled = fakeProvider("same", "bundled.tflite", byteArrayOf(1))
        val downloaded = fakeProvider("same", "sdcard:downloaded.tflite", byteArrayOf(2))
        val composite = CompositeWakeWordProvider(bundled, downloaded)

        assertEquals(listOf("same"), composite.getWakeWords().map { it.id })
        assertEquals(1, composite.loadWakeWordModel("bundled.tflite").remaining())
        assertEquals(1, composite.loadWakeWordModel("sdcard:downloaded.tflite").remaining())
    }

    private fun tempDir() = Files.createTempDirectory("ava-wakewords").toFile()

    private fun fakeProvider(id: String, expectedModel: String, bytes: ByteArray) = object : WakeWordProvider {
        override fun getWakeWords() = listOf(WakeWordWithId(id, wakeWord(expectedModel)))
        override fun loadWakeWordModel(model: String): ByteBuffer {
            if (model != expectedModel) throw IllegalArgumentException(model)
            return ByteBuffer.wrap(bytes)
        }
    }

    private fun wakeWord(model: String) = WakeWord(
        type = "micro",
        wake_word = "Wake",
        author = "test",
        website = "",
        model = model,
        trained_languages = arrayOf("en"),
        version = 1,
        micro = Micro(
            probability_cutoff = 0.85f,
            feature_step_size = 10,
            sliding_window_size = 5,
            tensor_arena_size = 22860,
            minimum_esphome_version = "2024.7.0"
        )
    )

    private fun wakeWordJson(model: String, wakeWord: String = "Wake") = """
        {
          "type": "micro",
          "wake_word": "$wakeWord",
          "author": "test",
          "website": "",
          "model": "$model",
          "trained_languages": ["en"],
          "version": 1,
          "micro": {
            "probability_cutoff": 0.85,
            "feature_step_size": 10,
            "sliding_window_size": 5,
            "tensor_arena_size": 22860,
            "minimum_esphome_version": "2024.7.0"
          }
        }
    """.trimIndent()
}
