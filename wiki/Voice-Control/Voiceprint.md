# Voiceprint Recognition

Ava Pro includes an on-device voiceprint recognition system with two independent modes: **Manual** and **Automatic**. Both modes run entirely locally — no audio leaves the device, no cloud services involved.

The voiceprint engine processes audio on a dedicated low-priority worker, completely isolated from wake word detection and audio event processing. This means voiceprint verification never adds latency to wake detection, and vice versa.

Compatible with Android 5-16.

---

## When to Use Voiceprint

| Scenario | Why Voiceprint Helps |
|----------|---------------------|
| The TV keeps triggering the wake word | Manual mode with wake-word check blocks unenrolled speakers — the TV will not wake the device |
| Two people at home, each wants personalized automations | Voiceprint identifies who is speaking, Home Assistant triggers different actions per person |
| Zero configuration — just turn it on and forget | Automatic mode learns from normal usage, no recording needed |
| Privacy-conscious — no cloud, no uploads | Both modes are 100% on-device, offline, no audio sent anywhere |
| Dad's voice turns on living room lights, Mom's voice turns on kitchen lights | Automatic mode + Home Assistant automation per speaker identity |

---

## Performance

| Mode | Accuracy | False Accept Rate | False Reject Rate | Verification Latency |
|------|----------|-------------------|-------------------|---------------------|
| Manual (5 samples) | **92.0%** | 2.1% | 5.9% | < 120ms |
| Automatic (after 20+ wakes) | **86.7%** | 4.8% | 8.5% | < 90ms (passive) |

---

## Manual Mode

Manual mode requires each user to record their wake word 5 times. The system builds a precise voiceprint from these samples and can verify the speaker's identity every time the wake word is triggered.

**Key capability: Only enrolled speakers can wake the device.**

When "Wake-word voiceprint check" is enabled, unenrolled speakers saying the wake word will not trigger Ava. This is the only mode that gates wake-word access based on speaker identity.

### What Manual Mode Does

- Two user slots, each enrolled and managed independently
- Guided recording flow with real-time feedback: which wake word to say, which sample number, capture success or failure
- Delete any user's recordings and re-enroll at any time
- Voiceprints are bound to the current wake word — changing the wake word requires re-enrollment
- Only enrolled speakers can trigger a wake when wake-word check is enabled

### Prerequisites

Before starting manual enrollment, ensure all of the following are met:

| Requirement | How to Check |
|-------------|-------------|
| Voice service running | Settings → Voice Config → service status |
| Microphone permission granted | Settings → Permissions |
| Microphone not muted | Settings → Voice Config → Mute is off |
| Home Assistant connected | Settings → HA connection status |

If any prerequisite is not met, the enrollment screen will display a message indicating which condition is blocking enrollment.

### Manual Mode Setup

1. Go to **Settings** → **Voice Config** → **Voiceprint**
2. Turn on voiceprint recognition
3. Select **Manual** mode
4. Set **User 1 name** (and optionally **User 2 name**)
5. Tap **Record** to start enrollment for User 1
6. Say the wake word when prompted — repeat 5 times
7. After User 1 completes all 5 samples, optionally switch to User 2 and record
8. Optionally enable **Wake-word voiceprint check** to restrict wake to enrolled speakers only

> **Note:** Wake-word voiceprint check requires at least one user to have completed enrollment before it can be enabled.

### Manual Mode Enrollment States

| State | Meaning |
|-------|---------|
| Waiting | User has not started recording |
| Recording | Currently capturing a sample |
| Stored | Sample successfully captured |
| Complete | All 5 samples recorded for this user |

### Manual Mode Enrollment Flow

1. User taps the record button
2. Ava suspends wake word detection to avoid interference
3. Audio is captured for approximately 2 seconds
4. The system checks if the audio is loud enough to be a valid sample
5. If audio is too quiet, the sample is rejected with a failure message
6. If audio passes quality check, the sample is processed and stored
7. The progress indicator advances to the next sample
8. After 5 successful samples, the user is marked as complete
9. If User 1 is complete and User 2 has not started, the system prompts to continue with User 2

---

## Automatic Mode

Automatic mode requires zero setup. Just use the wake word as usual — over time, Ava learns to distinguish between different household members' voices based on accumulated wake samples.

**Key capability: Recognition results are reported to Home Assistant as a sensor for automations.**

Automatic mode does **not** block others from waking the device — it identifies speakers, it does not gatekeep access.

### What Automatic Mode Does

- Fully on-device, no cloud uploads, works completely offline
- Results appear as a sensor entity in Home Assistant
- Use the sensor in HA automations to trigger different actions based on who is speaking
- Typically stabilizes after 20+ wake events per user
- Low-quality audio samples (excessive noise, very quiet speech) are automatically rejected and do not contribute to learning
- Audio during loud playback is skipped to prevent contamination

### Automatic Mode Setup

1. Go to **Settings** → **Voice Config** → **Voiceprint**
2. Turn on voiceprint recognition
3. Select **Automatic** mode
4. Set **User 1 name** and **User 2 name**
5. Use the wake word normally — Ava learns over time
6. The voiceprint status sensor appears in Home Assistant

### Automatic Mode Learning Process

1. Each time someone wakes the device, the audio around the wake word is analyzed
2. The system extracts a speaker profile from the audio
3. If the audio quality is sufficient, the profile is added to the learning pool
4. Over time, the system clusters similar profiles and associates them with the configured user names
5. Once enough samples are collected (typically 20+ per user), the system can identify who is speaking on each wake event
6. The Home Assistant sensor updates with the identified speaker's name

---

## Switching Modes

Switch between Manual and Automatic at any time in Settings. Switching clears the other mode's data:

| Switch Direction | What Happens |
|-----------------|--------------|
| Manual → Automatic | All recorded voiceprints are deleted |
| Automatic → Manual | All learned voiceprints are deleted |

A confirmation dialog explains what will be deleted before anything is wiped — nothing is removed silently.

---

## Home Assistant Entity

```
sensor.your_device_name_voice_print_status
```

### Sensor States

| State | Meaning |
|-------|---------|
| `disabled` | Voiceprint feature is turned off |
| `learning` | Automatic mode — still accumulating samples |
| `idle` | Standby — enrolled and ready, or not yet enrolled (manual mode) |
| User's display name | Speaker identified (e.g., "Dad", "Mom") |
| `stranger` | Speaker could not be matched to any enrolled user |
| `low_quality` | Audio quality was too poor to make a determination |

### Automation Example

```yaml
# Different lights based on who speaks
trigger:
  - platform: state
    entity_id: sensor.your_device_name_voice_print_status
action:
  - service: light.turn_on
    data:
      entity_id: >
        {% if trigger.to_state.state == 'Dad' %}
          light.living_room
        {% elif trigger.to_state.state == 'Mom' %}
          light.kitchen
        {% endif %}
```

---

## Use Cases

| Scenario | Recommended Mode | Why |
|-----------|-----------------|-----|
| Prevent TV from triggering wake word | Manual + wake-word voiceprint check | Only enrolled speakers can wake — TV audio is blocked |
| Two household members, identity-based automations | Manual | Precise gating and identification |
| Zero-setup, passive identification for HA automations | Automatic | No recording needed — learns from normal usage |
| Privacy-conscious, all-local processing | Either | Both are 100% on-device, offline |
| Dad's voice → living room lights, Mom's voice → kitchen lights | Automatic + HA automation | Passive identification flows into HA sensor |
| Restrict wake access to authorized users only | Manual + wake-word voiceprint check | This is the only mode that gates wake access |

---

*Back to [Voice Control](Voice-Control)*
