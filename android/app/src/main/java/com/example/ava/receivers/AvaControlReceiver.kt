package com.example.ava.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.utils.RuntimeIdentity

/** Controls the appliance through actions derived from its installed package. */
class AvaControlReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AvaControlReceiver"
        
        val ACTION_TOGGLE_MIC = RuntimeIdentity.action("ACTION_TOGGLE_MIC")
        val ACTION_MUTE_MIC = RuntimeIdentity.action("ACTION_MUTE_MIC")
        val ACTION_UNMUTE_MIC = RuntimeIdentity.action("ACTION_UNMUTE_MIC")
        val ACTION_WAKE = RuntimeIdentity.action("ACTION_WAKE")
        val ACTION_STOP = RuntimeIdentity.action("ACTION_STOP")
        val ACTION_START_SERVICE = RuntimeIdentity.action("ACTION_START_SERVICE")
        val ACTION_STOP_SERVICE = RuntimeIdentity.action("ACTION_STOP_SERVICE")
        val ACTION_BUTTON_DOWN = RuntimeIdentity.action("ACTION_BUTTON_DOWN")
        val ACTION_BUTTON_UP = RuntimeIdentity.action("ACTION_BUTTON_UP")
        const val ACTION_GLOBAL_BUTTON = "android.intent.action.GLOBAL_BUTTON"
        const val ACTION_BISCUIT_BUTTON_PRESSED = "com.amazon.device.intent.action.BUTTON_PRESSED"
        const val ACTION_BISCUIT_BUTTON_RELEASED = "com.amazon.device.intent.action.BUTTON_RELEASED"
        const val EXTRA_BISCUIT_BUTTON_NAME = "com.amazon.device.intent.extra.BUTTON_NAME"
        const val EXTRA_BISCUIT_KEY_CODE = "com.amazon.device.intent.extra.KEY_CODE"
        const val EXTRA_BISCUIT_SCAN_CODE = "com.amazon.device.intent.extra.SCAN_CODE"
        private const val BUTTON_NAME_HELP = "KEYCODE_HELP"
        private const val SCAN_CODE_HELP = 138

        internal fun biscuitButtonPressed(action: String?) = when (action) {
            ACTION_BISCUIT_BUTTON_PRESSED -> true
            ACTION_BISCUIT_BUTTON_RELEASED -> false
            else -> null
        }

        internal fun isBiscuitHelpButton(buttonName: String?, keyCode: Int, scanCode: Int) =
            buttonName == BUTTON_NAME_HELP && keyCode == KeyEvent.KEYCODE_HELP && scanCode == SCAN_CODE_HELP
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "Received action: $action")
        
        if (action == ACTION_GLOBAL_BUTTON) {
            @Suppress("DEPRECATION")
            val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
            if (event.keyCode != KeyEvent.KEYCODE_HELP) return
            when (event.action) {
                KeyEvent.ACTION_DOWN -> VoiceSatelliteService.setActionButtonPressed(true)
                KeyEvent.ACTION_UP -> VoiceSatelliteService.setActionButtonPressed(false)
            }
            return
        }

        biscuitButtonPressed(action)?.let { pressed ->
            if (!isBiscuitHelpButton(
                    intent.getStringExtra(EXTRA_BISCUIT_BUTTON_NAME),
                    intent.getIntExtra(EXTRA_BISCUIT_KEY_CODE, 0),
                    intent.getIntExtra(EXTRA_BISCUIT_SCAN_CODE, 0),
                )
            ) return
            VoiceSatelliteService.setActionButtonPressed(pressed)
            return
        }

        when (action) {
            ACTION_TOGGLE_MIC -> {
                Log.d(TAG, "Toggling microphone mute state")
                VoiceSatelliteService.toggleMicMute()
            }
            ACTION_MUTE_MIC -> {
                Log.d(TAG, "Muting microphone")
                VoiceSatelliteService.setMicMute(true)
            }
            ACTION_UNMUTE_MIC -> {
                Log.d(TAG, "Unmuting microphone")
                VoiceSatelliteService.setMicMute(false)
            }
            ACTION_WAKE -> {
                Log.d(TAG, "Manual wake triggered")
                VoiceSatelliteService.manualWake()
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping voice session")
                VoiceSatelliteService.stopVoiceSession()
            }
            ACTION_START_SERVICE -> {
                Log.d(TAG, "Starting VoiceSatelliteService")
                val serviceIntent = Intent(context, VoiceSatelliteService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start service", e)
                }
            }
            ACTION_STOP_SERVICE -> {
                Log.d(TAG, "Stopping VoiceSatelliteService")
                val serviceIntent = Intent(context, VoiceSatelliteService::class.java)
                context.stopService(serviceIntent)
            }
            ACTION_BUTTON_DOWN -> {
                Log.d(TAG, "Action button pressed")
                VoiceSatelliteService.setActionButtonPressed(true)
            }
            ACTION_BUTTON_UP -> {
                Log.d(TAG, "Action button released")
                VoiceSatelliteService.setActionButtonPressed(false)
            }
            else -> {
                Log.w(TAG, "Unknown action: $action")
            }
        }
    }
}
