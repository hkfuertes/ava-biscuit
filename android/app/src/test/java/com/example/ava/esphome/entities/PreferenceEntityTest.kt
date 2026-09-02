package com.example.ava.esphome.entities

import com.example.esphomeproto.api.EntityCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenceEntityTest {
    @Test
    fun mapsHaCategoriesAndFallbacks() {
        assertEquals(PreferenceRowCategory.CONFIGURATION, rowCategory(EntityCategory.ENTITY_CATEGORY_CONFIG, PreferenceRowCategory.SENSORS))
        assertEquals(PreferenceRowCategory.DIAGNOSTICS, rowCategory(EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC, PreferenceRowCategory.SENSORS))
        assertEquals(PreferenceRowCategory.SENSORS, rowCategory(EntityCategory.ENTITY_CATEGORY_NONE, PreferenceRowCategory.SENSORS))
    }

    @Test
    fun formatsReadableStateText() {
        assertEquals("On", formatOnOff(true))
        assertEquals("Off", formatOnOff(false))
        assertEquals("12 lx", formatMeasurement(12f, "lx"))
        assertEquals("0.5", formatMeasurement(0.5f, ""))
    }
}
