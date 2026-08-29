package com.example.ava.esphome

import com.example.ava.esphome.entities.BinarySensorEntity
import com.example.ava.esphome.entities.ButtonEntity
import com.example.ava.esphome.entities.Entity
import com.example.ava.esphome.entities.MediaPlayerEntity
import com.example.ava.esphome.entities.NumberEntity
import com.example.ava.esphome.entities.SelectEntity
import com.example.ava.esphome.entities.SensorEntity
import com.example.ava.esphome.entities.SwitchEntity
import com.example.ava.esphome.entities.TextSensorEntity

object ApplianceEntityAllowlist {
    val allowedObjectIds = setOf(
        "mute_microphone",
        "microphone_volume",
        "media_player",
        "wake_sound",
        "wake_sound_2",
        "stop_sound",
        "timer_finished_sound",
        "continuous_prompt_sound",
        "continuous_conversation",
        "assist_status",
        "timer",
        "start_assist",
        "action_button_independent",
        "action_button_pressed",
        "light_sensor"
    )

    fun isAllowed(entity: Entity) = objectIdOf(entity) in allowedObjectIds

    fun objectIdOf(entity: Entity): String = when (entity) {
        is BinarySensorEntity -> entity.objectId
        is ButtonEntity -> entity.objectId
        is MediaPlayerEntity -> entity.objectId
        is NumberEntity -> entity.objectId
        is SelectEntity -> entity.objectId
        is SensorEntity -> entity.objectId
        is SwitchEntity -> entity.objectId
        is TextSensorEntity -> entity.objectId
        else -> ""
    }
}
