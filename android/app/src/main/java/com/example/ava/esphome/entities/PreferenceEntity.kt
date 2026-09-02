package com.example.ava.esphome.entities

import com.example.esphomeproto.api.EntityCategory
import kotlinx.coroutines.flow.Flow

/**
 * Read-only grouping for the mirrored Settings Activity. ESPHome/HA only carries Config/Diagnostic
 * as entity_category; Sensors/Controls are inferred from entity type so the UI buckets roughly the
 * way HA's own UI does.
 */
enum class PreferenceRowCategory { CONFIGURATION, SENSORS, DIAGNOSTICS, CONTROLS }

/** One read-only settings row. No write path in v1: state is display text only, no setters. */
data class PreferenceRow(
    val objectId: String,
    val name: String,
    val category: PreferenceRowCategory,
    val stateText: Flow<String>? = null
)

/** Implemented by HA entity classes so the Settings Activity can mirror them without a hardcoded list. */
interface PreferenceEntity {
    val preferenceRow: PreferenceRow
}

internal fun rowCategory(entityCategory: EntityCategory, fallback: PreferenceRowCategory) = when (entityCategory) {
    EntityCategory.ENTITY_CATEGORY_CONFIG -> PreferenceRowCategory.CONFIGURATION
    EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC -> PreferenceRowCategory.DIAGNOSTICS
    else -> fallback
}

internal fun formatOnOff(value: Boolean) = if (value) "On" else "Off"

internal fun formatMeasurement(value: Float, unit: String): String {
    val text = if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()
    return if (unit.isEmpty()) text else "$text $unit"
}
