package com.example.ava.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class PlayerSettings(
    val volume: Float = 0.1f,
    val muted: Boolean = false,
    val enableWakeSound: Boolean = true,
    val wakeSound: String = "asset:///sounds/wake_word_triggered.wav",
    val wakeSound2: String = "asset:///sounds/wake_word_triggered.wav",
    val timerFinishedSound: String = "asset:///sounds/timer_finished.wav",
    val stopSound: String = "asset:///stopWords/stop_sound.wav",
    val enableStopSound: Boolean = true,
    val continuousPromptSound: String = "asset:///sounds/continuous_prompt.wav",
    val enableContinuousConversation: Boolean = false,
    val actionButtonIndependent: Boolean = false,
)

val Context.playerSettingsStore: DataStore<PlayerSettings> by dataStore(
    fileName = "player_settings.json",
    serializer = SettingsSerializer(PlayerSettings.serializer(), PlayerSettings()),
    corruptionHandler = defaultCorruptionHandler(PlayerSettings())
)

class PlayerSettingsStore(dataStore: DataStore<PlayerSettings>) :
    SettingsStoreImpl<PlayerSettings>(dataStore, PlayerSettings()) {
    val volume = SettingState(getFlow().map { it.volume }) { value -> update { it.copy(volume = value) } }
    val muted = SettingState(getFlow().map { it.muted }) { value -> update { it.copy(muted = value) } }
    val enableWakeSound = SettingState(getFlow().map { it.enableWakeSound }) { value ->
        update { it.copy(enableWakeSound = value) }
    }
    val wakeSound = SettingState(getFlow().map { it.wakeSound }) { value -> update { it.copy(wakeSound = value) } }
    val wakeSound2 = SettingState(getFlow().map { it.wakeSound2 }) { value -> update { it.copy(wakeSound2 = value) } }
    val timerFinishedSound = SettingState(getFlow().map { it.timerFinishedSound }) { value ->
        update { it.copy(timerFinishedSound = value) }
    }
    val stopSound = SettingState(getFlow().map { it.stopSound }) { value -> update { it.copy(stopSound = value) } }
    val enableStopSound = SettingState(getFlow().map { it.enableStopSound }) { value ->
        update { it.copy(enableStopSound = value) }
    }
    val continuousPromptSound = SettingState(getFlow().map { it.continuousPromptSound }) { value ->
        update { it.copy(continuousPromptSound = value) }
    }
    val enableContinuousConversation = SettingState(getFlow().map { it.enableContinuousConversation }) { value ->
        update { it.copy(enableContinuousConversation = value) }
    }
    val actionButtonIndependent = SettingState(getFlow().map { it.actionButtonIndependent }) { value ->
        update { it.copy(actionButtonIndependent = value) }
    }
}
