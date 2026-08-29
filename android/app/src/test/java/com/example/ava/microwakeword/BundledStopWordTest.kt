package com.example.ava.microwakeword

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BundledStopWordTest {
    @Test
    fun bundlesVacaStopWordModel() {
        val json = File("src/main/assets/stopWords/stop.json").readText()
        val model = File("src/main/assets/stopWords/stop.tflite")

        assertTrue(model.isFile)
        assertTrue(json.contains("\"wake_word\": \"Stop\""))
        assertTrue(json.contains("\"trained_languages\": [\"en\"]"))
        assertTrue(json.contains("\"model\": \"stop.tflite\""))
        assertTrue(json.contains("\"probability_cutoff\": 0.5"))
        assertEquals(45544, model.length())
    }
}
