package com.example.ava.esphome.voicesatellite

import android.content.Context
import com.example.ava.R
import com.example.ava.esphome.entities.BinarySensorEntity
import com.example.ava.esphome.entities.MediaPlayerEntity
import com.example.ava.esphome.entities.NumberEntity
import com.example.ava.esphome.entities.SelectEntity
import com.example.ava.esphome.entities.SwitchEntity
import com.example.ava.players.SoundOptions
import com.example.ava.settings.PlayerSettingsStore
import com.example.ava.settings.SettingState
import com.example.esphomeproto.api.EntityCategory
import kotlinx.coroutines.flow.map

object VoiceSatelliteEntities {
    fun buildEntities(
        audioInput: VoiceSatelliteAudioInput,
        player: VoiceSatellitePlayer,
        context: Context,
        playerSettingsStore: PlayerSettingsStore? = null,
        actionButtonBridge: BiscuitActionButtonBridge = BiscuitActionButtonBridge.shared,
        onMicrophoneVolumeChanged: ((Float) -> Unit)? = null
    ) = buildList {
        add(SwitchEntity(
            1,
            context.getString(R.string.entity_mute_microphone),
            "mute_microphone",
            "mdi:microphone-off",
            audioInput.muted,
            EntityCategory.ENTITY_CATEGORY_NONE
        ) { audioInput.setMuted(it) })

        add(NumberEntity(
            key = "microphone_volume".hashCode(),
            name = context.getString(R.string.entity_microphone_volume),
            objectId = "microphone_volume",
            icon = "mdi:microphone",
            minValue = 0.0f,
            maxValue = 2.0f,
            step = 0.1f,
            getState = audioInput.microphoneVolume,
            setState = { volume ->
                audioInput.setMicrophoneVolume(volume)
                onMicrophoneVolumeChanged?.invoke(volume)
            },
            entityCategory = EntityCategory.ENTITY_CATEGORY_CONFIG
        ))

        add(MediaPlayerEntity(0, context.getString(R.string.entity_media_player), "media_player", player))

        add(SwitchEntity(
            2,
            context.getString(R.string.entity_wake_sound),
            "play_wake_sound",
            "mdi:bell-ring",
            player.enableWakeSound,
            EntityCategory.ENTITY_CATEGORY_NONE
        ) { player.enableWakeSound.set(it) })

        if (playerSettingsStore != null) {
            add(SwitchEntity(
                key = 39,
                name = "Continuous Conversation",
                objectId = "continuous_conversation",
                icon = "mdi:chat-processing",
                getState = playerSettingsStore.enableContinuousConversation,
                entityCategory = EntityCategory.ENTITY_CATEGORY_CONFIG
            ) { enabled -> playerSettingsStore.enableContinuousConversation.set(enabled) })

            add(soundSelect(context, 44, "Wake Sound", "wake_sound", playerSettingsStore.wakeSound))
            add(soundSelect(context, 45, "Wake Sound 2", "wake_sound_2", playerSettingsStore.wakeSound2))
            add(soundSelect(context, 46, "Stop Sound", "stop_sound", playerSettingsStore.stopSound))
            add(soundSelect(context, 47, "Timer Finished Sound", "timer_finished_sound", playerSettingsStore.timerFinishedSound))
            add(soundSelect(context, 48, "Continuous Prompt Sound", "continuous_prompt_sound", playerSettingsStore.continuousPromptSound))

            add(SwitchEntity(
                key = 40,
                name = "Action Button Independent",
                objectId = "action_button_independent",
                icon = "mdi:gesture-tap-button",
                getState = playerSettingsStore.actionButtonIndependent,
                entityCategory = EntityCategory.ENTITY_CATEGORY_CONFIG
            ) { enabled ->
                playerSettingsStore.actionButtonIndependent.set(enabled)
                actionButtonBridge.setIndependent(enabled)
            })
            add(BinarySensorEntity(
                key = 41,
                name = "Action Button Pressed",
                objectId = "action_button_pressed",
                icon = "mdi:gesture-tap-button",
                getState = actionButtonBridge.pressed
            ))
        }
    }

    private fun soundSelect(
        context: Context,
        key: Int,
        name: String,
        objectId: String,
        setting: SettingState<String>
    ) = SelectEntity(
        key = key,
        name = name,
        objectId = objectId,
        icon = "mdi:music-note",
        options = { SoundOptions.labels(context) },
        getState = setting.map { SoundOptions.labelForUri(context, it) },
        entityCategory = EntityCategory.ENTITY_CATEGORY_CONFIG
    ) { label ->
        val uri = SoundOptions.uriForLabel(context, label)
        if (uri == null) {
            false
        } else {
            setting.set(uri)
            true
        }
    }
}
