package com.example.ava.esphome.voicesatellite

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.ava.esphome.Connected
import com.example.ava.esphome.Disconnected
import com.example.ava.esphome.EspHomeDevice
import com.example.ava.esphome.EspHomeState
import com.example.ava.esphome.Stopped
import com.example.ava.esphome.entities.ButtonEntity
import com.example.ava.settings.PlayerSettingsStore
import com.example.ava.settings.VoiceSatelliteSettingsStore
import com.example.esphomeproto.api.DeviceInfoResponse
import com.example.esphomeproto.api.SubscribeVoiceAssistantRequest
import com.example.esphomeproto.api.VoiceAssistantAnnounceFinished
import com.example.esphomeproto.api.VoiceAssistantAnnounceRequest
import com.example.esphomeproto.api.VoiceAssistantAudio
import com.example.esphomeproto.api.VoiceAssistantConfigurationRequest
import com.example.esphomeproto.api.VoiceAssistantConfigurationResponse
import com.example.esphomeproto.api.VoiceAssistantEventResponse
import com.example.esphomeproto.api.VoiceAssistantFeature
import com.example.esphomeproto.api.VoiceAssistantRequest
import com.example.esphomeproto.api.VoiceAssistantResponse
import com.example.esphomeproto.api.VoiceAssistantSetConfiguration
import com.example.esphomeproto.api.VoiceAssistantWakeWord
import com.google.protobuf.MessageLite
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

class VoiceSatellite(
    coroutineContext: CoroutineContext,
    name: String,
    port: Int,
    val audioInput: VoiceSatelliteAudioInput,
    val player: VoiceSatellitePlayer,
    private val settingsStore: VoiceSatelliteSettingsStore,
    private val playerSettingsStore: PlayerSettingsStore,
    private val context: Context
) : EspHomeDevice(
    coroutineContext,
    name,
    port,
    VoiceSatelliteEntities.buildEntities(
        audioInput = audioInput,
        player = player,
        context = context,
        playerSettingsStore = playerSettingsStore
    )
) {
    private val startGeneration = AtomicInteger()
    @Volatile private var pendingStartGeneration = 0
    @Volatile private var pendingStartWakeWordPhrase: String? = null
    @Volatile private var pendingStartWakeWordIndex = 0
    @Volatile private var lastConversationId = ""
    @Volatile private var continueConversationRequested = false
    @Volatile private var listeningCueReady = false
    @Volatile private var listeningCueWakeWordPhrase: String? = null
    @Volatile private var listeningCueWakeWordIndex = 0
    private val sensors = VoiceSatelliteSensors(context, scope, this)
    private val ring = BiscuitRingController(context)
    private val stateMachine = VoiceSatelliteStateMachine(
        scope = scope,
        audioInput = audioInput,
        player = player,
        state = _state,
        onStopSatellite = { stopVoiceSession() },
        onTtsFinished = { finishVoiceResponse() },
        onIntentEnd = { handleIntentEnd(it) },
        onListeningStarted = { playListeningCue() }
    )

    init {
        addEntity(ButtonEntity(
            key = 42,
            name = "Start/Stop Assist",
            objectId = "start_assist",
            icon = "mdi:account-voice"
        ) { toggleManualAssist() })
    }

    override fun start() {
        super.start()
        ring.start()
        state.onEach { ring.show(it) }.launchIn(scope)
        sensors.init()
        startAudioInput()
    }

    override suspend fun getDeviceInfo(): DeviceInfoResponse {
        val settings = settingsStore.get()
        return DeviceInfoResponse.newBuilder()
            .setName(name)
            .setFriendlyName(name)
            .setMacAddress(settings.macAddress)
            .setManufacturer(Build.MANUFACTURER ?: "Android")
            .setModel(Build.MODEL ?: "CM12.1")
            .setEsphomeVersion("Ava")
            .setLegacyVoiceAssistantVersion(1)
            .setVoiceAssistantFeatureFlags(voiceAssistantFeatureFlags)
            .build()
    }

    override suspend fun handleMessage(message: MessageLite) {
        when (message) {
            is SubscribeVoiceAssistantRequest -> Unit
            is VoiceAssistantResponse -> handleVoiceAssistantResponse(message)
            is VoiceAssistantEventResponse -> stateMachine.handleVoiceEvent(message)
            is VoiceAssistantConfigurationRequest -> sendVoiceAssistantConfiguration()
            is VoiceAssistantSetConfiguration -> audioInput.setActiveWakeWords(message.activeWakeWordsList)
            is VoiceAssistantAnnounceRequest -> playAnnouncement(message)
            else -> super.handleMessage(message)
        }
    }

    fun toggleMicMute() = setMicMute(!audioInput.muted.value)

    fun setMicMute(muted: Boolean) {
        audioInput.setMuted(muted)
    }

    fun manualWake() = triggerManualWake()

    fun toggleManualAssist() {
        if (isAssistRunning(state.value) || pendingStartGeneration != 0 || listeningCueReady) {
            stopVoiceSession()
        } else {
            triggerManualWake()
        }
    }

    fun triggerManualWake(wakeWordPhrase: String? = null, wakeWordIndex: Int = 0, conversationId: String = "") {
        if (audioInput.muted.value) return
        if (conversationId.isBlank()) lastConversationId = ""
        continueConversationRequested = false
        val generation = startGeneration.incrementAndGet()
        pendingStartGeneration = generation
        pendingStartWakeWordPhrase = wakeWordPhrase
        pendingStartWakeWordIndex = wakeWordIndex
        scope.launch { sendMessage(buildStartRequest(wakeWordPhrase, conversationId)) }
    }

    private fun handleVoiceAssistantResponse(message: VoiceAssistantResponse) {
        if (message.error) {
            Log.e(TAG, "Voice assistant start failed")
            stopVoiceSession()
            return
        }
        val generation = pendingStartGeneration
        if (!shouldAcceptStartResponse(generation, startGeneration.get())) {
            Log.d(TAG, "Ignoring stale voice assistant start response")
            return
        }
        listeningCueReady = true
        listeningCueWakeWordPhrase = pendingStartWakeWordPhrase
        listeningCueWakeWordIndex = pendingStartWakeWordIndex
        pendingStartGeneration = 0
        pendingStartWakeWordPhrase = null
        pendingStartWakeWordIndex = 0
    }

    fun stopVoiceSession() {
        startGeneration.incrementAndGet()
        continueConversationRequested = false
        lastConversationId = ""
        pendingStartGeneration = 0
        pendingStartWakeWordPhrase = null
        pendingStartWakeWordIndex = 0
        clearListeningCue()
        player.ttsPlayer.stop()
        finishVoiceSession()
        scope.launch { sendMessage(VoiceAssistantAudio.newBuilder().setEnd(true).build()) }
    }

    private suspend fun finishVoiceResponse() {
        val conversationId = lastConversationId
        val shouldContinue = shouldContinueConversation(
            enabled = playerSettingsStore.enableContinuousConversation.get(),
            requested = continueConversationRequested,
            conversationId = conversationId,
            muted = audioInput.muted.value
        )
        finishVoiceSession()
        continueConversationRequested = false
        if (shouldContinue) triggerManualWake(conversationId = conversationId)
    }

    private fun finishVoiceSession() {
        pendingStartGeneration = 0
        pendingStartWakeWordPhrase = null
        pendingStartWakeWordIndex = 0
        clearListeningCue()
        audioInput.isStreaming = false
        _state.value = Connected
    }

    private fun handleIntentEnd(data: VoiceAssistantIntentEndData) {
        lastConversationId = data.conversationId
        continueConversationRequested = data.continueConversation
    }

    private fun playListeningCue() {
        if (!listeningCueReady) return
        val wakeWordPhrase = listeningCueWakeWordPhrase
        val wakeWordIndex = listeningCueWakeWordIndex
        clearListeningCue()
        scope.launch {
            if (shouldPlayWakeSoundFor(wakeWordPhrase)) {
                player.playWakeSound(wakeWordIndex)
            } else {
                // Button/manual starts already show the Biscuit LED, so keep them silent.
                // player.playStartListeningSound()
            }
        }
    }

    private fun clearListeningCue() {
        listeningCueReady = false
        listeningCueWakeWordPhrase = null
        listeningCueWakeWordIndex = 0
    }

    private fun startAudioInput() {
        audioInput.start()
            .onEach { result ->
                when (result) {
                    is VoiceSatelliteAudioInput.AudioResult.Audio -> {
                        sendMessage(VoiceAssistantAudio.newBuilder().setData(result.audio).build())
                    }
                    is VoiceSatelliteAudioInput.AudioResult.WakeDetected -> {
                        val wakeWordIndex = audioInput.activeWakeWords.value.indexOf(result.wakeWordId).coerceAtLeast(0)
                        triggerManualWake(result.wakeWord, wakeWordIndex)
                    }
                    is VoiceSatelliteAudioInput.AudioResult.StopDetected -> player.playStopSound { stopVoiceSession() }
                }
            }
            .catch { e -> Log.e(TAG, "Audio input stopped", e) }
            .launchIn(scope)
    }

    private suspend fun sendVoiceAssistantConfiguration() {
        val response = VoiceAssistantConfigurationResponse.newBuilder()
            .addAllActiveWakeWords(audioInput.activeWakeWords.value)
            .setMaxActiveWakeWords(audioInput.availableWakeWords.size)
            .addAllAvailableWakeWords(audioInput.availableWakeWords.map { wakeWord ->
                VoiceAssistantWakeWord.newBuilder()
                    .setId(wakeWord.id)
                    .setWakeWord(wakeWord.wakeWord.wake_word)
                    .addAllTrainedLanguages(wakeWord.wakeWord.trained_languages.toList())
                    .build()
            })
            .build()
        sendMessage(response)
    }

    private suspend fun playAnnouncement(message: VoiceAssistantAnnounceRequest) {
        player.ttsPlayer.playAnnouncement(
            message.mediaId,
            message.preannounceMediaId.takeIf { it.isNotBlank() }
        ) {
            scope.launch {
                sendMessage(VoiceAssistantAnnounceFinished.newBuilder().setSuccess(true).build())
                if (message.startConversation) triggerManualWake()
            }
        }
    }

    override suspend fun onDisconnected() {
        super.onDisconnected()
        if (_state.value != Stopped) _state.value = Disconnected
    }

    override fun close() {
        sensors.stop()
        ring.close()
        audioInput.isStreaming = false
        player.close()
        super.close()
    }

    companion object {
        private const val TAG = "VoiceSatellite"

        internal val voiceAssistantFeatureFlags: Int =
            VoiceAssistantFeature.VOICE_ASSISTANT.flag or
                VoiceAssistantFeature.SPEAKER.flag or
                VoiceAssistantFeature.API_AUDIO.flag or
                VoiceAssistantFeature.ANNOUNCE.flag or
                VoiceAssistantFeature.START_CONVERSATION.flag

        internal fun buildStartRequest(wakeWordPhrase: String? = null, conversationId: String = ""): VoiceAssistantRequest {
            val request = VoiceAssistantRequest.newBuilder()
                .setStart(true)
                .setFlags(0)
                .setWakeWordPhrase(wakeWordPhrase.orEmpty())
            if (conversationId.isNotBlank()) request.setConversationId(conversationId)
            return request.build()
        }

        internal fun isAssistRunning(state: EspHomeState) = state == Listening || state == Processing || state == Responding
        internal fun shouldPlayWakeSoundFor(wakeWordPhrase: String?) = wakeWordPhrase != null
        internal fun shouldContinueConversation(enabled: Boolean, requested: Boolean, conversationId: String, muted: Boolean) =
            enabled && requested && conversationId.isNotBlank() && !muted
        internal fun shouldAcceptStartResponse(pendingGeneration: Int, currentGeneration: Int) =
            pendingGeneration != 0 && pendingGeneration == currentGeneration
    }
}
