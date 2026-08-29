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

    fun showTimerProgress(remainingMs: Long, totalMs: Long) {
        if (!isValidCountdown(remainingMs, totalMs)) return
        startBiscuitService(COUNTDOWN_PROGRESS) {
            putExtra(EXTRA_COUNTDOWN_REMAINING_MS, remainingMs)
            putExtra(EXTRA_COUNTDOWN_TOTAL_MS, totalMs)
        }
    }

    fun clearTimerProgress() {
        startBiscuitService(COUNTDOWN_CLEAR)
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

    private fun startBiscuitService(action: String, configure: Intent.() -> Unit = {}) {
        runCatching {
            appContext.startService(Intent(action).setComponent(ComponentName(PACKAGE, SERVICE_CLASS)).apply(configure))
        }.onFailure {
            Log.w(TAG, "Biscuit service action failed: $action", it)
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
        private const val SERVICE_CLASS = "com.amazon.biscuit.service.BiscuitService"
        private const val COUNTDOWN_PROGRESS = "com.amazon.biscuit.service.COUNTDOWN_PROGRESS"
        private const val COUNTDOWN_CLEAR = "com.amazon.biscuit.service.COUNTDOWN_CLEAR"
        private const val EXTRA_COUNTDOWN_REMAINING_MS = "com.amazon.biscuit.service.EXTRA_COUNTDOWN_REMAINING_MS"
        private const val EXTRA_COUNTDOWN_TOTAL_MS = "com.amazon.biscuit.service.EXTRA_COUNTDOWN_TOTAL_MS"

        internal fun animationFor(state: EspHomeState): String? = when (state) {
            Listening -> "solid_cyan"
            Processing -> "alexa_thinking"
            Responding -> "solid_blue"
            else -> null
        }

        internal fun isValidCountdown(remainingMs: Long, totalMs: Long) = totalMs > 0 && remainingMs in 0..totalMs
    }
}
