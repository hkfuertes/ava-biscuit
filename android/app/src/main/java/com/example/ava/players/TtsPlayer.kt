package com.example.ava.players

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class TtsPlayer(private val player: AudioPlayer) : AutoCloseable {
    private var _ttsPlayed = false
    val ttsPlayed: Boolean get() = _ttsPlayed

    private var onCompletion: (() -> Unit)? = null

    var volume
        get() = player.volume
        set(value) {
            player.volume = value
        }

    fun runStart(onCompletion: () -> Unit) {
        this.onCompletion = onCompletion
        _ttsPlayed = false
        player.init()
    }

    fun markAsPlayed() {
        _ttsPlayed = true
    }

    fun triggerCompletion() {
        fireAndRemoveCompletionHandler()
    }

    fun playTts(ttsUrl: String?) {
        Log.d(TAG, "playTts called: url=$ttsUrl")
        if (ttsUrl.isNullOrBlank()) {
            Log.w(TAG, "TTS URL is null or blank")
            return
        }
        _ttsPlayed = true
        player.play(ttsUrl) { fireAndRemoveCompletionHandler() }
    }

    fun playSound(soundUrl: String?, onCompletion: () -> Unit) {
        playAnnouncement(soundUrl, null, onCompletion)
    }

    fun playAnnouncement(mediaUrl: String?, preannounceUrl: String?, onCompletion: () -> Unit) {
        Log.d(TAG, "playAnnouncement: mediaUrl=$mediaUrl")
        if (mediaUrl.isNullOrBlank()) {
            Log.w(TAG, "Media URL is null or blank")
            onCompletion()
            return
        }
        player.play(listOfNotNull(preannounceUrl?.takeIf { it.isNotBlank() }, mediaUrl), onCompletion)
    }

    fun stop() {
        onCompletion = null
        _ttsPlayed = false
        player.stop()
    }

    private fun fireAndRemoveCompletionHandler() {
        val completion = onCompletion
        onCompletion = null
        completion?.invoke()
    }

    override fun close() {
        player.close()
    }

    companion object {
        private const val TAG = "TtsPlayer"
    }
}
