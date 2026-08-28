package com.example.ava.microwakeword

import java.nio.ByteBuffer

class CompositeWakeWordProvider(private vararg val providers: WakeWordProvider) : WakeWordProvider {
    override fun getWakeWords(): List<WakeWordWithId> = providers
        .flatMap { it.getWakeWords() }
        .distinctBy { it.id }

    override fun loadWakeWordModel(model: String): ByteBuffer {
        var lastFailure: Exception? = null
        for (provider in providers) {
            try {
                return provider.loadWakeWordModel(model)
            } catch (failure: Exception) {
                lastFailure = failure
            }
        }
        throw IllegalArgumentException("Wake word model not found: $model", lastFailure)
    }
}
