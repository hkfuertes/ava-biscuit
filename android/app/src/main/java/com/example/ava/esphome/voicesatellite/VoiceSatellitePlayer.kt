package com.example.ava.esphome.voicesatellite

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.example.ava.players.AudioPlayer
import com.example.ava.players.ExternalSoundResolver
import com.example.ava.players.SoundOptions
import com.example.ava.players.TtsPlayer
import com.example.ava.settings.SettingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class VoiceSatellitePlayer(
    val ttsPlayer: TtsPlayer,
    val mediaPlayer: AudioPlayer,
    private val wakeSoundPlayer: AudioPlayer,
    volume: Float = 1.0f,
    muted: Boolean = false,
    val enableWakeSound: SettingState<Boolean>,
    private val wakeSound: SettingState<String>,
    private val wakeSound2: SettingState<String>,
    private val timerFinishedSound: SettingState<String>,
    private val stopSound: SettingState<String>,
    private val enableStopSound: SettingState<Boolean>,
    private val continuousPromptSound: SettingState<String>
) : AutoCloseable {
    private val _volume = MutableStateFlow(volume)
    private val _muted = MutableStateFlow(muted)

    val volume = _volume.asStateFlow()
    val muted = _muted.asStateFlow()
    var onSetVolume: ((Float) -> Unit)? = null

    init {
        setVolumeFromSystem(volume)
        setMuted(muted)
    }

    fun setVolume(value: Float) {
        onSetVolume?.invoke(value) ?: setVolumeFromSystem(value)
    }

    fun setVolumeFromSystem(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _volume.value = clamped
        if (!_muted.value) {
            ttsPlayer.volume = clamped
            mediaPlayer.volume = clamped
        }
    }

    fun setMuted(value: Boolean) {
        _muted.value = value
        val outputVolume = if (value) 0.0f else _volume.value
        mediaPlayer.volume = outputVolume
        ttsPlayer.volume = outputVolume
    }

    suspend fun playWakeSound(wakeWordIndex: Int = 0, onCompletion: () -> Unit = {}) {
        val enabled = enableWakeSound.get()
        val sound = if (enabled) selectedWakeSound(wakeWordIndex) else SoundOptions.NO_SOUND_URI
        Log.d(TAG, "playWakeSound: enabled=$enabled, wakeWordIndex=$wakeWordIndex, sound=$sound")
        playSound(sound, onCompletion)
    }

    fun playStartListeningSound(onCompletion: () -> Unit = {}) {
        playSound(START_LISTENING_SOUND, onCompletion)
    }

    suspend fun playTimerFinishedSound(onCompletion: () -> Unit = {}) {
        playTtsSound(timerFinishedSound.get(), onCompletion)
    }

    suspend fun playStopSound(onCompletion: () -> Unit = {}) {
        val enabled = enableStopSound.get()
        val sound = if (enabled) stopSound.get() else SoundOptions.NO_SOUND_URI
        Log.d(TAG, "playStopSound: enabled=$enabled, sound=$sound")
        playSound(sound, onCompletion)
    }

    suspend fun playContinuousPromptSound(onCompletion: () -> Unit = {}) {
        playSound(continuousPromptSound.get(), onCompletion)
    }

    fun previewSound(soundUrl: String) {
        playSound(soundUrl)
    }

    private suspend fun selectedWakeSound(wakeWordIndex: Int) = if (wakeWordIndex == 1) wakeSound2.get() else wakeSound.get()

    private fun playSound(soundUrl: String, onCompletion: () -> Unit = {}) {
        if (SoundOptions.isNoSound(soundUrl)) onCompletion() else wakeSoundPlayer.play(resolveSound(soundUrl), onCompletion)
    }

    private fun playTtsSound(soundUrl: String, onCompletion: () -> Unit = {}) {
        if (SoundOptions.isNoSound(soundUrl)) onCompletion() else ttsPlayer.playSound(resolveSound(soundUrl), onCompletion)
    }

    private fun resolveSound(soundUrl: String) = ExternalSoundResolver.resolve(soundUrl) ?: soundUrl

    override fun close() {
        ttsPlayer.close()
        mediaPlayer.close()
        wakeSoundPlayer.close()
    }

    companion object {
        private const val TAG = "VoiceSatellitePlayer"
        private const val START_LISTENING_SOUND = "asset:///sounds/start_listening_button.wav"
    }
}
