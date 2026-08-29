package com.example.ava.esphome.voicesatellite

import com.example.ava.esphome.Connected
import com.example.ava.esphome.Disconnected
import com.example.ava.players.AudioPlayer
import com.example.ava.receivers.AvaControlReceiver
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.settings.PlayerSettings
import com.example.esphomeproto.api.VoiceAssistantEvent
import com.example.esphomeproto.api.VoiceAssistantEventResponse
import com.example.esphomeproto.api.VoiceAssistantFeature
import com.example.esphomeproto.api.voiceAssistantEventData
import android.view.KeyEvent
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSatelliteProtocolTest {
    @Test
    fun featureFlagsIncludeAnnounceSoHomeAssistantFetchesWakeWordConfig() {
        val flags = VoiceSatellite.voiceAssistantFeatureFlags

        assertTrue(flags and VoiceAssistantFeature.TIMERS.flag != 0)
        assertTrue(flags and VoiceAssistantFeature.ANNOUNCE.flag != 0)
        assertTrue(flags and VoiceAssistantFeature.START_CONVERSATION.flag != 0)
    }

    @Test
    fun localWakeWordStartRequestStartsAtStt() {
        val request = VoiceSatellite.buildStartRequest("okay nabu")

        assertTrue(request.start)
        assertEquals(0, request.flags)
        assertEquals("okay nabu", request.wakeWordPhrase)
    }

    @Test
    fun manualStartRequestStartsAtStt() {
        val request = VoiceSatellite.buildStartRequest()

        assertTrue(request.start)
        assertEquals(0, request.flags)
        assertEquals("", request.wakeWordPhrase)
    }

    @Test
    fun continuousStartRequestCarriesConversationId() {
        val request = VoiceSatellite.buildStartRequest(conversationId = "conversation-1")

        assertTrue(request.start)
        assertEquals("conversation-1", request.conversationId)
    }

    @Test
    fun intentEndDataCarriesConversationLoopDecision() {
        val event = VoiceAssistantEventResponse.newBuilder()
            .setEventType(VoiceAssistantEvent.VOICE_ASSISTANT_INTENT_END)
            .addData(voiceAssistantEventData {
                name = "conversation_id"
                value = "conversation-1"
            })
            .addData(voiceAssistantEventData {
                name = "continue_conversation"
                value = "1"
            })
            .build()

        val data = VoiceSatelliteStateMachine.intentEndDataFrom(event)

        assertEquals("conversation-1", data.conversationId)
        assertTrue(data.continueConversation)
    }

    @Test
    fun continuousConversationNeedsSettingHaRequestConversationAndUnmutedMic() {
        assertTrue(VoiceSatellite.shouldContinueConversation(true, true, "conversation-1", muted = false))
        assertEquals(false, VoiceSatellite.shouldContinueConversation(false, true, "conversation-1", muted = false))
        assertEquals(false, VoiceSatellite.shouldContinueConversation(true, false, "conversation-1", muted = false))
        assertEquals(false, VoiceSatellite.shouldContinueConversation(true, true, "", muted = false))
        assertEquals(false, VoiceSatellite.shouldContinueConversation(true, true, "conversation-1", muted = true))
    }

    @Test
    fun watchdogOnlyCoversActiveAssistStates() {
        assertEquals(45_000L, VoiceSatellite.watchdogTimeoutMs(Listening))
        assertEquals(60_000L, VoiceSatellite.watchdogTimeoutMs(Processing))
        assertEquals(120_000L, VoiceSatellite.watchdogTimeoutMs(Responding))
        assertEquals(null, VoiceSatellite.watchdogTimeoutMs(Connected))
    }

    @Test
    fun pipelineErrorStatusKeepsNoTextReadable() {
        assertEquals("stt-no-text", VoiceSatelliteStateMachine.statusForPipelineError("stt-no-text-recognized"))
        assertEquals("pipeline-error", VoiceSatelliteStateMachine.statusForPipelineError("wake-engine-missing"))
    }

    @Test
    fun buttonPressStopsOnlyWhileAssistIsRunning() {
        assertTrue(VoiceSatellite.isAssistRunning(Listening))
        assertTrue(VoiceSatellite.isAssistRunning(Processing))
        assertTrue(VoiceSatellite.isAssistRunning(Responding))
        assertEquals(false, VoiceSatellite.isAssistRunning(Connected))
        assertEquals(false, VoiceSatellite.isAssistRunning(Disconnected))
    }

    @Test
    fun startResponseIsAcceptedOnlyForCurrentPendingStart() {
        assertTrue(VoiceSatellite.shouldAcceptStartResponse(2, 2))
        assertEquals(false, VoiceSatellite.shouldAcceptStartResponse(0, 2))
        assertEquals(false, VoiceSatellite.shouldAcceptStartResponse(2, 3))
    }

    @Test
    fun buttonStartsStaySilentWhileWakeWordsCanPlayWakeSound() {
        assertEquals(false, VoiceSatellite.shouldPlayWakeSoundFor(null))
        assertTrue(VoiceSatellite.shouldPlayWakeSoundFor("okay nabu"))
    }

    @Test
    fun physicalActionButtonMapsOnlyCmHelpKey() {
        assertTrue(VoiceSatelliteService.isActionButtonKeyCode(KeyEvent.KEYCODE_HELP))
        assertEquals(false, VoiceSatelliteService.isActionButtonKeyCode(KeyEvent.KEYCODE_VOLUME_UP))
        assertEquals(false, VoiceSatelliteService.isActionButtonKeyCode(KeyEvent.KEYCODE_VOLUME_DOWN))
        assertEquals(false, VoiceSatelliteService.isActionButtonKeyCode(KeyEvent.KEYCODE_MENU))
    }

    @Test
    fun amazonBiscuitButtonBroadcastMapsOnlyHelpScanCode() {
        assertEquals(true, AvaControlReceiver.biscuitButtonPressed(AvaControlReceiver.ACTION_BISCUIT_BUTTON_PRESSED))
        assertEquals(false, AvaControlReceiver.biscuitButtonPressed(AvaControlReceiver.ACTION_BISCUIT_BUTTON_RELEASED))
        assertTrue(AvaControlReceiver.isBiscuitHelpButton("KEYCODE_HELP", KeyEvent.KEYCODE_HELP, 138))
        assertEquals(false, AvaControlReceiver.isBiscuitHelpButton("KEYCODE_MENU", KeyEvent.KEYCODE_MENU, 139))
        assertEquals(false, AvaControlReceiver.isBiscuitHelpButton("KEYCODE_HELP", KeyEvent.KEYCODE_HELP, 139))
    }

    @Test
    fun assistRingUsesBiscuitAnimationsForActiveStates() {
        assertEquals("solid_cyan", BiscuitRingController.animationFor(Listening))
        assertEquals("alexa_thinking", BiscuitRingController.animationFor(Processing))
        assertEquals("solid_blue", BiscuitRingController.animationFor(Responding))
        assertEquals(null, BiscuitRingController.animationFor(Connected))
    }

    @Test
    fun timerCountdownUsesRomServiceMilliseconds() {
        assertTrue(BiscuitRingController.isValidCountdown(180_000L, 240_000L))
        assertEquals(false, BiscuitRingController.isValidCountdown(240_001L, 240_000L))
        assertEquals(false, BiscuitRingController.isValidCountdown(1L, 0L))
        assertEquals(180_000L, VoiceSatellite.secondsToMillis(180))
        assertEquals(0L, VoiceSatellite.secondsToMillis(-1))
        assertEquals(180, VoiceSatellite.remainingSeconds(179_001L))
    }

    @Test
    fun timerStatusIsShortAndRestoresAssistStateNames() {
        assertEquals("timer-0s", VoiceSatellite.timerStatus(-1L))
        assertEquals("timer-180s", VoiceSatellite.timerStatus(180_000L))
        assertEquals("listening", VoiceSatellite.statusForState(Listening))
        assertEquals("processing", VoiceSatellite.statusForState(Processing))
        assertEquals("tts", VoiceSatellite.statusForState(Responding))
        assertEquals("idle", VoiceSatellite.statusForState(Connected))
    }

    @Test
    fun ttsStreamEndFinishesOnlyWhileResponding() {
        assertTrue(VoiceSatelliteStateMachine.shouldFinishOnTtsStreamEnd(Responding))
        assertEquals(false, VoiceSatelliteStateMachine.shouldFinishOnTtsStreamEnd(Listening))
        assertEquals(false, VoiceSatelliteStateMachine.shouldFinishOnTtsStreamEnd(Connected))
    }

    @Test
    fun audioPlayerCompletesOnlyAfterPlaybackActuallyStarted() {
        assertTrue(AudioPlayer.isCompletePlaybackState(Player.STATE_IDLE))
        assertTrue(AudioPlayer.shouldCompleteOnNotPlaying(Player.STATE_IDLE, playbackStarted = true))
        assertEquals(false, AudioPlayer.shouldCompleteOnNotPlaying(Player.STATE_IDLE, playbackStarted = false))
        assertEquals(false, AudioPlayer.shouldCompleteOnNotPlaying(Player.STATE_BUFFERING, playbackStarted = true))
        assertEquals(false, AudioPlayer.shouldCompleteOnNotPlaying(Player.STATE_READY, playbackStarted = true))
    }

    @Test
    fun bundledSoundDefaultsUseWavs() {
        val settings = PlayerSettings()

        assertEquals("asset:///sounds/wake_word_triggered.wav", settings.wakeSound)
        assertEquals("asset:///sounds/timer_finished.wav", settings.timerFinishedSound)
        assertEquals("asset:///stopWords/stop_sound.wav", settings.stopSound)
        assertEquals("asset:///sounds/continuous_prompt.wav", settings.continuousPromptSound)
    }
}
