package com.example.ava.players

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

enum class AudioPlayerState {
    PLAYING, PAUSED, IDLE
}

@UnstableApi
class AudioPlayer(
    private val playerBuilder: () -> Player
) : AutoCloseable {
    private var _player: Player? = null
    private var isPlayerInit = false
    private var currentListener: Player.Listener? = null
    private val isClosing = AtomicBoolean(false)

    private val _state = MutableStateFlow(AudioPlayerState.IDLE)
    val state = _state.asStateFlow()

    val isPlaying: Boolean get() = runCatching { _player?.isPlaying ?: false }.getOrDefault(false)
    val isPaused: Boolean
        get() = runCatching {
            _player?.let { !it.isPlaying && !isCompletePlaybackState(it.playbackState) } ?: false
        }.getOrDefault(false)

    private var _volume = 1.0f
    var volume
        get() = _volume
        set(value) {
            _volume = value
            runCatching { _player?.volume = value }
        }

    fun init() {
        releasePlayer()
        _player = runCatching { playerBuilder().apply { volume = _volume } }
            .onFailure { Log.e(TAG, "Failed to init player", it) }
            .getOrNull()
        isPlayerInit = _player != null
    }

    fun play(mediaUri: String, onCompletion: () -> Unit = {}) {
        play(listOf(mediaUri), onCompletion)
    }

    fun play(mediaUris: Iterable<String>, onCompletion: () -> Unit = {}) {
        if (!isPlayerInit) init()
        val player = _player ?: run {
            Log.e(TAG, "Player is null, cannot play")
            onCompletion()
            return
        }
        isPlayerInit = false

        runCatching {
            currentListener?.let { player.removeListener(it) }
            player.stop()
            player.clearMediaItems()

            val listener = getPlayerListener(onCompletion)
            currentListener = listener
            player.addListener(listener)
            mediaUris.forEach { player.addMediaItem(MediaItem.fromUri(it)) }
            player.playWhenReady = true
            player.prepare()
        }.onFailure {
            Log.e(TAG, "Error playing media $mediaUris", it)
            onCompletion()
            safeClose()
        }
    }

    fun pause() {
        if (isPlaying) runCatching { _player?.pause() }
    }

    fun unpause() {
        if (isPaused) runCatching { _player?.play() }
    }

    fun stop() {
        safeClose()
    }

    private fun getPlayerListener(onCompletion: () -> Unit) = object : Player.Listener {
        private val completionCalled = AtomicBoolean(false)
        private val playbackStarted = AtomicBoolean(false)

        private fun complete() {
            if (completionCalled.compareAndSet(false, true)) onCompletion()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED ||
                shouldCompleteOnNotPlaying(playbackState, playbackStarted.get())
            ) {
                complete()
                safeClose()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "Player error: ${error.message}", error)
            complete()
            safeClose()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "onIsPlayingChanged: isPlaying=$isPlaying")
            if (isPlaying) {
                playbackStarted.set(true)
                _state.value = AudioPlayerState.PLAYING
            } else {
                _state.value = if (isPaused) AudioPlayerState.PAUSED else AudioPlayerState.IDLE
                if (shouldCompleteOnNotPlaying(_player?.playbackState ?: Player.STATE_IDLE, playbackStarted.get())) {
                    complete()
                }
            }
        }
    }

    private fun safeClose() {
        if (isClosing.compareAndSet(false, true)) {
            try {
                close()
            } finally {
                isClosing.set(false)
            }
        }
    }

    private fun releasePlayer() {
        val oldPlayer = _player
        val oldListener = currentListener
        _player = null
        currentListener = null
        oldPlayer?.let { player ->
            runCatching { oldListener?.let { player.removeListener(it) } }
            runCatching {
                player.stop()
                player.clearMediaItems()
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                runCatching { player.release() }
            }
        }
    }

    override fun close() {
        isPlayerInit = false
        _state.value = AudioPlayerState.IDLE
        releasePlayer()
    }

    companion object {
        private const val TAG = "AudioPlayer"

        internal fun isCompletePlaybackState(playbackState: Int): Boolean =
            playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED

        internal fun shouldCompleteOnNotPlaying(playbackState: Int, playbackStarted: Boolean): Boolean =
            playbackStarted && isCompletePlaybackState(playbackState)
    }
}
