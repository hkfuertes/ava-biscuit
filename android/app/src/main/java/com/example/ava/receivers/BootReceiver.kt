package com.example.ava.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.utils.RootHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in BOOT_ACTIONS) return
        val pendingResult = goAsync()
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                startServiceWithRetry(context.applicationContext, 3)
            } finally {
                pendingResult.finish()
            }
        }, 3000L)
    }

    private fun startServiceWithRetry(context: Context, maxRetries: Int) {
        var retryCount = 0
        fun tryStart() {
            try {
                val serviceIntent = Intent(context, VoiceSatelliteService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (_: Exception) {
                if (!RootHelper.startServiceWithRoot(context.packageName, VoiceSatelliteService::class.java.name)) {
                    retryCount++
                    if (retryCount < maxRetries) {
                        Handler(Looper.getMainLooper()).postDelayed({ tryStart() }, 2000L * retryCount)
                    }
                }
            }
        }
        tryStart()
    }

    companion object {
        private val BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON"
        )
    }
}
