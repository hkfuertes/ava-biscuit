package com.example.ava.esphome.voicesatellite

import android.content.Context
import com.example.ava.R
import com.example.ava.esphome.entities.BinarySensorEntity
import com.example.ava.esphome.entities.MediaPlayerEntity
import com.example.ava.esphome.entities.NumberEntity
import com.example.ava.esphome.entities.SwitchEntity
import com.example.ava.settings.PlayerSettingsStore
import com.example.esphomeproto.api.EntityCategory

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
                key = 40,
                name = "Action Button Independent",
                objectId = "action_button_independent",
                icon = "mdi:gesture-tap-button",
                getState = playerSettingsStore.actionButtonIndependent,
                entityCategory = EntityCategory.ENTITY_CATEGORY_NONE
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
}
