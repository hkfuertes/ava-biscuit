package com.example.ava.microwakeword

import kotlinx.serialization.json.Json
import java.io.File
import java.nio.ByteBuffer

class FileWakeWordProvider(private val directory: File) : WakeWordProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override fun getWakeWords(): List<WakeWordWithId> {
        val files = directory.listFiles { file -> file.isFile && file.name.endsWith(".json") } ?: return emptyList()
        return files.sortedBy { it.name }.mapNotNull { file ->
            runCatching {
                val fileId = file.name.removeSuffix(".json")
                if (!isSafeFileName(file.name) || !isSafeFileName(fileId)) return@mapNotNull null
                val wakeWord = json.decodeFromString<WakeWord>(file.readText())
                if (!isSafeFileName(wakeWord.model)) return@mapNotNull null
                if (!File(directory, wakeWord.model).isFile) return@mapNotNull null
                WakeWordWithId(
                    "$ID_PREFIX$fileId",
                    wakeWord.copy(
                        wake_word = "${wakeWord.wake_word} (external)",
                        model = MODEL_PREFIX + wakeWord.model
                    )
                )
            }.getOrNull()
        }
    }

    override fun loadWakeWordModel(model: String): ByteBuffer {
        require(model.startsWith(MODEL_PREFIX)) { "External wake word model must use $MODEL_PREFIX" }
        val fileName = model.removePrefix(MODEL_PREFIX)
        require(isSafeFileName(fileName)) { "Unsafe wake word model path" }
        val bytes = File(directory, fileName).readBytes()
        return ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            rewind()
        }
    }

    companion object {
        private const val ID_PREFIX = "external_"
        private const val MODEL_PREFIX = "sdcard:"
        private val SAFE_FILE_NAME = Regex("[A-Za-z0-9._-]+")

        internal fun isSafeFileName(name: String) = SAFE_FILE_NAME.matches(name) && name != "." && name != ".."
    }
}
