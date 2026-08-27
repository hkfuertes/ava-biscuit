package com.example.ava.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.ava.esphome.Disconnected
import com.example.ava.esphome.EspHomeState
import com.example.ava.esphome.Stopped
import com.example.ava.esphome.voicesatellite.AndroidSystemAudioAdapter
import com.example.ava.esphome.voicesatellite.BiscuitActionButtonBridge
import com.example.ava.esphome.voicesatellite.SystemStateBridge
import com.example.ava.esphome.voicesatellite.VoiceSatellite
import com.example.ava.esphome.voicesatellite.VoiceSatelliteAudioInput
import com.example.ava.esphome.voicesatellite.VoiceSatellitePlayer
import com.example.ava.esphome.voicesatellite.VoiceStateListener
import com.example.ava.microwakeword.AssetWakeWordProvider
import com.example.ava.notifications.createVoiceSatelliteServiceNotification
import com.example.ava.notifications.createVoiceSatelliteServiceNotificationChannel
import com.example.ava.nsd.NsdRegistration
import com.example.ava.nsd.registerVoiceSatelliteNsd
import com.example.ava.players.AudioPlayer
import com.example.ava.players.TtsPlayer
import com.example.ava.receivers.AvaControlReceiver
import com.example.ava.settings.ExperimentalSettingsStore
import com.example.ava.settings.MicrophoneSettingsStore
import com.example.ava.settings.PlayerSettingsStore
import com.example.ava.settings.VoiceSatelliteSettings
import com.example.ava.settings.VoiceSatelliteSettingsStore
import com.example.ava.settings.microphoneSettingsStore
import com.example.ava.settings.playerSettingsStore
import com.example.ava.settings.voiceSatelliteSettingsStore
import com.example.ava.utils.RootHelper
import com.example.ava.utils.translate
import com.example.ava.wakelocks.WifiWakeLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
class VoiceSatelliteService : LifecycleService() {
    private val wifiWakeLock = WifiWakeLock()
    private val actionButtonBridge = BiscuitActionButtonBridge.shared
    private val initializing = AtomicBoolean(false)
    private val voiceSatelliteNsd = AtomicReference<NsdRegistration?>(null)
    private var controlReceiver: BroadcastReceiver? = null
    private var systemStateBridgeJob: Job? = null

    private val satelliteSettingsStore by lazy { VoiceSatelliteSettingsStore(applicationContext.voiceSatelliteSettingsStore) }
    private val microphoneSettingsStore by lazy { MicrophoneSettingsStore(applicationContext.microphoneSettingsStore) }
    private val playerSettingsStore by lazy { PlayerSettingsStore(applicationContext.playerSettingsStore) }
    private val experimentalSettingsStore by lazy { ExperimentalSettingsStore(applicationContext) }

    internal val _voiceSatellite = MutableStateFlow<VoiceSatellite?>(null)
    val voiceSatelliteState = _voiceSatellite.flatMapLatest { it?.state ?: flowOf(Stopped) }

    class VoiceSatelliteBinder(val service: VoiceSatelliteService) : Binder()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return VoiceSatelliteBinder(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        actionButtonBridge.onLocalPress = { toggleManualAssist() }
        registerControlReceiver()
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (_voiceSatellite.value != null || !initializing.compareAndSet(false, true)) return START_STICKY
        createVoiceSatelliteServiceNotificationChannel(this)
        startForegroundCompat("Starting...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                satelliteSettingsStore.ensureMacAddressIsSet()
                val settings = satelliteSettingsStore.get()
                updateNotification(Stopped)

                val satellite = createVoiceSatellite(settings)
                _voiceSatellite.value = satellite
                satellite.start()

                voiceSatelliteNsd.getAndSet(null)?.unregister(this@VoiceSatelliteService)
                voiceSatelliteNsd.set(registerVoiceSatelliteNsd(settings))
                wifiWakeLock.create(applicationContext, TAG)
                wifiWakeLock.acquire()
                startSettingsWatcher()
                updateNotificationOnStateChanges()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start voice satellite", e)
                stopForegroundCompat()
                stopSelf()
            } finally {
                initializing.set(false)
            }
        }
        return START_STICKY
    }

    fun startVoiceSatellite() {
        getSharedPreferences("ava_prefs", MODE_PRIVATE).edit()
            .putBoolean("service_user_stopped", false)
            .apply()
        RootHelper.installBootScript(packageName, VoiceSatelliteService::class.java.name)
        val serviceIntent = Intent(this, VoiceSatelliteService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent) else startService(serviceIntent)
    }

    fun stopVoiceSatellite() {
        getSharedPreferences("ava_prefs", MODE_PRIVATE).edit()
            .putBoolean("service_user_stopped", true)
            .apply()
        _voiceSatellite.value?.close()
        _voiceSatellite.value = null
        stopSystemStateBridge()
        voiceSatelliteNsd.getAndSet(null)?.unregister(this)
        wifiWakeLock.release()
        stopForegroundCompat()
        initializing.set(false)
    }

    fun restartVoiceSatellite() {
        lifecycleScope.launch {
            stopVoiceSatellite()
            delay(500)
            startVoiceSatellite()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val userStopped = getSharedPreferences("ava_prefs", MODE_PRIVATE)
            .getBoolean("service_user_stopped", false)
        if (!userStopped && _voiceSatellite.value != null) startVoiceSatellite()
    }

    override fun onDestroy() {
        instance = null
        actionButtonBridge.onLocalPress = {}
        controlReceiver?.let { runCatching { unregisterReceiver(it) } }
        controlReceiver = null
        stopVoiceSatellite()
        RootHelper.removeBootScript()
        super.onDestroy()
    }

    private suspend fun createVoiceSatellite(settings: VoiceSatelliteSettings): VoiceSatellite {
        val microphoneSettings = microphoneSettingsStore.get()
        val playerSettings = playerSettingsStore.get()
        val audioInput = VoiceSatelliteAudioInput(
            activeWakeWords = microphoneSettings.wakeWords.ifEmpty { listOf(microphoneSettings.wakeWord) },
            activeStopWords = listOf(microphoneSettings.stopWord),
            wakeWordProvider = AssetWakeWordProvider(assets),
            stopWordProvider = AssetWakeWordProvider(assets, "stopWords"),
            muted = microphoneSettings.muted
        )
        val player = VoiceSatellitePlayer(
            ttsPlayer = TtsPlayer(createAudioPlayer()),
            mediaPlayer = createAudioPlayer(AudioManager.AUDIOFOCUS_GAIN),
            wakeSoundPlayer = createAudioPlayer(),
            volume = playerSettings.volume,
            muted = playerSettings.muted,
            enableWakeSound = playerSettingsStore.enableWakeSound,
            wakeSound = playerSettingsStore.wakeSound,
            wakeSound2 = playerSettingsStore.wakeSound2,
            timerFinishedSound = playerSettingsStore.timerFinishedSound,
            stopSound = playerSettingsStore.stopSound,
            enableStopSound = playerSettingsStore.enableStopSound,
            continuousPromptSound = playerSettingsStore.continuousPromptSound,
            enableContinuousConversation = playerSettingsStore.enableContinuousConversation
        )

        actionButtonBridge.setIndependent(playerSettings.actionButtonIndependent)
        val bridge = SystemStateBridge(
            AndroidSystemAudioAdapter(getSystemService(AUDIO_SERVICE) as AudioManager),
            object : VoiceStateListener {
                override fun onMuteStateChanged(muted: Boolean) {
                    audioInput.setMutedFromSystem(muted)
                    lifecycleScope.launch(Dispatchers.IO) { microphoneSettingsStore.muted.set(muted) }
                }

                override fun onVolumeChanged(normalizedVolume: Float) {
                    player.setVolumeFromSystem(normalizedVolume)
                    lifecycleScope.launch(Dispatchers.IO) { playerSettingsStore.volume.set(normalizedVolume) }
                }
            }
        )
        audioInput.onSetMuted = bridge::onHaMuteCommand
        player.onSetVolume = bridge::onHaVolumeCommand
        bridge.pollAndPublish()
        startSystemStateBridge(bridge)

        return VoiceSatellite(
            coroutineContext = lifecycleScope.coroutineContext,
            name = settings.name,
            port = settings.serverPort,
            audioInput = audioInput,
            player = player,
            settingsStore = satelliteSettingsStore,
            experimentalSettingsStore = experimentalSettingsStore,
            playerSettingsStore = playerSettingsStore,
            context = this
        )
    }

    private fun createAudioPlayer(focusGain: Int = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK): AudioPlayer {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        return AudioPlayer(audioManager, focusGain) {
            ExoPlayer.Builder(this)
                .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(this)))
                .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(500, 2000, 100, 100).build())
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(USAGE_MEDIA).setContentType(AUDIO_CONTENT_TYPE_MUSIC).build(),
                    true
                )
                .build()
        }
    }

    private fun startSettingsWatcher() {
        _voiceSatellite.flatMapLatest { satellite ->
            if (satellite == null) emptyFlow() else merge(
                satellite.audioInput.activeWakeWords.drop(1).onEach { words ->
                    if (words.isNotEmpty()) microphoneSettingsStore.wakeWords.set(words)
                },
                microphoneSettingsStore.wakeWords.drop(1).onEach { words ->
                    if (words.isNotEmpty() && words.toSet() != satellite.audioInput.activeWakeWords.value.toSet()) {
                        satellite.audioInput.setActiveWakeWords(words)
                    }
                },
                playerSettingsStore.actionButtonIndependent.drop(1).onEach { enabled ->
                    actionButtonBridge.setIndependent(enabled)
                }
            )
        }.launchIn(lifecycleScope)
    }

    private fun updateNotificationOnStateChanges() {
        voiceSatelliteState.onEach { updateNotification(it) }.launchIn(lifecycleScope)
    }

    private fun updateNotification(state: EspHomeState) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(
            NOTIFICATION_ID,
            createVoiceSatelliteServiceNotification(this, state.translate(resources))
        )
    }

    private fun registerVoiceSatelliteNsd(settings: VoiceSatelliteSettings) = registerVoiceSatelliteNsd(
        context = this,
        name = settings.name,
        port = settings.serverPort,
        macAddress = settings.macAddress,
        onNameChanged = { newName -> lifecycleScope.launch { satelliteSettingsStore.saveName(newName) } }
    )

    private fun registerControlReceiver() {
        controlReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    AvaControlReceiver.ACTION_TOGGLE_MIC -> toggleMicMute()
                    AvaControlReceiver.ACTION_MUTE_MIC -> setMicMute(true)
                    AvaControlReceiver.ACTION_UNMUTE_MIC -> setMicMute(false)
                    AvaControlReceiver.ACTION_WAKE -> manualWake()
                    AvaControlReceiver.ACTION_STOP -> stopVoiceSession()
                    AvaControlReceiver.ACTION_BUTTON_DOWN -> setActionButtonPressed(true)
                    AvaControlReceiver.ACTION_BUTTON_UP -> setActionButtonPressed(false)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(AvaControlReceiver.ACTION_TOGGLE_MIC)
            addAction(AvaControlReceiver.ACTION_MUTE_MIC)
            addAction(AvaControlReceiver.ACTION_UNMUTE_MIC)
            addAction(AvaControlReceiver.ACTION_WAKE)
            addAction(AvaControlReceiver.ACTION_STOP)
            addAction(AvaControlReceiver.ACTION_BUTTON_DOWN)
            addAction(AvaControlReceiver.ACTION_BUTTON_UP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(controlReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(controlReceiver, filter)
        }
    }

    private fun startSystemStateBridge(bridge: SystemStateBridge) {
        systemStateBridgeJob?.cancel()
        systemStateBridgeJob = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                bridge.pollAndPublish()
                delay(500)
            }
        }
    }

    private fun stopSystemStateBridge() {
        systemStateBridgeJob?.cancel()
        systemStateBridgeJob = null
    }

    private fun startForegroundCompat(text: String) {
        val notification = createVoiceSatelliteServiceNotification(this, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE) else stopForeground(true)
    }

    fun triggerManualWake() = _voiceSatellite.value?.triggerManualWake()
    fun toggleManualAssist() = _voiceSatellite.value?.toggleManualAssist()
    fun getState(): EspHomeState = _voiceSatellite.value?.state?.value ?: Disconnected
    fun onScreenTouch(isTouching: Boolean) = _voiceSatellite.value?.onScreenTouch(isTouching)
    fun setActionButtonPressed(pressed: Boolean) = actionButtonBridge.onPhysicalButton(pressed)
    suspend fun callHaService(service: String, entityId: String) = _voiceSatellite.value?.callHaServicePublic(service, entityId)
    fun getQuickEntityStates(): Map<String, String> = _voiceSatellite.value?.getQuickEntityStateCache() ?: emptyMap()
    fun getQuickEntityUnits(): Map<String, String> = _voiceSatellite.value?.getQuickEntityUnitCache() ?: emptyMap()
    suspend fun resubscribeQuickEntities() = _voiceSatellite.value?.subscribeQuickEntities()

    companion object {
        const val TAG = "VoiceSatelliteService"
        private const val NOTIFICATION_ID = 2
        private val actionKeyCodes = setOf(KeyEvent.KEYCODE_HELP)
        private var instance: VoiceSatelliteService? = null
        internal fun isActionButtonKeyCode(keyCode: Int) = keyCode in actionKeyCodes
        fun getInstance(): VoiceSatelliteService? = instance
        fun toggleMicMute() = instance?._voiceSatellite?.value?.toggleMicMute()
        fun setMicMute(muted: Boolean) = instance?._voiceSatellite?.value?.setMicMute(muted)
        fun manualWake() = instance?._voiceSatellite?.value?.manualWake()
        fun stopVoiceSession() = instance?._voiceSatellite?.value?.stopVoiceSession()
        fun setActionButtonPressed(pressed: Boolean) = BiscuitActionButtonBridge.shared.onPhysicalButton(pressed)
    }
}
