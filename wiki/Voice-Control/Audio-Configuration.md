# Audio Configuration

Configure how Ava captures and processes audio from the microphone.

Compatible with Android 5-16.

---

## System Recording Mode

Choose how the microphone captures audio:

| Mode | Description |
|------|-------------|
| Auto select | Lets the device choose. Best default |
| Speech boost | Tries to make speech clearer. May change tone |
| Normal pickup | Plain microphone input. Safest choice |
| Call boost | More like call audio. May over-process |
| Unprocessed | Closest to raw input. Good for testing |

---

## Audio Processing

| Option | Description |
|--------|-------------|
| Noise suppression | Use device noise suppressor when available |
| Automatic gain control | Use device AGC when available |
| Hardware echo cancellation | Enable hardware echo cancellation when supported |
| Software echo cancellation | Cancel Ava's own playback from microphone so wake words work during music/TTS playback |

> **Software AEC** is recommended when using music playback or TTS. It replaces the device echo canceler while enabled.

---

## Software Mic Gain

Boost PCM audio before wake word detection and streaming. Range: 0-24 dB.

---

## Audio Config Presets

| Preset | Description |
|--------|-------------|
| Standard 16kHz Mono | Default for voice recognition |
| Broadcast 48kHz Mono | Higher quality capture |
| Stereo Input 48kHz | Stereo recording |
| Low Latency 16kHz | Reduced latency |
| Voice Call Optimized | For voice call use case |
| Unprocessed Raw 48kHz | Raw capture, no processing |
| Compact 8kHz | Lowest bandwidth |
| CD Quality 44.1kHz | High quality |
| Studio 96kHz | Very high quality |
| Ultra HD 192kHz | Maximum quality |

---

## Voice Channel

The Voice Channel switch disables all voice input, wake word, and voice assistant threads. Service restart required to apply.

---

## Continuous Conversation

Continuous conversation lets you issue multiple commands without saying the wake word each time.

### How It Works

1. Say wake word + first command
2. After Ava responds, automatically enters listening mode
3. Say next command directly (no wake word needed)
4. After 10 seconds of silence, exits continuous conversation mode

### End Conditions

| Mode | Description |
|------|-------------|
| Exit Keyword Stop | End when the assistant says goodbye or farewell phrases (default) |
| Question Mark Continue | Keep listening only if the reply ends with "?"; stop after other replies |

### How to Enable

1. Go to **Settings** -> **Interaction**
2. Turn on **Continuous Conversation** switch
3. Choose end condition mode

---

## Conversation Subtitles

Conversation subtitles display what you said and Ava's response on screen as a floating window.

### How to Enable

1. Go to **Settings** -> **Interaction**
2. Turn on **Floating Subtitle** switch

---

## Mute Mode

Mute mode turns off the microphone, Ava won't respond to any wake words.

### How to Enable

**Method 1: In Settings**
1. Go to **Settings** -> **Voice Config**
2. Turn on **Mute** switch

**Method 2: Home Assistant Control**
```yaml
service: switch.turn_on
target:
  entity_id: switch.your_device_name_mute
```

---

*Back to [Voice Control](Voice-Control)*
