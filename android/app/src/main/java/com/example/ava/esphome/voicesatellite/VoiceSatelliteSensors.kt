package com.example.ava.esphome.voicesatellite

import android.content.Context
import com.example.ava.R
import com.example.ava.esphome.EspHomeDevice
import com.example.ava.esphome.entities.SensorEntity
import com.example.ava.sensor.EnvironmentSensorManager
import com.example.esphomeproto.api.EntityCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VoiceSatelliteSensors(
    private val context: Context,
    private val scope: CoroutineScope,
    private val device: EspHomeDevice
) {
    private var environmentSensorManager: EnvironmentSensorManager? = null
    private var lightSensorEntity: SensorEntity? = null
    private var sensorUpdateJob: Job? = null

    companion object {
        private const val SENSOR_UPDATE_INTERVAL_MS = 35_000L

        fun createLightSensorEntity(name: String) = SensorEntity(
            key = 20,
            name = name,
            objectId = "light_sensor",
            icon = "mdi:brightness-6",
            unitOfMeasurement = "lx",
            accuracyDecimals = 0,
            deviceClass = "illuminance",
            entityCategory = EntityCategory.ENTITY_CATEGORY_NONE
        )
    }

    fun init() {
        environmentSensorManager = EnvironmentSensorManager(context)
        val sensorManager = environmentSensorManager ?: return

        if (sensorManager.hasLightSensor) {
            lightSensorEntity = createLightSensorEntity(context.getString(R.string.entity_light_sensor))
            lightSensorEntity?.let { device.addEntity(it) }
        }

        sensorManager.startListening()
        startSensorUpdateLoop()
    }

    private fun startSensorUpdateLoop() {
        sensorUpdateJob?.cancel()
        sensorUpdateJob = scope.launch {
            delay(500)
            updateSensorValuesFiltered()

            while (true) {
                delay(SENSOR_UPDATE_INTERVAL_MS)
                updateSensorValues()
            }
        }
    }

    private suspend fun updateSensorValuesFiltered() {
        val manager = environmentSensorManager ?: return
        val lightSamples = mutableListOf<Float>()
        repeat(3) {
            lightSamples.add(manager.lightLevel.value)
            delay(100)
        }
        lightSensorEntity?.updateState(lightSamples.sorted()[1])
    }

    private fun updateSensorValues() {
        environmentSensorManager?.let { manager ->
            lightSensorEntity?.updateState(manager.lightLevel.value)
        }
    }

    fun stop() {
        sensorUpdateJob?.cancel()
        sensorUpdateJob = null
        environmentSensorManager?.stopListening()
        environmentSensorManager = null
    }
}
