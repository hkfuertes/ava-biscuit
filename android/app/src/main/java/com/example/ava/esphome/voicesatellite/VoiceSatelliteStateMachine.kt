package com.example.ava.esphome.voicesatellite

import android.util.Log
import com.example.ava.esphome.Connected
import com.example.ava.esphome.EspHomeState
import com.example.esphomeproto.api.VoiceAssistantEvent
import com.example.esphomeproto.api.VoiceAssistantEventResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class VoiceAssistantIntentEndData(val conversationId: String, val continueConversation: Boolean)

class VoiceSatelliteStateMachine(
    private val scope: CoroutineScope,
    private val audioInput: VoiceSatelliteAudioInput,
    private val player: VoiceSatellitePlayer,
    private val state: MutableStateFlow<EspHomeState>,
    private val onStopSatellite: suspend (String) -> Unit,
    private val onTtsFinished: suspend () -> Unit,
    private val onIntentEnd: (VoiceAssistantIntentEndData) -> Unit = {},
    private val onStatus: (String) -> Unit = {},
    private val onListeningStarted: (() -> Unit)? = null,
) {
    private var currentTtsText = ""

    fun handleVoiceEvent(voiceEvent: VoiceAssistantEventResponse) {
        Log.d(TAG, "Voice event: ${voiceEvent.eventType}, state: ${state.value}")
        when (voiceEvent.eventType) {
            VoiceAssistantEvent.VOICE_ASSISTANT_ERROR -> {
                val code = voiceEvent.dataList.firstOrNull { it.name == "code" }?.value ?: "unknown"
                val message = voiceEvent.dataList.firstOrNull { it.name == "message" }?.value ?: ""
                val status = statusForPipelineError(code)
                Log.e(TAG, "PIPELINE_ERROR: code=$code, message=$message, state=${state.value}")
                onStatus(status)
                audioInput.isStreaming = false
                scope.launch { onStopSatellite(status) }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_RUN_START -> {
                Log.d(TAG, "RUN_START received")
                onStatus("running")
                currentTtsText = ""
                player.ttsPlayer.runStart { scope.launch { onTtsFinished() } }
                audioInput.isStreaming = true
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_STT_START -> {
                Log.d(TAG, "STT_START received, listening")
                onStatus("listening")
                if (state.value == Connected) {
                    state.value = Listening
                    onListeningStarted?.invoke()
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_STT_VAD_START -> {
                Log.d(TAG, "STT_VAD_START received, user started speaking")
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_STT_VAD_END -> {
                Log.d(TAG, "STT_VAD_END received, switching to Processing")
                onStatus("processing")
                audioInput.isStreaming = false
                state.value = Processing
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_STT_END -> {
                val sttText = voiceEvent.dataList.firstOrNull { it.name == "text" }?.value
                Log.d(TAG, "STT_END received, hasText=${!sttText.isNullOrBlank()}")
                audioInput.isStreaming = false

                if (isPipelineTextError(sttText)) {
                    Log.w(TAG, "STT returned error, stopping session")
                    scope.launch { onStopSatellite("pipeline-error") }
                    return
                }

                if (state.value == Listening) state.value = Processing
                onStatus("processing")
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_INTENT_END -> {
                val data = intentEndDataFrom(voiceEvent)
                Log.d(TAG, "INTENT_END received, continueConversation=${data.continueConversation}")
                onIntentEnd(data)
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_TTS_START -> {
                Log.d(TAG, "TTS_START received, stopping audio input")
                onStatus("tts")
                audioInput.isStreaming = false

                val ttsText = voiceEvent.dataList.firstOrNull { it.name == "text" }?.value
                if (isPipelineTextError(ttsText)) {
                    Log.w(TAG, "TTS is about error, stopping session")
                    scope.launch { onStopSatellite("pipeline-error") }
                    return
                }

                state.value = Responding
                currentTtsText = ttsText.orEmpty()
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_TTS_END -> {
                val ttsUrl = voiceEvent.dataList.firstOrNull { it.name == "url" }?.value
                Log.d(TAG, "TTS_END received, ttsUrl=$ttsUrl, ttsPlayed=${player.ttsPlayer.ttsPlayed}, state=${state.value}")
                if (state.value == Responding && !player.ttsPlayer.ttsPlayed) {
                    player.ttsPlayer.markAsPlayed()
                    Log.d(TAG, "TTS_END: playing url=$ttsUrl, text='${currentTtsText.take(20)}...'")
                    if (ttsUrl.isNullOrBlank()) {
                        player.ttsPlayer.triggerCompletion()
                    } else {
                        player.ttsPlayer.playTts(ttsUrl)
                    }
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_RUN_END -> {
                val wasTtsPlayed = player.ttsPlayer.ttsPlayed
                Log.d(TAG, "RUN_END received, current state: ${state.value}, ttsPlayed: $wasTtsPlayed")
                audioInput.isStreaming = false

                when (state.value) {
                    is Listening, is Processing -> scope.launch { onStopSatellite("idle") }
                    is Responding -> if (!wasTtsPlayed) scope.launch { onStopSatellite("idle") }
                    else -> Unit
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_TTS_STREAM_END -> {
                Log.d(TAG, "TTS_STREAM_END received, finishing voice session")
                if (shouldFinishOnTtsStreamEnd(state.value)) scope.launch { onTtsFinished() }
            }

            else -> Unit
        }
    }

    private fun isPipelineTextError(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lowerText = text.lowercase()
        return lowerText.contains("list index out of range") ||
            lowerText.contains("索引超出范围") ||
            lowerText.contains("索引错误") ||
            (lowerText.contains("index") && lowerText.contains("range") && lowerText.contains("error"))
    }

    companion object {
        private const val TAG = "VoiceSatelliteStateMachine"

        internal fun intentEndDataFrom(voiceEvent: VoiceAssistantEventResponse): VoiceAssistantIntentEndData {
            var conversationId = ""
            var continueConversation = false
            voiceEvent.dataList.forEach { data ->
                when (data.name) {
                    "conversation_id" -> conversationId = data.value
                    "continue_conversation" -> continueConversation = data.value == "1"
                }
            }
            return VoiceAssistantIntentEndData(conversationId, continueConversation)
        }

        internal fun shouldFinishOnTtsStreamEnd(state: EspHomeState) = state == Responding
        internal fun statusForPipelineError(code: String) =
            if (code == "stt-no-text-recognized") "stt-no-text" else "pipeline-error"
    }
}
