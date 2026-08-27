package com.example.ava.esphome.voicesatellite

import com.example.esphomeproto.api.SensorStateResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceSatelliteSensorsTest {
    @Test
    fun lightSensorEntityPublishesRealValue() = runBlocking {
        val entity = VoiceSatelliteSensors.createLightSensorEntity("Light Sensor")

        entity.updateState(123f)
        val state = entity.subscribe().first() as SensorStateResponse

        assertEquals("light_sensor", entity.objectId)
        assertEquals(123f, state.state, 0.001f)
    }
}
