package com.example.ava.esphome.voicesatellite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiscuitActionButtonBridgeTest {
    @Test
    fun normalModePressPublishesAndRunsLocalAssistOnce() {
        var localPresses = 0
        val bridge = BiscuitActionButtonBridge { localPresses++ }

        bridge.onPhysicalButton(true)
        bridge.onPhysicalButton(true)

        assertTrue(bridge.pressed.value)
        assertEquals(1, localPresses)
    }

    @Test
    fun independentModePressPublishesWithoutLocalAssist() {
        var localPresses = 0
        val bridge = BiscuitActionButtonBridge { localPresses++ }
        bridge.setIndependent(true)

        bridge.onPhysicalButton(true)

        assertTrue(bridge.pressed.value)
        assertEquals(0, localPresses)
    }

    @Test
    fun releasePublishesFalseWithoutLocalAssist() {
        var localPresses = 0
        val bridge = BiscuitActionButtonBridge { localPresses++ }

        bridge.onPhysicalButton(true)
        bridge.onPhysicalButton(false)

        assertFalse(bridge.pressed.value)
        assertEquals(1, localPresses)
    }
}
