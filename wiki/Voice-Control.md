# Voice Control

Voice control is the core feature of Ava, allowing you to control smart home devices by speaking.

Compatible with Android 5-16.

---

## How It Works

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  You speak  │ -> │ Ava records │ -> │Home Assistant│ -> │ Ava plays   │
│ wake word + │    │ sends audio │    │   speech    │    │   voice     │
│   command   │    │             │    │ recognition │    │   reply     │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

**Detailed Flow:**

1. **Standby**: Ava continuously listens for wake word (local processing, no internet)
2. **Wake Detection**: When wake word detected, plays prompt sound, starts recording
3. **Audio Transmission**: Recording sent to Home Assistant via ESPHome protocol
4. **Speech Recognition**: Home Assistant's voice assistant performs speech-to-text
5. **Intent Processing**: Home Assistant understands intent and executes action
6. **Speech Synthesis**: Home Assistant generates voice response
7. **Playback**: Ava receives and plays voice response

---

## Topics

| Topic | Description |
|-------|-------------|
| [LLM Integration](LLM-Integration) | Claw Assistant — multi-turn tool calls, Ava device detection, voice intent map, ADB runbook, workspace persona, 50+ tools |
| [Wake Word Engines](Wake-Word-Engines) | microWakeWord vs vsWakeWord — architecture, comparison, engine switching, visual feedback, stop words |
| [Custom Wake Words](Custom-Wake-Words) | Import custom wake word models via in-app library or APK injection — microWakeWord and vsWakeWord formats |
| [Voiceprint Recognition](Voiceprint) | On-device speaker identification — Manual mode (enrollment-based gating) and Automatic mode (passive learning) |
| [Audio Event Detection](Audio-Event-Detection) | Local detection of household sounds — alarm, baby crying, doorbell, glass breaking, siren, cough, speech |
| [Audio Configuration](Audio-Configuration) | Microphone settings, noise suppression, echo cancellation, gain, continuous conversation, subtitles, mute |
| [Text to Speech](Text-to-Speech) | Standard vs Streaming TTS playback, whisper response, announcement, pipeline error i18n, recommended TTS engine |
| [Settings Summary & Services](Settings-Summary) | Complete settings reference, Home Assistant services, and FAQ |

---

## On-Device TTS & STT Engines

Ava ships with two self-developed voice engine mods that run entirely on the Android device — no separate server, no cloud dependency for STT:

| Engine | Protocol | Port | Description |
|--------|----------|------|-------------|
| **HA Edge TTS** | Wyoming TTS | 10301 | Cloud-based TTS using Microsoft Edge TTS voices. 400+ voices in 40+ languages. Natural neural voices without API costs. |
| **HA STT Engine** | Wyoming STT | 10300 | Offline speech recognition using SenseVoice model. Supports zh / en / ja / ko / Cantonese. ~230MB model download. |

Both mods expose Wyoming protocol servers for Home Assistant auto-discovery via mDNS. Install them from the [Mod Store](Mod-Store) — see the [Mod Catalog](Mod-Store-Catalog) for full details, configuration, and setup instructions.

**Setup:**
1. Install **HA Edge TTS** and/or **HA STT Engine** from Settings -> Advanced -> Mod Store
2. For STT, download the model (~230MB) in mod settings
3. In Home Assistant, add the **Wyoming Protocol** integration
4. Connect to the Ava device IP on port `10301` (TTS) or `10300` (STT)
5. Select the engine in your voice assistant pipeline

---

*Back to [Home](Home)*
