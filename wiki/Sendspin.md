# Sendspin

Sendspin is Ava's integration with [Music Assistant](https://music-assistant.io/) for multi-room audio synchronization. It enables multiple Ava devices to play music in perfect sync across rooms, turning your home into a unified audio experience.

> **A note on positioning:** Sendspin's sync quality rivals dedicated multi-room systems, achieving what we call 5.4-level synchronization. But music playback is not Ava's main feature. Ava is first and foremost a voice satellite and smart home panel. Sendspin is a bonus that comes free with every Ava install. If you only need a media player, there are simpler apps. But if you already have Ava running as your voice satellite and smart home display, Sendspin turns it into a high-quality multi-room speaker at zero extra cost.

---

## Overview

Imagine walking from your kitchen to your living room, and the music follows you seamlessly, perfectly in time, with no echo, no delay, no gap. That is what Sendspin makes possible.

Sendspin allows Ava to act as a Music Assistant player, receiving audio streams and synchronizing playback across multiple devices on the same local network. Whether you have two devices or ten, in adjacent rooms or across the house, Sendspin keeps them locked together.

You can absolutely use Ava as a standalone media player. Put an old Android phone or tablet in each room, install Ava, enable Sendspin, and you have a whole-home smart speaker system. No dedicated hardware, no extra cost, just your existing devices and your Home Assistant.

### Ava vs Other Android Speaker Solutions

| Dimension | Ava + Sendspin | Other Android music apps | Dedicated multi-room (Sonos etc.) |
|-----------|----------------|--------------------------|-----------------------------------|
| Multi-room sync | Sub-20ms precision, adaptive speed correction, AirPlay-level tightness | Attempted but with noticeable drift and echo, cannot match AirPlay sync quality | Yes, but requires proprietary hardware |
| Sync technology | Clock sync + jitter buffer + adaptive playback speed | Basic buffering, no clock sync, no adaptive correction | Proprietary, closed |
| Audio codec choice | Opus, PCM, FLAC, Automatic (user selectable) | Fixed by app, no user choice | Fixed, no user choice |
| Audio stuttering | Adaptive jitter buffer + speed correction handles network jitter smoothly | Common, especially on WiFi. Stuttering, gaps, and incomplete playback on weaker networks | Rare, but hardware-dependent |
| Playback completeness | Every sample accounted for, late packets handled gracefully | Often drops or skips audio chunks when buffer underruns, playback sounds incomplete | Good, but proprietary |
| Voice control | Built-in, wake word + HA voice assistant | None or requires separate app | Limited, cloud-dependent |
| Smart home panel | Full HA dashboard overlay with floating windows | None | None |
| Floating windows during music | Clock, weather, notifications, subtitles overlay on top of music without blocking playback | Not possible, music apps take full screen | Not applicable |
| Music + voice simultaneously | Ducking lowers music for voice, then restores automatically | Not supported | Limited |
| Cost | Free, uses your existing Android devices | Free but sync quality is poor | Expensive proprietary hardware |
| Hardware required | Any Android 5+ device | Any Android device | Proprietary speakers only |
| Privacy | All local, no cloud | Varies, many phone home | Cloud-dependent |
| Open protocol | Sendspin protocol + Music Assistant | N/A | Proprietary |

### The Floating Window Difference

This is what makes Ava unique as a media player. Traditional Android music apps take over the full screen. When music is playing, you cannot see your clock, your weather, your notifications, or your smart home controls without switching apps.

Ava's floating windows sit on top of the music playback. While Sendspin streams music, you can simultaneously see:

- **Dream Clock** or **Simple Clock** floating on top, showing time and date
- **Weather Overlay** showing current conditions and forecast
- **Notification Scenes** popping up over the music when someone rings the doorbell or a timer fires
- **Floating Subtitle** showing what song is playing or what you said to the voice assistant
- **Quick Entity Panel** for one-tap control of lights, switches, or scenes without leaving the music screen

All of these overlay on top of the Music Assistant interface without interrupting playback. The music keeps playing in perfect sync across rooms, and your smart home information stays visible.

No other Android music app or multi-room solution can do this. Dedicated speakers like Sonos have no screen at all. Android music apps cannot overlay widgets on top of their own UI. HACS-based panel apps that try to do music + display usually switch between full-screen modes, interrupting one or the other.

Ava's overlay architecture, powered by Android's `SYSTEM_ALERT_WINDOW` permission, makes true simultaneous music + smart home display possible. The music plays underneath, the information floats on top, and neither blocks the other.

### Why Other Android Solutions Cannot Match AirPlay-Level Sync

AirPlay is the gold standard for consumer multi-room audio. Apple achieved this through tight integration of hardware, OS-level clock sync, and a proprietary protocol. Most Android music apps attempt multi-room playback but fall short in several measurable ways:

**Sync drift and echo.** Without clock synchronization between devices, each device's internal clock drifts independently. Over a 3-minute song, a 50ppm clock difference accumulates to 9ms of drift. Over an hour, that is 180ms, clearly audible as an echo between rooms. Other Android apps rely on simple buffer-based timing, which does not correct for clock drift. Sendspin's clock sync continuously tracks and corrects drift, keeping devices locked together over hours, not just seconds.

**No adaptive speed correction.** When a device falls behind, other Android apps have only two options: ignore it (audible desync) or skip ahead (audible glitch). Sendspin's adaptive playback speed correction subtly speeds up or slows down (within 1.5%) to smoothly re-converge without any audible artifact. This is the same approach AirPlay uses, and it is why Sendspin can match AirPlay-level sync quality on standard Android hardware.

**Audio codec limitations.** Most Android music apps use whatever codec the streaming service provides, with no user control. If the codec is too heavy for the device's CPU, the app stutters. If the codec is too high-bandwidth for the WiFi, packets arrive late and audio drops. Sendspin lets you choose between Opus (low bandwidth, low CPU), PCM (lowest CPU, highest bandwidth), and FLAC (lossless, medium bandwidth). You can match the codec to your device and network. Other apps offer no such choice.

**Stuttering and incomplete playback.** When network jitter hits, other Android apps often drop audio chunks entirely. You hear gaps, clicks, or missing beats. The playback sounds incomplete. Sendspin's jitter buffer reorders late packets and the adaptive speed controller smooths over gaps. If a packet is truly too late, the Opus decoder produces a gentle fade rather than a hard silence, so the gap is far less noticeable.

**No AirPlay-level target.** AirPlay achieves sync by using the AirPlay protocol's master clock and buffer preloading. Android has no equivalent OS-level mechanism. Sendspin implements its own equivalent: clock sync via filtering, jitter buffering, and adaptive speed correction. The result is sync quality that matches AirPlay on standard Android hardware, without requiring any proprietary OS features.

**Features:**
- Multi-room audio sync with sub-20ms precision
- Opus, PCM, and FLAC audio format support
- Adaptive playback speed correction for network jitter
- Clock synchronization between device and server
- Adjustable sync offset for fine-tuning
- Low memory mode for older devices
- Device or software volume control
- Auto-discovery via mDNS, no manual IP configuration
- Multi-server arbitration (automatically picks the right server)
- Ducking (auto-lower volume during voice commands or notifications)

---

## How It Works

### The Sync Challenge

Multi-room audio sync is harder than it looks. When music plays on two devices in different rooms, even a 50ms difference is clearly audible as an echo. To achieve perfect sync, every device needs to agree on exactly when to play each audio sample.

The challenges:
- Each device has its own internal clock, and these clocks drift apart over time
- Network latency varies moment to moment (jitter)
- Different devices have different processing speeds
- Audio buffers introduce variable delays
- WiFi is inherently unpredictable

### How Sendspin Solves It

Sendspin uses a multi-layer approach to achieve tight sync:

**1. Clock Synchronization**

Every device maintains a continuous clock sync with the Music Assistant server. Sendspin tracks the offset and drift between the device's clock and the server's clock using a filtering technique that combines multiple measurements over time. This is similar in spirit to how NTP (Network Time Protocol) keeps computers synchronized, but adapted for real-time audio.

The clock sync continuously evaluates network quality and adjusts its confidence level accordingly. On a good network, it converges quickly and stays stable. On a poor network, it becomes more conservative to avoid overcorrecting.

**2. Jitter Buffer**

Audio packets arrive at irregular intervals due to network jitter. Sendspin maintains a smart buffer that reorders late packets and drops packets that are too late to be useful. The buffer size adapts to network conditions: larger when jitter is high (more stability, more latency), smaller when jitter is low (less latency, tighter sync).

In low memory mode, the buffer is capped to avoid exhausting RAM on older devices.

**3. Adaptive Playback Speed**

This is the key innovation. Instead of just buffering and hoping for the best, Sendspin subtly adjusts the playback speed to stay in sync:

- If the device is falling behind, playback speeds up slightly (up to 1.5% faster)
- If the device is running ahead, playback slows down slightly (up to 1.5% slower)
- During startup, a brief boost helps the device catch up to the server position quickly
- For large desyncs (over 80ms), a hard correction kicks in to snap back into sync

The speed adjustments are tiny enough that the human ear cannot detect the pitch change, but large enough to correct drift within seconds. This is the same technique used by professional multi-room audio systems like Sonos.

**4. Position Tracking**

Sendspin continuously monitors the server-reported playback position (where in the song the server thinks it is) and compares it with the local playback position. The difference is smoothed using an exponential moving average to avoid overreacting to single measurements. When the difference exceeds a 20ms tolerance, corrections are applied.

**5. Urgent Audio Priority**

Audio playback runs on a dedicated thread with the highest audio priority level the OS allows. This ensures that even under CPU load, audio output is not interrupted or delayed. The playback thread is isolated from the rest of the app to prevent jitter from UI updates or background tasks.

---

## Audio Formats

Sendspin supports three audio codecs, each with different trade-offs:

| Format | Quality | Bandwidth | CPU Usage | Best For |
|--------|---------|-----------|----------|----------|
| Opus 48kHz 16bit | Good | Low (~96-128 kbps) | Low (self-compiled native decoder) | WiFi with many devices, low-end hardware |
| PCM 44.1/48kHz 16/24bit | Highest | High (~1.5-2.3 Mbps) | Very low | Wired Ethernet, high-end devices |
| FLAC 44.1/48kHz 16/24bit | High (lossless) | Medium (~800-1000 kbps) | Medium | Good WiFi, quality-conscious setups |

**Automatic mode** (recommended) lets the Music Assistant server choose the best format based on your network and device capabilities. The server negotiates with Ava during connection and picks the optimal codec.

### The Self-Compiled Opus Decoder

Opus is the workhorse codec for Sendspin. It delivers excellent audio quality at low bitrates, making it ideal for WiFi multi-room setups with many devices. But Ava does not use a pre-built Opus library from a third party. The Opus decoder is self-compiled from the official Opus source code, tailored specifically for Ava's needs.

**Why self-compiled?**

Most Android apps that use Opus rely on pre-built libraries distributed via Maven or AAR packages. These libraries are compiled for general-purpose use, include features that Ava does not need (encoder, packet loss concealment modes, DTX, FEC), and are often larger than necessary. Ava's self-compiled decoder strips away everything except what Sendspin actually uses:

- **Decoder only.** No encoder code. Ava only receives and decodes audio, it never encodes.
- **Only the sample rates Sendspin uses:** 8kHz, 12kHz, 16kHz, 24kHz, 48kHz. Other rates are excluded.
- **Mono and stereo only.** No surround sound or multichannel support.
- **Static linking.** The Opus code is compiled directly into the native library, no external dependencies.
- **Size optimized.** The final library is only 210KB (arm64) and 180KB (armv7). A typical pre-built Opus library is 500KB-1MB.

**Why this matters for you:**

- **Lower APK size.** The entire Opus decoder adds less than 400KB to the APK across both CPU architectures.
- **Lower memory usage.** The decoder allocates only what it needs: one decoder instance and one PCM output buffer. No unused code loaded into memory.
- **Faster decode.** Without unused code paths, the decoder runs leaner. On low-end devices, this means fewer CPU cycles per audio frame, leaving more headroom for the sync engine and UI.
- **No third-party trust.** You do not need to trust a pre-built binary from an unknown maintainer. The Opus codec is open source, audited, and used by WhatsApp, Discord, and Zoom. Ava compiles it directly from the official source.
- **Security.** A self-compiled library from audited source is more trustworthy than a binary blob downloaded from a package repository. There is no supply chain risk from a third-party Maven artifact.

**How it works:**

The native library handles the heavy lifting of Opus decoding in C, exposed to Kotlin via JNI. Each audio packet received from the Music Assistant server is passed to the native decoder, which produces PCM audio samples. These samples are then fed into the jitter buffer and playback pipeline.

The decoder supports instant reset (clearing internal state when seeking or switching tracks) and clean resource disposal (freeing all memory when playback stops). Error handling is defensive: if a corrupted packet arrives, the decoder returns silence for that frame rather than crashing, so a single bad packet never interrupts playback.

---

## Setup

### Prerequisites

1. [Music Assistant](https://music-assistant.io/) installed and running in Home Assistant
2. Ava devices on the same LAN as Music Assistant
3. A stable WiFi network (5GHz recommended for multi-room sync)

### Enable Sendspin

1. Go to **Settings** -> **Extensions** -> **Playback** -> **Media Player**
2. Turn on **Music Assistant Media Controls**
3. Set the **Sendspin Device Name** (leave empty to use system device name)
4. Choose **Sendspin Audio Format** (Automatic recommended)
5. Ava will appear as a player in Music Assistant

That's it. Ava auto-discovers the Music Assistant server on your network via mDNS. No manual IP entry, no port configuration. Just turn it on and it connects.

> **Note:** Sendspin device names are customizable. Saving triggers a session restart so Music Assistant refreshes the display name faster. If the old name persists, refresh the player in Music Assistant or fully kill and restart Ava.

---

## Settings

Go to **Settings** -> **Extensions** -> **Playback** -> **Media Player**

| Setting | Description | Default |
|---------|-------------|--------|
| Music Assistant Media Controls | Show music controls during MA playback | Off |
| Sendspin Volume Control | Device Volume or Software Volume | Device Volume |
| Sendspin Optimization Mode | Recommended for low-memory devices | Off |
| Sendspin Sync Offset | Fine-tune playback timing in milliseconds | 0 ms |
| Sendspin Audio Format | Audio codec and quality | Automatic |
| Sendspin Device Name | Name shown on Music Assistant server | System device name |
| Sync device volume | Keep all related media volumes aligned | Off |

### Volume Control Mode

| Mode | Description |
|------|-------------|
| Device Volume | Use the device hardware volume. Best for dedicated devices with physical buttons |
| Software Volume | Software-based volume control. Best when device volume is fixed or unavailable |

### Sync Device Volume

When enabled, volume changes on one Ava device propagate to all other Ava devices playing the same Music Assistant stream. This lets you control the volume of your entire home from any device.

### Ducking

When a voice command is triggered or a notification arrives during music playback, Sendspin automatically lowers the music volume (ducking) so you can hear the response. Once the voice session or notification ends, music volume smoothly returns to normal.

---

## Sync Offset

If audio is out of sync between rooms, adjust the sync offset:

1. Go to **Settings** -> **Extensions** -> **Playback** -> **Media Player**
2. Find **Sendspin Sync Offset**
3. Adjust in milliseconds (positive or negative)
4. Changes take effect immediately
5. Long-press the player to hide it

**Tip:** If you previously used -200 ms, change it to 0 ms and toggle the Optimization Mode switch a few times. The adaptive sync system should handle timing automatically in most cases.

---

## Low Memory Mode

For devices with limited RAM (1GB or less):

1. Enable **Sendspin Optimization Mode**
2. This caps the jitter buffer size to prevent memory exhaustion
3. Reduces background processing during playback
4. Recommended for old tablets and phones

Even with low memory mode enabled, sync quality remains good. The trade-off is slightly more susceptibility to network jitter, not a loss of sync accuracy.

---

## Multi-Server Support

If you have multiple Music Assistant servers on your network (e.g., for testing or migration), Sendspin automatically handles the situation:

1. When a second server connects, Sendspin completes the handshake with both
2. It compares connection reasons and your last-played server preference
3. It picks the winner and sends a polite goodbye to the loser
4. You never get audio from two servers at once

This happens automatically, no user intervention needed.

---

## Auto-Discovery

Sendspin uses mDNS (the same technology as AirPlay and Chromecast) to find the Music Assistant server on your local network. This means:

- No manual IP entry
- No port configuration
- Works automatically when Music Assistant is installed
- Reconnects automatically if the server restarts
- Discovers new servers when they appear on the network

---

## Home Assistant Integration

Sendspin integrates through Music Assistant. Once enabled, Ava appears as a player in Music Assistant and can be controlled from there.

### Music Assistant URL

Access Music Assistant from Home Assistant:
```
http://your-ha-ip:8123/music-assistant
```

---

## FAQ

### Ava doesn't appear in Music Assistant?

1. Ensure Music Assistant Media Controls is enabled
2. Check that Ava and Music Assistant are on the same LAN
3. Try restarting the Ava service
4. Check the Sendspin Device Name
5. Verify Music Assistant is running and healthy in HA

### Audio out of sync between rooms?

1. First, try setting Sync Offset to 0 and let the adaptive sync system work
2. If still out of sync, adjust Sendspin Sync Offset in small steps (10-50 ms)
3. Ensure all devices use the same audio format
4. Check that all devices are on the same WiFi band (mixing 2.4GHz and 5GHz can cause sync issues due to different latency)
5. If using old devices, ensure Low Memory Mode is enabled

### Audio stuttering on old devices?

1. Enable Sendspin Optimization Mode
2. Switch to Opus format (lowest CPU and bandwidth)
3. Check network signal strength
4. Close other resource-intensive apps
5. If on 2.4GHz WiFi with many devices, try 5GHz or wired connection

### Volume not syncing?

1. Enable **Sync device volume** in settings
2. Check if Volume Control is set to Device Volume
3. Try switching to Software Volume
4. Ensure all devices have Sendspin enabled

### Music stops when I trigger a voice command?

This is normal behavior called ducking. The music volume lowers so you can hear the voice response. It returns to normal after the voice session ends. If you prefer music to pause completely, this can be configured in the interaction settings.

### Can I use Sendspin without Music Assistant?

No. Sendspin is specifically designed as a Music Assistant player. It uses the Sendspin protocol to communicate with Music Assistant servers. For regular media playback (TTS, HA media player), use the standard Media Player settings instead.

---

## Network Recommendations

For the best multi-room sync experience:

- **Use 5GHz WiFi** when possible. It has lower latency and less interference than 2.4GHz
- **Keep all devices on the same WiFi band**. Mixing bands introduces latency differences
- **Ensure good signal strength** on all devices. Weak signal causes jitter and packet loss
- **Wired Ethernet** is ideal for fixed installations (wall tablets)
- **Avoid heavy network traffic** during critical listening. Large downloads on the same network can cause jitter
- **A dedicated IoT VLAN** can help isolate music traffic from other network activity

---

*Back to [Home](Home)*
