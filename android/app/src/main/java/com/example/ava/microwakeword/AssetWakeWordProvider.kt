package com.example.ava.microwakeword

import android.content.res.AssetManager
import android.util.Log
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer

class AssetWakeWordProvider(val assets: AssetManager, val path: String = DEFAULT_WAKE_WORD_PATH) :
    WakeWordProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override fun getWakeWords(): List<WakeWordWithId> {
        val assetsList = assets.list(path) ?: return emptyList()
        return buildList {
            for (asset in assetsList) {
                if (!asset.endsWith(".json")) continue

                runCatching {
                    val contents = assets.open("$path/$asset").bufferedReader().use { it.readText() }
                    add(WakeWordWithId(asset.removeSuffix(".json"), json.decodeFromString<WakeWord>(contents)))
                }.onFailure {
                    Log.e(TAG, "Error loading wake word: $asset", it)
                }
            }
        }
    }

    override fun loadWakeWordModel(model: String): ByteBuffer {
        val bytes = assets.open("$path/$model").use { it.readBytes() }
        return ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            rewind()
        }
    }

    companion object {
        private const val TAG = "AssetWakeWordProvider"
        const val DEFAULT_WAKE_WORD_PATH = "wakeWords"
    }
}
