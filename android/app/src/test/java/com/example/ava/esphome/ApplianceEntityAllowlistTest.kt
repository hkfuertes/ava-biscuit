package com.example.ava.esphome

import com.example.ava.esphome.entities.ButtonEntity
import com.example.ava.esphome.entities.SelectEntity
import com.example.ava.esphome.entities.SensorEntity
import com.example.ava.esphome.entities.SwitchEntity
import com.example.ava.esphome.entities.TextSensorEntity
import com.example.esphomeproto.api.DeviceInfoResponse
import com.example.esphomeproto.api.EntityCategory
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Job
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplianceEntityAllowlistTest {
    @Test
    fun allowsRequiredApplianceEntities() {
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("mute_microphone"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("microphone_volume"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("media_player"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("play_wake_sound"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("wake_sound"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("wake_sound_2"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("stop_sound"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("timer_finished_sound"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("continuous_prompt_sound"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("continuous_conversation"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("assist_status"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("start_assist"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("action_button_independent"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("action_button_pressed"))
        assertTrue(ApplianceEntityAllowlist.allowedObjectIds.contains("light_sensor"))
    }

    @Test
    fun rejectsOptionalProductEntities() {
        val screenToggle = SwitchEntity(
            key = 3,
            name = "Screen Toggle",
            objectId = "screen_toggle",
            getState = flowOf(false),
            entityCategory = EntityCategory.ENTITY_CATEGORY_NONE,
            setState = {}
        )
        val magneticSensor = SensorEntity(
            key = 21,
            name = "Magnetic Sensor",
            objectId = "magnetic_sensor"
        )
        val startAssist = ButtonEntity(
            key = 42,
            name = "Start Assist",
            objectId = "start_assist",
            press = {}
        )
        val assistStatus = TextSensorEntity(
            key = 43,
            name = "Assist Status",
            objectId = "assist_status"
        )
        val wakeSound = SelectEntity(
            key = 44,
            name = "Wake Sound",
            objectId = "wake_sound",
            options = { listOf("Wake Word Triggered") },
            getState = flowOf("Wake Word Triggered"),
            setState = { true }
        )

        assertTrue(ApplianceEntityAllowlist.isAllowed(startAssist))
        assertTrue(ApplianceEntityAllowlist.isAllowed(assistStatus))
        assertTrue(ApplianceEntityAllowlist.isAllowed(wakeSound))
        assertFalse(ApplianceEntityAllowlist.isAllowed(screenToggle))
        assertFalse(ApplianceEntityAllowlist.isAllowed(magneticSensor))

        val device = TestDevice()
        device.register(screenToggle)
        device.register(magneticSensor)
        assertTrue(device.registeredObjectIds.isEmpty())
    }

    private class TestDevice : EspHomeDevice(Job(), "test", 0) {
        val registeredObjectIds get() = entities.map(ApplianceEntityAllowlist::objectIdOf)
        fun register(entity: com.example.ava.esphome.entities.Entity) = addEntity(entity)
        override suspend fun getDeviceInfo(): DeviceInfoResponse = DeviceInfoResponse.getDefaultInstance()
    }
}
