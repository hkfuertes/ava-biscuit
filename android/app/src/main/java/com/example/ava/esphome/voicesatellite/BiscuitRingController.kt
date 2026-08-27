package com.example.ava.esphome.voicesatellite

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.amazon.biscuit.service.IBiscuitService
import com.example.ava.esphome.EspHomeState

class BiscuitRingController(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private var bound = false
    private var applied = false
    private var appliedAnimation: String? = null
    private var desiredAnimation: String? = null
    @Volatile private var service: IBiscuitService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IBiscuitService.Stub.asInterface(binder)
            applied = false
            applyDesired()
            Log.i(TAG, "Biscuit ring service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            applied = false
            Log.w(TAG, "Biscuit ring service disconnected")
        }
    }

    fun start() {
        if (bound) return
        bound = runCatching {
            appContext.bindService(Intent(ACTION_BIND).setPackage(PACKAGE), connection, Context.BIND_AUTO_CREATE)
        }.getOrElse {
            Log.w(TAG, "Biscuit ring bind failed", it)
            false
        }
    }

    fun show(state: EspHomeState) {
        desiredAnimation = animationFor(state)
        applyDesired()
    }

    private fun applyDesired() {
        val next = desiredAnimation
        if (applied && appliedAnimation == next) return
        val svc = service ?: return
        val ok = runCatching { next?.let(svc::play) ?: svc.clear() }.getOrDefault(false)
        if (ok) {
            applied = true
            appliedAnimation = next
        }
    }

    override fun close() {
        desiredAnimation = null
        applyDesired()
        if (bound) runCatching { appContext.unbindService(connection) }
        bound = false
        service = null
    }

    companion object {
        private const val TAG = "BiscuitRingController"
        private const val PACKAGE = "com.amazon.biscuit.service"
        private const val ACTION_BIND = "com.amazon.biscuit.service.IBiscuitService"

        internal fun animationFor(state: EspHomeState): String? = when (state) {
            Listening -> "solid_cyan"
            Processing -> "alexa_thinking"
            Responding -> "solid_blue"
            else -> null
        }
    }
}
