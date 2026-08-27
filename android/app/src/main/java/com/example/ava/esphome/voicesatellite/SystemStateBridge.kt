package com.example.ava.esphome.voicesatellite

import android.media.AudioManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Adapts Android system audio (AudioManager) for the bridge.
 * Implementations wrap platform APIs; tests use fakes.
 */
interface SystemAudioAdapter {
    fun isMicMuted(): Boolean
    fun setMicMuted(muted: Boolean)
    fun getMusicVolume(): Int
    fun getMusicMaxVolume(): Int
    fun setMusicVolume(volume: Int)
}

class AndroidSystemAudioAdapter(private val audioManager: AudioManager) : SystemAudioAdapter {
    override fun isMicMuted() = audioManager.isMicrophoneMute
    override fun setMicMuted(muted: Boolean) = audioManager.setMicrophoneMute(muted)
    override fun getMusicVolume() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    override fun getMusicMaxVolume() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    override fun setMusicVolume(volume: Int) {
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume.coerceIn(0, getMusicMaxVolume().coerceAtLeast(0)),
            0
        )
    }
}

/** Receives observed state changes to publish toward HA-facing entity flows. */
interface VoiceStateListener {
    fun onMuteStateChanged(muted: Boolean)
    fun onVolumeChanged(normalizedVolume: Float)
}

/**
 * Bidirectional bridge: Android system mic-mute + MUSIC-stream volume ↔ HA voice state.
 *
 * - Physical changes: call [pollAndPublish] to read system state and emit only on delta.
 * - HA commands: call [onHaMuteCommand] / [onHaVolumeCommand] which set system state first,
 *   then read back the observed value and publish it (source-of-truth is always Android).
 * - Loop guard: the [applyingCommand] flag prevents echo when a command triggers a poll.
 * - Stale guard: only publishes when observed value differs from last published value.
 * - Range guard: volume is coerced to [0, max] before writing and [0, 1] after reading.
 */
class SystemStateBridge(
    private val adapter: SystemAudioAdapter,
    private val listener: VoiceStateListener
) {
    private var lastPublishedMute: Boolean? = null
    private var lastPublishedVolume: Float? = null
    private val applyingCommand = AtomicBoolean(false)

    /** Read system state and publish any deltas. Call from a periodic poll or ContentObserver. */
    fun pollAndPublish() {
        if (applyingCommand.get()) return
        publishMute(adapter.isMicMuted())
        publishVolume(readNormalizedVolume())
    }

    /** HA mute command: set Android system state, then publish observed result. */
    fun onHaMuteCommand(muted: Boolean) {
        applyingCommand.set(true)
        try {
            adapter.setMicMuted(muted)
            publishMute(adapter.isMicMuted())
        } finally {
            applyingCommand.set(false)
        }
    }

    /** HA volume command (normalized 0..1): set Android system state, then publish observed result. */
    fun onHaVolumeCommand(normalizedVolume: Float) {
        val clamped = normalizedVolume.coerceIn(0f, 1f)
        applyingCommand.set(true)
        try {
            val raw = denormalize(clamped)
            adapter.setMusicVolume(raw)
            publishVolume(readNormalizedVolume())
        } finally {
            applyingCommand.set(false)
        }
    }

    private fun publishMute(muted: Boolean) {
        if (muted != lastPublishedMute) {
            lastPublishedMute = muted
            listener.onMuteStateChanged(muted)
        }
    }

    private fun publishVolume(volume: Float) {
        if (volume != lastPublishedVolume) {
            lastPublishedVolume = volume
            listener.onVolumeChanged(volume)
        }
    }

    private fun readNormalizedVolume(): Float {
        val max = adapter.getMusicMaxVolume()
        if (max <= 0) return 0f
        return (adapter.getMusicVolume().coerceIn(0, max)).toFloat() / max.toFloat()
    }

    private fun denormalize(normalized: Float): Int {
        val max = adapter.getMusicMaxVolume().coerceAtLeast(0)
        return (normalized * max).toInt().coerceIn(0, max)
    }
}
