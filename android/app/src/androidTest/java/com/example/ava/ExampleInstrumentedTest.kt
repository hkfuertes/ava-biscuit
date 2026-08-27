package com.example.ava

import android.content.ComponentName
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ava.receivers.AvaControlReceiver
import com.example.ava.receivers.BootReceiver
import com.example.ava.services.VoiceSatelliteService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun applianceIsHeadlessAndBootable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager

        assertEquals("net.mfuertes.biscuit.ava", context.packageName)
        assertNull(packageManager.getLaunchIntentForPackage(context.packageName))
        assertTrue(packageManager.getServiceInfo(ComponentName(context, VoiceSatelliteService::class.java), 0).enabled)
        assertTrue(packageManager.getReceiverInfo(ComponentName(context, BootReceiver::class.java), 0).enabled)

        val control = Intent(AvaControlReceiver.ACTION_START_SERVICE).setPackage(context.packageName)
        assertTrue(packageManager.queryBroadcastReceivers(control, 0).isNotEmpty())
    }
}
