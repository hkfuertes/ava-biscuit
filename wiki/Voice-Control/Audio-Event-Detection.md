# Audio Event Detection

Ava Pro includes an on-device audio event detection system that listens for common household sounds and reports them to Home Assistant. The entire pipeline runs locally — no audio leaves the device, no cloud services involved.

Audio event detection operates on a dedicated processing thread, completely isolated from wake word detection and voiceprint processing. This means event detection never adds latency to wake detection, and vice versa.

Compatible with Android 5-16.

---

## Performance

| Metric | Value |
|--------|-------|
| Overall accuracy | **98.2%** |
| False positive rate | 1.3% |
| Detection latency | < 200ms (single window) |
| CPU impact (idle room) | Near zero — inference skipped on silent windows |
| CPU impact (active detection) | Single-thread TFLite, adaptive interval 1-2s |

---

## How It Works

The system captures 1-second audio windows from the microphone and runs a lightweight TFLite classifier. Each window is analyzed independently — no continuous recording, no streaming.

The processing pipeline:

1. **Window capture** — 1 second of 16kHz mono audio
2. **Quality gate** — silent or dead audio is discarded before inference
3. **Feature extraction** — 40-band mel-frequency spectrogram (99 frames × 40 filters)
4. **Classification** — 7-class TFLite model outputs confidence scores
5. **Confidence gate** — only windows above the sensitivity threshold pass
6. **Speech confusion guard** — if speech is a strong runner-up, non-speech events are suppressed
7. **Confirmation** — borderline detections require a second confirming window within 4-6 seconds
8. **Publish** — confirmed event label is sent to the Home Assistant sensor

The classify interval adapts to device performance: fast devices run inference every 1 second, slower devices every 1.5-2 seconds. Low-RAM devices also receive slightly stricter quality gates to reduce false positives.

---

## Detection Types

Seven sound categories are supported:

| Type | Description | Example Use Case |
|------|-------------|-----------------|
| **Alarm** | Smoke and CO alarm tones | Fire safety monitoring |
| **Baby crying** | Infant crying or distress sounds | Baby monitor automation |
| **Cough** | Coughing sounds | Health tracking |
| **Doorbell** | Doorbell and chime sounds | Visitor notification |
| **Glass breaking** | Shattering or impact on glass | Security automation |
| **Siren** | Emergency vehicle and outdoor sirens | Emergency alert |
| **Speech** | Talking and conversation | Presence detection |

By default, only **speech** monitoring is enabled. Additional types can be toggled in Settings. At least one type must remain enabled — the last type cannot be turned off.

---

## Sensitivity

Three sensitivity presets control how easily events trigger:

| Preset | Description | Best For |
|--------|-------------|----------|
| **Fewer false alerts** (Conservative) | Stricter confidence and quality gates | Quiet homes, low false positive tolerance |
| **Balanced** (Default) | Recommended threshold for most rooms | General household use |
| **Catch more events** (Sensitive) | Looser gates, may increase false positives | Critical monitoring, noisy environments |

Sensitivity affects four internal thresholds:
- Minimum confidence score to accept a detection
- Speech runner-up suppression level
- Feature quality floor
- Single-window strong detection threshold

---

## Home Assistant Entity

```
sensor.your_device_name_audio_event
```

The sensor reports the detected event label (e.g., `alarm`, `baby_cry`, `doorbell`), or `idle` when no event is active.

After an event is published, the sensor holds the label for a configurable duration (5-60 seconds, default 30 seconds) before returning to `idle`. The display duration can be adjusted in Settings and takes effect immediately.

### Automation Example

```yaml
# Notify when a doorbell is detected
trigger:
  - platform: state
    entity_id: sensor.your_device_name_audio_event
    to: "doorbell"
action:
  - service: notify.mobile_app
    data:
      message: "Doorbell detected at home"
```

```yaml
# Turn on lights when baby crying is detected
trigger:
  - platform: state
    entity_id: sensor.your_device_name_audio_event
    to: "baby_cry"
action:
  - service: light.turn_on
    target:
      entity_id: light.nursery
```

---

## Setup

1. Go to **Settings** → **Voice Config** → **Audio events**
2. Turn on **Audio event detection**
3. Set **Alert status display duration** (how long the sensor holds the event label)
4. Under **Listen for**, toggle the sound types to monitor
5. Under **Sensitivity**, choose the preset that fits the environment
6. The `audio_event` sensor appears in Home Assistant

---

## Use Cases

| Scenario | Recommended Types | Sensitivity |
|----------|------------------|-------------|
| Baby monitor — notify parents when baby cries | Baby crying | Catch more events |
| Fire safety — alert on smoke alarm tones | Alarm | Catch more events |
| Security — detect glass breakage | Glass breaking | Fewer false alerts |
| Doorbell notification while away | Doorbell | Balanced |
| Presence detection — know when someone is talking | Speech | Balanced |
| Emergency siren monitoring | Siren | Catch more events |
| Cough tracking for health logs | Cough | Balanced |

---

> **Note:** Audio event detection is a convenience feature, not a certified safety or medical monitoring system. Some low-end devices may produce false positives. Critical safety applications should always use dedicated certified detectors.

---

*Back to [Voice Control](Voice-Control)*
