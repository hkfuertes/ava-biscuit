# Mod Catalog

All Ava mods in one place. Tap any name in the index below to jump to its detail.

Source: [github.com/knoop7/ava-mods](https://github.com/knoop7/ava-mods)

---

## Index

### Feature Mods

- [HA Edge TTS](#ha-edge-tts) — cloud TTS, 400+ voices, Wyoming protocol
- [HA STT Engine](#ha-stt-engine) — offline STT, zh/en/ja/ko/Cantonese, Wyoming protocol
- [BLE ADV Proxy](#ble-adv-proxy) — raw BLE advertising proxy for ha-ble-adv
- [DLNA Renderer](#dlna-renderer) — turn Ava into a LAN DLNA speaker
- [Screen Color Filter](#screen-color-filter) — global screen tint from Home Assistant
- [Flashlight](#flashlight) — control the camera flash from Home Assistant
- [GPS Location](#gps-location) — expose GPS to Home Assistant
- [Sticky Note](#sticky-note) — draggable on-screen note from Home Assistant
- [WiFi & ADB Keep-Alive](#wifi--adb-keep-alive) — keep WiFi and ADB alive across reboots
- [OpenClaw (Mini)](#openclaw-mini) — on-device AI assistant with tool use
- [Qualcomm Audio Concurrency Fix](#qualcomm-audio-concurrency-fix) — fix mic blocking on Qualcomm devices
- [Zigbee Gateway](#zigbee-gateway) — serial-to-TCP bridge for Zigbee coordinators

### Device Support Mods

- [Allwinner A64 Custom](#allwinner-a64-custom) — A64 tablets and smart screens
- [Echo Show Support](#echo-show-support) — Amazon Echo Show
- [Facebook Portal](#facebook-portal) — Portal family sensors and controls
- [IR Blaster (ESPhome)](#ir-blaster-esphome) — IR transmitter for Home Assistant
- [Phicomm R1](#phicomm-r1) — Phicomm R1 LED ring and voice
- [Tuya S8E](#tuya-s8e) — Tuya S8E smart screen
- [YX LED Controller](#yx-led-controller) — YX M5612 LED and motion

---

## Feature Mods

General-purpose mods that work on any compatible Android device.

---

### HA Edge TTS

**Version:** 1.0.8 | **Author:** Ava

Cloud-based TTS using Microsoft Edge TTS voices. Runs a Wyoming protocol TCP server inside Ava so Home Assistant voice pipelines can use 400+ voices in 40+ languages without a separate TTS server.

**What It Does:**
- Wyoming TTS server on TCP port 10301 (configurable)
- mDNS discovery (`_wyoming._tcp`) for Home Assistant auto-discovery
- 400+ Microsoft Edge Neural voices, 40+ languages
- Adjustable rate, volume, and pitch
- Streams synthesized audio directly to Home Assistant

**Use Case:**
You want natural-sounding TTS for your Home Assistant voice assistant but don't want to run a separate TTS server or pay for cloud TTS APIs. This mod turns any Ava device into a Wyoming TTS endpoint using Microsoft's free Edge TTS service.

**Entities Exposed to Home Assistant:**

All diagnostic entities are off by default. Enable **Show Diagnostic Entities** in mod settings to expose them.

| Entity | Type | Description |
|--------|------|-------------|
| TTS Server | binary_sensor | Server running status (diagnostic) |
| Restart TTS Server | button | Restart the Wyoming server (diagnostic) |
| Last TTS Text | text_sensor | Last synthesized text (opt-in) |

**Configuration:**

| Setting | Default | Description |
|---------|---------|-------------|
| Default Voice | en-US-AriaNeural | Microsoft Edge TTS voice (400+ options) |
| Speech Rate | +0% | Rate adjustment, e.g. +10%, -5% |
| Volume | +0% | Volume adjustment, e.g. -10% |
| Pitch | +0Hz | Pitch adjustment, e.g. +2Hz |
| TCP Port | 10301 | Wyoming server port |
| Listen Address | 0.0.0.0 | Bind address (0.0.0.0 = LAN, 127.0.0.1 = local only) |
| Auto Start | On | Start server when mod is enabled |
| mDNS Discovery | On | Broadcast `_wyoming._tcp` for HA auto-discovery |
| Show Last TTS Text | Off | Expose last synthesized text as HA sensor |
| Show Diagnostic Entities | Off | Expose server status and restart button |

**Requirements:** Android 5.0+ (API 21), Internet permission

**How to Use:**
1. Install the HA Edge TTS mod
2. Pick a voice in mod settings (e.g. `zh-CN-XiaoxiaoNeural` for Chinese, `en-US-AriaNeural` for English)
3. In Home Assistant, add the **Wyoming Protocol** integration
4. Connect to the Ava device IP on port `10301`
5. Select **HA Edge TTS** as the TTS engine in your voice assistant pipeline
6. Test by making Ava speak through HA

---

### HA STT Engine

**Version:** 1.2.1 | **Author:** Ava

Local offline STT for Home Assistant using the SenseVoice model. Supports Chinese, English, Japanese, Korean, and Cantonese. Runs a Wyoming protocol TCP server on port 10300 so Home Assistant voice pipelines get on-device speech recognition without cloud dependency.

**What It Does:**
- Wyoming STT server on TCP port 10300 (configurable)
- mDNS discovery (`_wyoming._tcp`) for Home Assistant auto-discovery
- On-demand SenseVoice model download (~230MB) to external app storage
- Offline recognition with emotion and audio-event metadata
- Languages: en (default), zh, ja, ko, yue — switch language before downloading; changing language invalidates the installed model until re-download

**Use Case:**
You want fully offline speech recognition for Home Assistant voice pipelines. No cloud, no API keys, no latency to remote servers. The Ava device itself becomes the STT engine. Ideal for privacy-sensitive setups or locations with unreliable internet.

**Entities Exposed to Home Assistant:**

Diagnostic entities are off by default. The emotion sensor has its own toggle.

| Entity | Type | Description |
|--------|------|-------------|
| STT Server | binary_sensor | Server running status (diagnostic) |
| Model Status | text_sensor | Model download/install status (diagnostic) |
| Download Progress | sensor | Model download percentage (diagnostic) |
| Last Transcript | text_sensor | Last recognized text (diagnostic) |
| STT Emotion | text_sensor | Detected emotion from last utterance (opt-in) |
| Download Model | button | Trigger model download (diagnostic) |
| Restart STT Server | button | Restart the Wyoming server (diagnostic) |
| Delete Model | button | Delete installed model (diagnostic) |

**Configuration:**

| Setting | Default | Description |
|---------|---------|-------------|
| Recognition Language | en | Fixed decode language: en, zh, ja, ko, yue |
| TCP Port | 10300 | Wyoming server port |
| Listen Address | 0.0.0.0 | Bind address |
| Auto Start | On | Start server when mod is enabled |
| mDNS Discovery | On | Broadcast `_wyoming._tcp` for HA auto-discovery |
| Inference Threads | 2 | CPU threads for SenseVoice inference (1-8) |
| Show Emotion Sensor | On | Expose STT Emotion as HA text sensor |
| Show Diagnostic Entities | Off | Expose server, model status, download, and control buttons |
| Auto Download Model | Off | Download the ~230MB model on first start |

**Requirements:** Android 9+ (API 28), Internet permission (for model download only), ~230MB storage for model

**How to Use:**
1. Install the HA STT Engine mod
2. Set **Recognition Language** before downloading (e.g. `zh` for Chinese)
3. Either enable **Auto Download Model** or tap **Download Model** in mod settings
4. Wait for the ~230MB model download to complete (progress shown in mod settings)
5. In Home Assistant, add the **Wyoming Protocol** integration
6. Connect to the Ava device IP on port `10300`
7. Select **HA STT Engine** as the ASR engine in your voice assistant pipeline

**Model Storage:**

```
Android/data/com.example.ava/files/ha-stt-engine/
├── model.int8.onnx
├── tokens.txt
└── downloaded
```

---

### BLE ADV Proxy

**Version:** 0.4.9 | **Author:** Ava

ha-ble-adv BLE proxy. Scans nearby devices and controls fans or lamps by exposing ESPHome services to Home Assistant.

**What It Does:**
- Exposes ESPHome `setup_svc_v0`, `adv_svc`, `adv_svc_v1` services to Home Assistant
- Scans nearby raw BLE broadcasts and fires `esphome.ble_adv.raw_adv` events
- Queued raw ADV transmit with deduped scan
- Optional adapter name sensor for multi-proxy setups
- Disables Ava's built-in Bluetooth proxy when enabled (this mod owns BLE scan/TX)

**Use Case:**
You have BLE-controlled ceiling fans or lamps that use raw BLE advertising protocols (the kind ha-ble-adv targets). Instead of buying an ESP32 proxy for each room, an Ava device already in that room can act as the BLE ADV proxy. Home Assistant's ha-ble-adv integration talks to Ava over ESPHome services, exactly like an ESP32 would.

**Entities Exposed to Home Assistant:**

| Entity | Type | Description |
|--------|------|-------------|
| BLE ADV Proxy Name | text_sensor | Adapter name for multi-proxy setups (diagnostic) |

**Configuration:**

| Setting | Default | Description |
|---------|---------|-------------|
| Enable BLE ADV Proxy | On | Expose ble_adv_proxy services and events to Home Assistant |
| Max TX Power | On | Use highest BLE advertise TX power when sending control packets |
| Force BLE Broadcast | On | Forces standard BLE broadcast (works on all phones). Turn off only on rooted devices with kernel HCI for true 1:1 with Flags |
| Adapter Name | (empty) | Optional unique adapter name for ha-ble-adv (blank = use device name) |

**Requirements:** Bluetooth permissions (scan, connect, advertise)

**How to Use:**
1. Install the BLE ADV Proxy mod
2. Grant Bluetooth permissions if prompted
3. In Home Assistant, use the ha-ble-adv integration as you would with an ESP32 proxy
4. Ava's ESPHome services (`setup_svc_v0`, `adv_svc_v1`) appear automatically
5. Scanned BLE broadcasts fire as `esphome.ble_adv.raw_adv` events in HA

---

### DLNA Renderer

**Version:** 1.5.7 | **Author:** Ava

Turns Ava into a standard UPnP/DLNA MediaRenderer on the local network. Any DLNA controller can push audio directly to the device — no Music Assistant or Home Assistant required for playback.

**What It Does:**
- UPnP/DLNA MediaRenderer:1 discovery on LAN
- AVTransport:1, RenderingControl:1, ConnectionManager:1 services
- Playback via Android MediaPlayer (MP3, AAC/M4A, FLAC, WAV, OGG)
- Audio focus management — pauses Ava's own media during DLNA playback
- Voice ducking — lowers DLNA volume during wake/listen/TTS
- Optional Cinema overlay UI for full-screen playback
- Dual output (speaker + earpiece) on supported devices

**Use Case:**
You want to cast music to an Ava device (smart speaker, tablet) from any DLNA controller app — BubbleUPnP, foobar2000, Windows "Cast to Device", Synology DS Audio — without setting up Music Assistant. Ava just appears as a DLNA renderer on the network. When you talk to the voice assistant, music ducks automatically and resumes after.

**Supported Controllers:**
- BubbleUPnP, mconnect, Hi-Fi Cast (Android)
- foobar2000 (UPnP output), JRiver, AudioStation / DS Audio (Synology NAS)
- Windows Explorer "Cast to Device"

**Entities Exposed to Home Assistant:**

All entities are off by default. Enable **Show Diagnostic Entities** in mod settings.

| Entity | Type | Description |
|--------|------|-------------|
| DLNA Renderer | binary_sensor | Renderer online status (diagnostic) |
| DLNA Playback State | text_sensor | playing / paused / loading / stopped (diagnostic) |
| Restart DLNA Renderer | button | Restart the UPnP stack (diagnostic) |
| Stop DLNA Playback | button | Stop DLNA playback (diagnostic) |

**Configuration:**

| Setting | Default | Description |
|---------|---------|-------------|
| Device Name | Ava | Friendly name shown in controller apps |
| Auto Start | On | Start renderer when mod is enabled |
| Allow Volume Control | On | Let DLNA controllers change device media volume |
| Voice Ducking | On | Lower playback during voice interactions |
| Cinema Overlay | Off | Show full-screen Cinema player UI during playback |
| Keep Playback Bar Visible | On | Keep bottom playback bar visible; top bar auto-hides |
| Dual Output (Speaker + Earpiece) | Off | Play on speaker and earpiece together |
| Show Diagnostic Entities | Off | Expose renderer status entities in HA |

**Requirements:** Android 5.0+ (API 21), Internet, WiFi, and wake lock permissions

**How to Use:**
1. Install the DLNA Renderer mod
2. Set **Device Name** if you want a custom name in controller apps
3. Open any DLNA controller app on the same network (e.g. BubbleUPnP)
4. Ava appears as a renderer — select it and push music
5. Talk to Ava normally — playback ducks during voice interaction and resumes after

---

### Screen Color Filter

**Version:** 1.0.1 | **Author:** Ava

Applies a global screen color tint overlay controllable from Home Assistant. Full-screen translucent overlay sits above all Ava layers with touch passthrough.

**What It Does:**
- HA `select` entity with options: off, red, blue, dark, yellow, green, gray
- Full-screen translucent overlay above all Ava foreground layers
- Touch passthrough (`FLAG_NOT_TOUCHABLE`) — tint doesn't block interaction
- Configurable opacity (0-100)
- Persists selected color across restarts
- Opt-in `overlay_z_order` hook keeps tint above notification/voice overlays

**Use Case:**
- Night mode: switch to **red** or **dark** at sunset to reduce blue light
- Accessibility: **yellow** tint for reduced contrast sensitivity
- Ambient matching: tint the screen to match room lighting
- Schedule color changes from Home Assistant automations

**Entities Exposed to Home Assistant:**

| Entity | Type | Description |
|--------|------|-------------|
| Screen Color Filter | select | off, red, blue, dark, yellow, green, gray |

Example HA service call:
```yaml
service: select.select_option
target:
  entity_id: select.<device>_screen_color_filter_color_filter
data:
  option: red
```

**Configuration:**

| Setting | Default | Description |
|---------|---------|-------------|
| Filter Opacity | 30 | Tint strength 0 (transparent) to 100 (strongest), step 5 |

**Requirements:** Android 6.0+ recommended, Ava build with `ModOverlayZOrderBridge` support, Display over other apps permission

**How to Use:**
1. Install the Screen Color Filter mod
2. Grant display-over-other-apps permission if prompted
3. Set opacity in mod settings (default 30, lower = subtler)
4. Control the tint from Home Assistant's `select` entity
5. Automate color changes with HA automations (e.g. red at sunset)

---

### Flashlight

**Version:** 1.0.0 | **Author:** Ava

Control the device flashlight (camera flash) from Home Assistant.

**What It Does:**
- Exposes a button entity to toggle the flashlight on/off

**Use Case:**
You have an Android device with a camera flash running Ava as a smart display or satellite. You want to use the flash as a notification light, emergency strobe, or remote-controlled flashlight from Home Assistant automations.

**Entities Exposed to Home Assistant:**

| Entity | Type | Description |
|--------|------|-------------|
| Toggle Flashlight | button | Toggle the flashlight on/off |

**Requirements:** Android 5.0+ (API 21) with Camera2 API, Camera permission, Physical flashlight hardware

**How to Use:**
1. Install on any device with a flashlight
2. Grant camera permission if prompted
3. Toggle the flashlight from the HA button entity

---

### GPS Location

**Version:** 1.3.0 | **Author:** Ava

Exposes device GPS location to Home Assistant with configurable sensors and update behavior. Includes reverse geocoding via OpenStreetMap Nominatim.

**What It Does:**
- Latitude, longitude, altitude, speed, and accuracy sensors
- Configurable GPS or network provider
- Configurable update interval and minimum distance
- Per-sensor enable/disable toggles
- Force refresh button

**Use Case:**
You have an Android device running Ava in a vehicle, on a robot, or as a portable tracker. You want real-time GPS location in Home Assistant for presence detection, tracking, or automation triggers based on location.

**Entities Exposed to Home Assistant:**

| Entity | Type | Description |
|--------|------|-------------|
| Latitude | sensor | Current latitude |
| Longitude | sensor | Current longitude |
| Altitude | sensor | Current altitude |
| Speed | sensor | Current speed |
| Accuracy | sensor | GPS accuracy in meters |
| Refresh Location | button | Force a location update |

**Configuration:**

| Setting | Default | Description |
|---------|---------|-------------|
| Provider | gps | GPS_PROVIDER or NETWORK_PROVIDER |
| Update Interval (seconds) | 5 | How often to request location updates |
| Min Distance (meters) | 1 | Minimum movement to trigger update |
| Enable Latitude | On | Expose latitude sensor |
| Enable Longitude | On | Expose longitude sensor |
| Enable Altitude | On | Expose altitude sensor |
| Enable Speed | On | Expose speed sensor |
| Enable Accuracy | On | Expose accuracy sensor |

**Requirements:** Android 5.0+ (API 21), Location permission (ACCESS_FINE_LOCATION), GPS or network location provider

---

### Sticky Note

**Version:** 1.0.0 | **Author:** Ava

Shows a draggable on-screen note controlled from Home Assistant. Display text, notes, or reminders directly on the Ava screen.

**What It Does:**
- Draggable sticky note overlay on the screen
- Text content controlled from Home Assistant
- Color selection: yellow, pink, blue, green, orange, purple, dark
- Adjustable size (50-200%)
- Clear button to reset the note
- Touch passthrough — note doesn't block other interactions

**Use Case:**
You want to display reminders, shopping lists, or notes on an Ava device (smart display, tablet) that family members can see. Control the note content from Home Assistant automations or manually through the HA text entity.

**Entities Exposed to Home Assistant:**

| Entity | Type | Description |
|--------|------|-------------|
| Sticky Note | text | Note content displayed on screen |
| Note Color | select | yellow, pink, blue, green, orange, purple, dark |
| Note Size | number | Size 50-200% (slider) |
| Clear Note | button | Clear the note content |

**Configuration:**

No configuration options — all controls are through Home Assistant entities.

**Requirements:** Android 6.0+ recommended, Ava build with `ModOverlayZOrderBridge` support, Display over other apps permission

**How to Use:**
1. Install the Sticky Note mod
2. Grant display-over-other-apps permission if prompted
3. Set the note text from the HA text entity
4. Change color and size from HA select and number entities
5. Drag the note to your preferred position on screen
6. Use the Clear Note button to reset

---

### WiFi & ADB Keep-Alive

**Version:** 1.1.1 | **Author:** Ava

Keep WiFi and ADB debugging alive across crashes and reboots. Root preferred, Shizuku fallback.

**What It Does:**
- Monitors WiFi state and re-enables if dropped
- Monitors ADB debugging (over WiFi) and re-enables if disabled
- Survives app crashes and device reboots

**Use Case:**
You have an Ava device running headless or in a remote location. WiFi or ADB occasionally drops after crashes or reboots, cutting off remote access. This mod automatically restores connectivity so you don't need physical access to fix it.

**Requirements:** Root access preferred, Shizuku as fallback for non-rooted devices

---

### OpenClaw (Mini)

**Version:** 1.5.0 | **Author:** Ava

A pure-Java Android AI assistant that runs as a mod inside Ava. Connects any OpenAI or Anthropic-compatible LLM to device-level tools via an agentic loop. No separate APK, no framework dependency.

**What It Does:**
- Execute shell commands and Termux scripts
- Automate UI interactions via Accessibility Service (click, scroll, type, screenshot)
- Read and write files on the device
- Control Ava's internal browser
- Search the web (Tavily API with 17 fallback engines)
- Schedule recurring tasks via cron
- Maintain persistent memory and learn skills

**Use Case:**
You want an AI assistant that can actually **do things** on the device — not just chat. OpenClaw connects an LLM (OpenAI, Anthropic, OpenRouter, etc.) to real device tools. Talk to Ava, and the AI can run scripts, take screenshots, control apps, search the web, and remember context across conversations. Accessible via voice, web console, Telegram, or QQ.

**Channels:**

| Channel | Description |
|---------|-------------|
| Android | Native voice satellite integration via Ava |
| WebConsole | Local HTTP server with SSE streaming and tool call rendering |
| Telegram | Bot channel with long-polling |
| QQ | QQ Bot channel (sandbox supported) |

**Entities Exposed to Home Assistant:**

| Entity | Type | Description |
|--------|------|-------------|
| Agent Status | text_sensor | Current agent state |
| AI Response | text_sensor | Latest AI response (optional) |
| Heartbeat Status | text_sensor | Heartbeat monitoring state |
| Total Tokens | text_sensor | Cumulative token usage |
| Clear Memory | button | Clear all persistent memory |

**Configuration:**

| Setting | Default | Description |
|---------|---------|-------------|
| Provider | openai | API protocol: openai or anthropic |
| API Key | (empty) | Your LLM API key |
| Model | stepfun/step-3.5-flash | Model ID |
| Custom API URL | https://openrouter.ai/api/v1 | API endpoint |
| Max Tokens | 4096 | Max reply tokens (512-8192) |
| Max Tool Iterations | 30 | Max tool rounds per turn (1-100) |
| Tavily Key | (empty) | Optional Tavily web search API key |
| Telegram Token | (empty) | Optional Telegram bot token |
| QQ App ID | (empty) | Optional QQ bot app ID |
| QQ Client Secret | (empty) | Optional QQ bot secret |
| QQ Sandbox | Off | QQ sandbox mode |
| Heartbeat Enabled | On | Heartbeat monitoring |
| Expose AI Response | Off | Show AI responses as HA text sensor |
| Web Console Enabled | On | Enable local web console |
| Web Console Password | openclaw | Web console password |

**Requirements:** Android 9+ (API 28), Ava app installed, Internet permission. Optional: Shizuku or root for shell commands, Accessibility service for UI automation.

**How to Use:**
1. Install the OpenClaw mod
2. Configure your LLM provider, API key, and model in mod settings
3. Optionally configure Telegram, QQ, or web console channels
4. Talk to Ava normally — OpenClaw intercepts and processes with your chosen LLM
5. The AI can control the device, run scripts, take screenshots, and more
6. Access the web console at `http://device-ip:port` for a chat interface with tool call visualization
7. Use the Clear Memory button to reset the AI's persistent memory

---

### Qualcomm Audio Concurrency Fix

**Version:** 1.0.0 | **Author:** pantherale0

Fixes Qualcomm audio concurrency issues that block microphone access when wake sounds play during voice assistant activation.

**The Problem:**
On Qualcomm devices (ThinkSmart View and others using `msm8953-snd-card-mtp`), the HAL driver blocks microphone access when audio is already playing. When Ava plays a wake sound during `VOICE_ASSISTANT_STT_START`, the microphone fails with error code `-19` (ENODEV).

This mod fixes the issue by modifying the sound trigger configuration to allow concurrent audio sessions.

**Use Case:**
You have a ThinkSmart View or similar Qualcomm device where voice assistant wake sounds cause the microphone to fail. Without this fix, Ava can't hear you after playing a wake sound. This mod patches the system audio config to allow simultaneous audio playback and mic access.

**Entities Exposed to Home Assistant:**

| Entity | Type | Description |
|--------|------|-------------|
| Apply Audio Fix | button | Apply the concurrency fix |
| Fix Status | sensor | Current fix status |

**Supported Devices:**
- ThinkSmart View (Android 11 via treble)
- Other Qualcomm devices using `msm8953-snd-card-mtp` sound card
- Run `cat /proc/asound/cards` to confirm compatibility

**Requirements:** Root access is required to modify system files

**How to Use:**
1. Install on a compatible Qualcomm device
2. Tap **Apply Audio Fix** in mod settings or from the HA button
3. The mod modifies `/vendor/etc/sound_trigger_platform_info.xml` and `/vendor/build.prop`
4. Reboot the device after applying
5. Verify the fix by checking the Fix Status sensor
6. After the fix, wake sounds and microphone access work simultaneously

---

### Zigbee Gateway

**Version:** 1.1.2 | **Author:** Ava

Turns your Ava device into a Zigbee coordinator gateway by bridging a USB Zigbee dongle over TCP. Supports Tuya S6E/T6E/S7E/S9E and NSPanel Pro.

**What It Does:**
- Serial-to-TCP bridge for Zigbee coordinator dongles
- Configurable serial port, baud rate, TCP port, and bind address
- Hardware flow control (RTS/CTS) option
- Auto-start on Ava boot
- Status sensor and restart button in Home Assistant

**Use Case:**
You have an Ava device with a USB port (smart screen, tablet with USB OTG) and a Zigbee coordinator dongle (CC2652, ConBee, EZSP). Instead of buying a dedicated Zigbee gateway (Home Assistant Yellow, SkyConnect, etc.), the Ava device can serve as the Zigbee coordinator. Home Assistant's ZHA or Zigbee2MQTT connects to the Ava device over TCP.

**Entities Exposed to Home Assistant:**

| Entity | Type | Description |
|--------|------|-------------|
| Zigbee Status | binary_sensor | TCP server running status |
| Zigbee Restart | button | Restart the TCP server |

**Configuration:**

| Setting | Default | Description |
|---------|---------|-------------|
| Serial Port | /dev/ttyS5 | Serial device path for the Zigbee dongle |
| Baud Rate | 115200 | Serial communication speed |
| TCP Port | 8888 | TCP port for the bridge server |
| Listen Address | 0.0.0.0 | Bind address (0.0.0.0 = all interfaces) |
| Auto Start | On | Start the bridge automatically when Ava starts |
| RTS/CTS Flow | Off | Hardware flow control |

**Requirements:** Android device with USB host or serial port access, Root access for serial port access, Zigbee coordinator dongle (CC2652, ConBee, EZSP, etc.)

**How to Use:**
1. Install the Zigbee Gateway mod
2. Connect your Zigbee dongle to the device (USB or built-in serial)
3. Configure the serial port path, baud rate, and TCP port in mod settings
4. Enable Auto Start for automatic operation
5. In Home Assistant, add a Zigbee2MQTT or ZHA integration pointing to `device-ip:8888`
6. Use the Restart button if you need to restart the bridge
7. Monitor the Zigbee Status sensor to confirm the server is running

---

## Device Support Mods

Hardware-specific mods for particular devices or chip platforms. Install only on the matching device.

---

### Allwinner A64 Custom

**Version:** 1.0.5 | **Author:** Ava

Device compatibility hooks for Allwinner A64 chip devices, including Ococci tablets and smart screens.

**What It Does:**
- Home key remapping — configure short press and long press actions
- Inductor sensor support — GPIO 129 sensor for proximity-based screen toggle
- Volume control fix — corrects volume control behavior on A64 devices

**Use Case:**
You have an Ococci tablet or smart screen built on the Allwinner A64 SoC. The home key behavior is wrong, the inductor sensor (used for proximity-based screen on/off) isn't recognized, and volume control is broken. This mod fixes all three at the device level.

**Configuration:**

| Setting | Options | Default |
|---------|---------|---------|
| Home Key Short Press | screen_toggle, voice_wake, none | screen_toggle |
| Home Key Long Press | screen_toggle, voice_wake, service_toggle, none | voice_wake |
| Inductor Sensor (GPIO 129) | screen_toggle, none | screen_toggle |

**How to Use:**
1. Install on an Allwinner A64 device (Ococci tablet, smart screen, etc.)
2. Configure home key actions in mod settings
3. The inductor sensor automatically toggles the screen when triggered
4. No Home Assistant entities are exposed — this mod operates at the device level

---

### Echo Show Support

**Version:** 1.1.0 | **Author:** Ava

Device compatibility hooks for Amazon Echo Show models. Handles brightness, BLE, overlay, and screensaver dark-off behavior.

**What It Does:**
- Corrects audio routing for Echo Show's speaker and microphone
- Handles Echo Show-specific display behavior (brightness, overlay)
- BLE support for Echo Show
- Screensaver dark-off behavior
- Enables Ava to function as a voice satellite on Echo Show devices

**Use Case:**
You have an Amazon Echo Show that you want to repurpose as an Ava voice satellite and smart display. Echo Show has specific audio routing, display, and BLE quirks that standard Android apps don't handle. This mod applies the necessary device hooks automatically.

**How to Use:**
1. Install on an Amazon Echo Show device that has been set up for Android app installation
2. No configuration needed — the mod applies hooks automatically
3. No Home Assistant entities are exposed — this mod operates at the device level

---

### IR Blaster (ESPhome)

**Version:** 1.0.0 | **Author:** Ava

Turns the device's infrared emitter into a native Home Assistant IR transmitter via ESPHome integration.

**What It Does:**
- Auto-detects built-in Consumer IR blaster and known USB IR dongles
- Runs ESPHome native-API server on TCP port 6054
- Advertises as an infrared emitter entity to Home Assistant
- Home Assistant 2026.4+ discovers it as an ESPHome device
- Supports consumer IR integrations (LG, Samsung, Daikin, Edifier, etc.)
- mDNS discovery (`_esphomelib._tcp`) for auto-discovery

**Use Case:**
You have an Android device with an IR blaster (phone, tablet, smart speaker) running Ava. You want to control IR devices (TV, AC, audio system) from Home Assistant without buying a dedicated IR blaster like Broadlink RM. This mod makes the device appear as an ESPHome IR emitter in Home Assistant.

**Entities Exposed to Home Assistant:**

All entities are off by default. Enable **Show Diagnostic Entities** in mod settings.

| Entity | Type | Description |
|--------|------|-------------|
| IR API Server | binary_sensor | Server running status (diagnostic) |
| IR Emitter | text_sensor | Detected IR emitter info (diagnostic) |
| Last IR Transmit | text_sensor | Last IR transmission details (diagnostic) |
| Restart IR API Server | button | Restart the ESPHome server (diagnostic) |

**Configuration:**

| Setting | Default | Description |
|---------|---------|-------------|
| Enable IR API Server | Off | Advertise device as ESPHome IR emitter |
| TCP Port | 6054 | ESPHome native API port (different from Ava's 6053) |
| Listen Address | 0.0.0.0 | Bind address (0.0.0.0 = LAN, 127.0.0.1 = local only) |
| mDNS Discovery | On | Broadcast `_esphomelib._tcp` for HA auto-discovery |
| Show Diagnostic Entities | Off | Expose server status, emitter info, last transmit, restart button |

**Requirements:** Android 5.0+ (API 21), TRANSMIT_IR permission, Consumer IR hardware or USB IR dongle, Home Assistant 2026.4+ for infrared platform

**How to Use:**
1. Install the IR Blaster mod
2. Enable **Enable IR API Server** in mod settings
3. In Home Assistant, add **ESPHome** integration
4. Host: this device's LAN IP (or wait for auto-discovery)
5. Port: 6054 (default, change if configured differently)
6. Encryption key: leave blank (plaintext API)
7. The device appears with an **IR Blaster** infrared emitter entity
8. Add IR-controlled device integrations (Samsung Infrared, LG Infrared, etc.) and select this emitter

---

### Facebook Portal

**Version:** 1.1.3 | **Author:** Ava

Exposes Facebook Portal hardware sensors and controls to Home Assistant through Ava. Sensor logic adapted from the [portal-ha-bridge](https://github.com/RoadRunner-1024/portal-ha-bridge) project, without requiring MQTT.

**Supported Devices:** Facebook Portal family on Android 9-10: Portal (10"), Portal Mini, Portal+ (1st and 2nd gen).

**What It Does:**
- Meta face-presence detection via logcat
- Ambient light, color channels, and temperature sensors
- Accelerometer and tap/tilt detection
- Sound level monitoring
- Synthesized doorbell and alert tones on media stream
- Screen timeout with presence-aware wake

**Use Case:**
You have a Facebook Portal device sitting unused after Meta discontinued the line. With Ava + this mod, it becomes a Home Assistant sensor hub: presence detection, ambient light, temperature, sound level, and tap/tilt gestures — all exposed to HA. The doorbell and alert tones can be triggered from HA automations.

**Entities Exposed to Home Assistant:**

All features are disabled by default. Enable each one in the mod settings before it appears in Home Assistant.

| Entity | Type | Description |
|--------|------|-------------|
| Portal Presence | binary_sensor | Meta face-presence detection via logcat |
| Presence Detection | switch | Enable/disable presence monitoring |
| Ambient Light | sensor | Lux reading from TCS34x0 sensor |
| Light R / G / B | sensor | Color channels (hardware dependent) |
| Temperature | sensor | Ambient temperature (hardware dependent) |
| Temperature Offset | number | Calibration offset for temperature |
| Tap Tilt | text_sensor | left / right / up / down / front / back |
| Tap Tilt Sensitivity | number | Tap detection threshold |
| Accel X / Y / Z | sensor | Raw accelerometer values |
| Sound Level | sensor | 0-100 ambient loudness |
| Doorbell | button | Synthesized doorbell tone on media stream |
| Alert | button | Synthesized alert tone on media stream |
| Screen Timeout | switch | Idle screen-off timer |
| Screen Timeout Minutes | number | 1-240 minutes, presence keeps screen awake |

**Permissions (require ADB or root):**

| Permission | Used For |
|------------|----------|
| READ_LOGS | Portal presence sensor |
| RECORD_AUDIO | Sound level sensor |
| WRITE_SECURE_SETTINGS | Screen sleep fallback |
| CAMERA | Reserved for future camera features |
| WRITE_SETTINGS | Brightness control |
| SYSTEM_ALERT_WINDOW | Background overlay access |

**How to Use:**
1. Install the Facebook Portal mod from Settings -> Advanced -> Mod Store
2. Download `provision.sh` from the [mod source](https://github.com/knoop7/ava-mods/tree/main/mods/devices/portal-support)
3. `chmod +x provision.sh`
4. Connect your Portal via USB and verify ADB: `adb devices`
5. Run: `./provision.sh com.example.ava`
6. Open mod settings and enable the features you need
7. For presence or screen timeout, also turn on the corresponding switch in Home Assistant

---

### Phicomm R1

**Version:** 1.3.0 | **Author:** Ava

Phicomm R1 smart speaker support with native LED ring control, voice-reactive lighting, and music RGB effects.

**What It Does:**
- Stock msgcenter voice LED behavior replication
- Directional wake LEDs (1-24 white LEDs) via 4-mic array
- RGB ring music visualization (modes 0-3)
- Voice pipeline integration for wake/loading lights
- Dormant light mode (double-click top cover)
- Top key gesture sensor (single/double/triple/long press)
- Network disconnect LED indicator

**Use Case:**
You have a Phicomm R1 smart speaker that you want to run Ava on. The stock 小讯 (Unisound) firmware has specific LED behaviors for voice interaction and music that you want to preserve. This mod replicates the stock LED ring behavior while using Ava's voice pipeline, adds music RGB effects, and exposes device controls to Home Assistant.

**Entities Exposed to Home Assistant:**

| Entity | Type | Description |
|--------|------|-------------|
| Voice LED | switch | Enable/disable voice LED effects |
| Music RGB Light | switch | Enable/disable music RGB ring |
| Top Key | text_sensor | Last top-cover gesture (diagnostic) |
| Music Light Backend | text_sensor | Music light implementation (diagnostic) |

**Configuration:**

| Setting | Default | Description |
|---------|---------|-------------|
| 4-Mic Array (DOA) | Off | Enable directional wake LEDs (requires stock libUni4micHalJNI) |
| Reverse DOA Angle | Off | Mirror wake direction if LED lights on opposite side |
| Voice LED Effects | On | Pipeline wake/loading lights |
| Music RGB Light | On | RGB ring during music playback |
| Music Light Style | 0 | 0 = breathing, 1-3 = spectrum effects |
| Dormant Light | On | Double-click top cover enters dormant mode |
| Network Disconnect LED | Off | Light 254 when offline |
| Volume Ring LED (mod) | Off | Enable if system volume LEDs are missing |
| Top Key Sensor | On | Expose top key gestures as sensor |

**Requirements:** Phicomm R1 device, Root access (for 4-mic array and native libraries), msgcenter IPC availability

**How to Use:**
1. Install on a Phicomm R1 device
2. Enable **Voice LED Effects** for wake/loading lights
3. Enable **Music RGB Light** and select **Music Light Style** for music visualization
4. Optionally enable **4-Mic Array (DOA)** for directional wake LEDs
5. Use the Voice LED and Music RGB Light switches in Home Assistant
6. Monitor Top Key sensor for gesture events
7. If wake LEDs light on opposite side, enable **Reverse DOA Angle**

---

### Tuya S8E

**Version:** 1.0.10 | **Author:** Ava

Device support for Tuya S8E smart screens with screen backlight control, small display control, and climate readings.

**What It Does:**
- Main screen backlight toggle
- Small display backlight toggle
- Ambient temperature and humidity sensors
- Rotary knob position with reset
- Gesture direction detection

**Use Case:**
You have a Tuya S8E smart screen running Ava. You want to control the backlights (main and small display) from Home Assistant, monitor room temperature and humidity, and use the rotary knob and gesture detection for automations.

**Entities Exposed to Home Assistant:**

| Entity | Type | Description |
|--------|------|-------------|
| Screen Backlight | switch | Toggle the main screen backlight |
| Small Screen Backlight | switch | Toggle the small display backlight |
| Temperature | sensor | Ambient temperature in °C |
| Humidity | sensor | Ambient humidity in % |
| Rotary Position | text_sensor | Current rotary knob position |
| Reset Rotary Position | button | Reset the rotary position counter |
| Gesture Direction | text_sensor | Detected gesture direction |

**How to Use:**
1. Install on a Tuya S8E device
2. No configuration needed — all entities appear automatically in Home Assistant
3. Control backlights from HA switches
4. Monitor temperature and humidity from HA sensors
5. The rotary position and gesture direction update in real time (500ms refresh)

---

### YX LED Controller

**Version:** 1.0.0 | **Author:** slovebj

LED and motion sensor support for YX M5612 devices.

**What It Does:**
- Individual RGB LED control (blue, red, green)
- PIR motion sensor exposure

**Use Case:**
You have a YX M5612 device with RGB LEDs and a PIR motion sensor. You want to control each LED color independently from Home Assistant and use the motion sensor for presence-based automations.

**Entities Exposed to Home Assistant:**

| Entity | Type | Description |
|--------|------|-------------|
| Blue LED | switch | Toggle blue LED |
| Red LED | switch | Toggle red LED |
| Green LED | switch | Toggle green LED |
| Motion Sensor | binary_sensor | PIR motion detection |

**How to Use:**
1. Install on a YX M5612 device
2. No configuration needed — all entities appear automatically in Home Assistant
3. Toggle individual LED colors from HA switches
4. Use the motion sensor for presence-based automations

---

## Which Mod Should I Install?

| Situation | Mod |
|-----------|-----|
| Need TTS for HA voice pipeline | [HA Edge TTS](#ha-edge-tts) |
| Need offline STT for HA voice pipeline | [HA STT Engine](#ha-stt-engine) |
| BLE-controlled ceiling fans or lamps | [BLE ADV Proxy](#ble-adv-proxy) |
| Cast music to Ava from any DLNA app | [DLNA Renderer](#dlna-renderer) |
| Screen tint / night mode / blue light filter | [Screen Color Filter](#screen-color-filter) |
| Remote-controlled flashlight | [Flashlight](#flashlight) |
| GPS tracking or presence detection | [GPS Location](#gps-location) |
| On-screen note / reminder display | [Sticky Note](#sticky-note) |
| WiFi or ADB keeps dropping | [WiFi & ADB Keep-Alive](#wifi--adb-keep-alive) |
| On-device AI assistant with tool use | [OpenClaw (Mini)](#openclaw-mini) |
| Qualcomm mic fails on wake sound | [Qualcomm Audio Concurrency Fix](#qualcomm-audio-concurrency-fix) |
| Ava device as Zigbee coordinator | [Zigbee Gateway](#zigbee-gateway) |
| Allwinner A64 tablet / smart screen | [Allwinner A64 Custom](#allwinner-a64-custom) |
| Amazon Echo Show | [Echo Show Support](#echo-show-support) |
| Facebook Portal | [Facebook Portal](#facebook-portal) |
| IR blaster for Home Assistant | [IR Blaster (ESPhome)](#ir-blaster-esphome) |
| Phicomm R1 smart speaker | [Phicomm R1](#phicomm-r1) |
| Tuya S8E smart screen | [Tuya S8E](#tuya-s8e) |
| YX M5612 LED controller | [YX LED Controller](#yx-led-controller) |

---

*Back to [Mod Store](Mod-Store) | [Home](Home)*
