# Settings Summary & Services

Compatible with Android 5-16.

---

## Settings Summary

| Setting | Location | Description | Default |
|---------|----------|-------------|---------|
| Device Name | Voice Config | Name shown in HA | device_model_voice_assistant |
| Port | Voice Config | ESPHome communication port | 6053 |
| Wake Word Engine | Voice Config | microWakeWord or vsWakeWord | microWakeWord |
| Wake Word 1 | Voice Config | Primary wake word | Hey Jarvis |
| Wake Word 2 | Voice Config | Secondary wake word | None |
| Sensitivity | Voice Config | Wake word sensitivity | - |
| Wake Sound | Voice Config | Prompt sound when recording starts | Optional |
| Wake Word Library | Voice Config | Import custom wake word models | Empty |
| Voice Channel | Voice Config | Master voice input switch | On |
| System Recording Mode | Voice Config | Audio capture mode | Auto select |
| Noise suppression | Voice Config | Device noise suppressor | - |
| Automatic gain control | Voice Config | Device AGC | - |
| Hardware echo cancellation | Voice Config | Hardware AEC | - |
| Software echo cancellation | Voice Config | Software AEC for playback | - |
| Voiceprint recognition | Voice Config → Voiceprint | Speaker recognition on/off | Off |
| Voiceprint mode | Voice Config → Voiceprint | Manual (record 5 samples) or Automatic (passive learning) | Automatic |
| Voiceprint User 1 name | Voice Config → Voiceprint | Display name for speaker 1 | User 1 |
| Voiceprint User 2 name | Voice Config → Voiceprint | Display name for speaker 2 | User 2 |
| Wake-word voiceprint check | Voice Config → Voiceprint | Restrict wake to enrolled speakers (manual mode only) | Off |
| Audio event detection | Voice Config → Audio events | Local sound detection on/off | Off |
| Audio event display duration | Voice Config → Audio events | How long sensor holds event label | 30s |
| Audio event monitored types | Voice Config → Audio events | Which sound types to detect | Speech only |
| Audio event sensitivity | Voice Config → Audio events | Detection sensitivity preset | Balanced |
| Software Mic Gain | Voice Config | PCM boost 0-24 dB | 0 |
| Audio Config | Voice Config | Capture preset | Standard 16kHz Mono |
| Mute | Voice Config | Turn off microphone | Off |
| Continuous Conversation | Interaction | Issue commands continuously | Off |
| Floating Subtitle | Interaction | Display conversation text | Off |

---

## Home Assistant Services

### Manually Trigger Wake

```yaml
service: esphome.your_device_name_trigger_wake
data: {}
```

### Control Mute

```yaml
# Enable mute
service: switch.turn_on
target:
  entity_id: switch.your_device_name_mute

# Disable mute
service: switch.turn_off
target:
  entity_id: switch.your_device_name_mute
```

### Set Volume

```yaml
service: media_player.volume_set
target:
  entity_id: media_player.your_device_name
data:
  volume_level: 0.8  # 0.0 - 1.0
```

### Send Voice Command (Text)

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_voice_command
data:
  value: "Turn on the living room light"
```

---

## FAQ

### Ava can't hear me?

1. Check if microphone permission is granted
2. Check if mute mode is enabled
3. Check if Voice Channel is enabled
4. Make sure device volume isn't muted
5. Try speaking closer to device
6. Try a different System Recording Mode

### Wake word recognition inaccurate?

1. Try a different wake word
2. Adjust wake word sensitivity
3. Make sure environment isn't too noisy
4. Enable noise suppression
5. Speak at moderate speed, pronounce clearly
6. Try vsWakeWord engine — its CTC phoneme matching is more robust to noise and accents

### Wake word doesn't work during music playback?

1. Enable **Software echo cancellation** in Settings -> Voice Config
2. This cancels Ava's own playback from the microphone input

### Speech recognition results wrong?

This is usually a Home Assistant issue:
1. Check HA voice assistant configuration
2. Make sure Whisper etc. components are working
3. Check network latency
4. Try speaking more clearly

---

*Back to [Voice Control](Voice-Control)*
