package com.example.ava.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeIdentityTest {
    @Test
    fun derivesActionsFromTheAppliancePackage() {
        assertEquals(
            "net.mfuertes.biscuit.ava.ACTION_START_SERVICE",
            RuntimeIdentity.action("ACTION_START_SERVICE")
        )
    }
}
