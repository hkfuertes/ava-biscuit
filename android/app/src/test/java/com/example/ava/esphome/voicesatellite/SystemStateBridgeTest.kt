package com.example.ava.esphome.voicesatellite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SystemStateBridgeTest {

    private lateinit var adapter: FakeAudioAdapter
    private lateinit var listener: FakeListener
    private lateinit var bridge: SystemStateBridge

    @Before
    fun setUp() {
        adapter = FakeAudioAdapter(maxVolume = 15)
        listener = FakeListener()
        bridge = SystemStateBridge(adapter, listener)
    }

    // --- Physical → HA direction ---

    @Test
    fun pollPublishesMuteChangeFromSystem() {
        adapter.rawMicMuted = true
        bridge.pollAndPublish()
        assertEquals(listOf(true), listener.muteEvents)
    }

    @Test
    fun pollPublishesVolumeChangeFromSystem() {
        adapter.rawMusicVolume = 8  // 8/15 ≈ 0.533
        bridge.pollAndPublish()
        assertEquals(1, listener.volumeEvents.size)
        assertEquals(8f / 15f, listener.volumeEvents[0], 0.001f)
    }

    @Test
    fun pollSkipsPublishWhenNoChange() {
        adapter.rawMicMuted = false
        adapter.rawMusicVolume = 0
        bridge.pollAndPublish()  // initial: mute=false, vol=0
        listener.muteEvents.clear()
        listener.volumeEvents.clear()

        bridge.pollAndPublish()  // same values again
        assertTrue(listener.muteEvents.isEmpty())
        assertTrue(listener.volumeEvents.isEmpty())
    }

    @Test
    fun pollDetectsPhysicalMuteToggle() {
        adapter.rawMicMuted = false
        bridge.pollAndPublish()
        assertEquals(listOf(false), listener.muteEvents)

        adapter.rawMicMuted = true  // user pressed physical mute button
        bridge.pollAndPublish()
        assertEquals(listOf(false, true), listener.muteEvents)
    }

    @Test
    fun pollDetectsPhysicalVolumeChange() {
        adapter.rawMusicVolume = 5
        bridge.pollAndPublish()
        val first = listener.volumeEvents[0]

        adapter.rawMusicVolume = 10  // user pressed physical volume button
        bridge.pollAndPublish()
        assertEquals(2, listener.volumeEvents.size)
        assertEquals(10f / 15f, listener.volumeEvents[1], 0.001f)
        assertTrue(first != listener.volumeEvents[1])
    }

    // --- HA → Android direction ---

    @Test
    fun haMuteCommandSetsSystemAndPublishes() {
        adapter.rawMicMuted = false
        bridge.pollAndPublish()  // establish baseline
        listener.muteEvents.clear()

        bridge.onHaMuteCommand(true)
        assertTrue(adapter.rawMicMuted)  // system state changed
        assertEquals(listOf(true), listener.muteEvents)  // observed state published
    }

    @Test
    fun haVolumeCommandSetsSystemAndPublishes() {
        adapter.rawMusicVolume = 0
        bridge.pollAndPublish()
        listener.volumeEvents.clear()

        bridge.onHaVolumeCommand(0.5f)
        // 0.5 * 15 = 7 (Int truncation)
        assertEquals(7, adapter.rawMusicVolume)
        assertEquals(1, listener.volumeEvents.size)
        assertEquals(7f / 15f, listener.volumeEvents[0], 0.001f)
    }

    @Test
    fun haVolumeCommandClampsOutOfRange() {
        bridge.onHaVolumeCommand(1.5f)  // above 1.0
        assertEquals(adapter.rawMaxVolume, adapter.rawMusicVolume)  // clamped to max

        adapter.rawMusicVolume = 10
        bridge.onHaVolumeCommand(-0.5f)  // below 0.0
        assertEquals(0, adapter.rawMusicVolume)  // clamped to 0
    }

    // --- Loop prevention ---

    @Test
    fun pollDuringCommandDoesNotEcho() {
        // Simulate: HA sends mute=true, adapter immediately reflects it.
        // The command handler sets + reads back. A concurrent poll must not double-publish.
        adapter.rawMicMuted = false
        bridge.pollAndPublish()
        listener.muteEvents.clear()

        // Use an adapter that triggers a poll during setMicMuted
        val echoingAdapter = EchoingFakeAudioAdapter(maxVolume = 15)
        val echoingListener = FakeListener()
        val echoBridge = SystemStateBridge(echoingAdapter, echoingListener)
        echoingAdapter.bridge = echoBridge

        echoBridge.onHaMuteCommand(true)
        // Should publish exactly once (from the command handler's read-back),
        // not twice (which would happen if the poll during setMicMuted also published).
        assertEquals(listOf(true), echoingListener.muteEvents)
    }

    @Test
    fun haCommandSkipsPublishWhenObservedMatchesLastPublished() {
        adapter.rawMicMuted = true
        bridge.pollAndPublish()  // publishes mute=true
        listener.muteEvents.clear()

        // HA sends mute=true again; system already true; no delta → no publish
        bridge.onHaMuteCommand(true)
        assertTrue(listener.muteEvents.isEmpty())
    }

    // --- Edge cases ---

    @Test
    fun zeroMaxVolumeReturnsZeroNormalized() {
        val zeroAdapter = FakeAudioAdapter(maxVolume = 0)
        val zeroListener = FakeListener()
        val zeroBridge = SystemStateBridge(zeroAdapter, zeroListener)

        zeroBridge.pollAndPublish()
        assertEquals(0f, zeroListener.volumeEvents[0], 0.001f)
    }

    @Test
    fun volumeIsCoercedToAdapterRange() {
        adapter.rawMusicVolume = -5  // out of range below
        bridge.pollAndPublish()
        // coerceIn(0, 15) → 0
        assertEquals(0f, listener.volumeEvents[0], 0.001f)

        adapter.rawMusicVolume = 100  // out of range above
        bridge.pollAndPublish()
        // coerceIn(0, 15) → 15
        assertEquals(1f, listener.volumeEvents[1], 0.001f)
    }

    // --- Fakes ---

    private class FakeAudioAdapter(maxVolume: Int) : SystemAudioAdapter {
        var rawMicMuted = false
        var rawMusicVolume = 0
        val rawMaxVolume = maxVolume

        override fun isMicMuted(): Boolean = rawMicMuted
        override fun setMicMuted(muted: Boolean) { rawMicMuted = muted }
        override fun getMusicVolume(): Int = rawMusicVolume
        override fun getMusicMaxVolume(): Int = rawMaxVolume
        override fun setMusicVolume(volume: Int) { rawMusicVolume = volume.coerceIn(0, rawMaxVolume) }
    }

    private class FakeListener : VoiceStateListener {
        val muteEvents = mutableListOf<Boolean>()
        val volumeEvents = mutableListOf<Float>()

        override fun onMuteStateChanged(muted: Boolean) { muteEvents.add(muted) }
        override fun onVolumeChanged(normalizedVolume: Float) { volumeEvents.add(normalizedVolume) }
    }

    /** Adapter whose setMicMuted triggers a bridge poll to test loop guard. */
    private class EchoingFakeAudioAdapter(maxVolume: Int) : SystemAudioAdapter {
        var rawMicMuted = false
        var rawMusicVolume = 0
        val rawMaxVolume = maxVolume
        lateinit var bridge: SystemStateBridge

        override fun isMicMuted(): Boolean = rawMicMuted
        override fun setMicMuted(muted: Boolean) {
            rawMicMuted = muted
            bridge.pollAndPublish()  // simulate ContentObserver firing during command
        }
        override fun getMusicVolume(): Int = rawMusicVolume
        override fun getMusicMaxVolume(): Int = rawMaxVolume
        override fun setMusicVolume(volume: Int) {
            rawMusicVolume = volume.coerceIn(0, rawMaxVolume)
            bridge.pollAndPublish()
        }
    }
}
