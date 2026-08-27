package com.example.ava.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class ExperimentalSettings(
    val sensorUpdateInterval: Int = 35
)

val Context.experimentalSettingsDataStore: DataStore<ExperimentalSettings> by dataStore(
    fileName = "experimental_settings.json",
    serializer = SettingsSerializer(ExperimentalSettings.serializer(), ExperimentalSettings()),
    corruptionHandler = defaultCorruptionHandler(ExperimentalSettings())
)

class ExperimentalSettingsStore(context: Context) : SettingsStoreImpl<ExperimentalSettings>(
    context.experimentalSettingsDataStore,
    ExperimentalSettings()
) {
    val sensorUpdateInterval = SettingState(getFlow().map { it.sensorUpdateInterval }) { value ->
        update { it.copy(sensorUpdateInterval = value.coerceIn(10, 60)) }
    }
}
