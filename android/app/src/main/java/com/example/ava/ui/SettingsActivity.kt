package com.example.ava.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceCategory
import com.example.ava.R
import com.example.ava.esphome.entities.PreferenceEntity
import com.example.ava.esphome.entities.PreferenceRowCategory
import com.example.ava.esphome.voicesatellite.VoiceSatellite
import com.example.ava.services.VoiceSatelliteService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Launcher-visible, read-only mirror of the HA-visible entities. Rows are built from whatever
 * VoiceSatelliteService already exposes to Home Assistant - no hardcoded setting list, no writes.
 */
@Suppress("DEPRECATION")
class SettingsActivity : PreferenceActivity() {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + Job())
    private val rowJobs = mutableListOf<Job>()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as? VoiceSatelliteService.VoiceSatelliteBinder)?.service ?: return
            service.voiceSatelliteInstance.onEach { render(it) }.launchIn(scope)
        }
        override fun onServiceDisconnected(name: ComponentName?) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render(null)
        val intent = Intent(this, VoiceSatelliteService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        runCatching { unbindService(connection) }
        scope.cancel()
        super.onDestroy()
    }

    /** satellite == null covers both "not started yet" and "stopped again": both show the loading row. */
    private fun render(satellite: VoiceSatellite?) {
        rowJobs.forEach { it.cancel() }
        rowJobs.clear()
        val screen = preferenceManager.createPreferenceScreen(this)
        val rows = satellite?.snapshotEntities()?.filterIsInstance<PreferenceEntity>()
        if (rows == null) {
            screen.addPreference(Preference(this).apply {
                title = getString(R.string.settings_starting)
                isSelectable = false
            })
        } else {
            for (bucket in BUCKET_ORDER) {
                val bucketRows = rows.filter { it.preferenceRow.category == bucket }
                if (bucketRows.isEmpty()) continue
                val category = PreferenceCategory(this).apply { title = getString(titleFor(bucket)) }
                screen.addPreference(category)
                bucketRows.forEach { category.addPreference(buildPreference(it)) }
            }
        }
        preferenceScreen = screen
    }

    private fun buildPreference(entity: PreferenceEntity): Preference {
        val row = entity.preferenceRow
        return Preference(this).apply {
            key = row.objectId
            title = row.name
            summary = row.objectId
            isSelectable = false
            row.stateText?.onEach { text -> summary = "${row.objectId} \u00b7 $text" }
                ?.launchIn(scope)
                ?.let(rowJobs::add)
        }
    }

    private fun titleFor(bucket: PreferenceRowCategory) = when (bucket) {
        PreferenceRowCategory.CONFIGURATION -> R.string.settings_category_configuration
        PreferenceRowCategory.SENSORS -> R.string.settings_category_sensors
        PreferenceRowCategory.DIAGNOSTICS -> R.string.settings_category_diagnostics
        PreferenceRowCategory.CONTROLS -> R.string.settings_category_controls
    }

    companion object {
        private val BUCKET_ORDER = listOf(
            PreferenceRowCategory.CONFIGURATION,
            PreferenceRowCategory.SENSORS,
            PreferenceRowCategory.DIAGNOSTICS,
            PreferenceRowCategory.CONTROLS
        )
    }
}
