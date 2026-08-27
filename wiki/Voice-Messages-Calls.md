# Voice Messages & Calls

Ava turns multiple devices into a simple voice network inside your home. Send a quick voice message to another room, keep messages on a message board, start a live voice or video call between Ava devices on the same local network, or drive all of this from Home Assistant automations.

---

## Overview

**Features:**

- **Voice Messages** — Record and send short messages to one or more Ava devices
- **Message Board** — Keep received messages on screen and replay them later
- **Live Calls** — Room-to-room voice conversation over LAN
- **Video Calls** — Optional video during live calls (quality configurable in settings)
- **Home Assistant** — `voice_action` service for automations; diagnostic entities for monitoring
- **Local Network First** — Designed for LAN use, without a cloud account or external relay

---

## How It Works

Ava uses NSD (Network Service Discovery) to find other Ava devices on the same LAN. Voice data is transmitted directly between devices using a custom protocol with Opus codec compression. Video during calls uses chunked JPEG over a separate LAN port.

```
┌──────────────┐                    ┌──────────────┐
│  Ava Device A │  ── LAN ──>  │  Ava Device B │
│  (Kitchen)    │  Voice/Video  │  (Living Room) │
└──────────────┘                    └──────────────┘
```

Each device advertises a **sender display name** (configurable). Other devices see that name in the nearby list and in incoming message/call UI.

---

## Setup

### Enable sending

1. Go to **Settings → Interaction → Voice Messages & Calls**
2. Turn on **Enable send shortcut** (shows the voice button in the sidebar)
3. Set **Sender display name** (shown on other devices; leave blank to use the device name)

### Enable receiving

1. Turn on **Receive voice messages**
2. Choose **Display after receipt** — dismiss after playback, or keep on message board
3. For calls: configure **Answer incoming calls** (manual answer vs auto-connect) and **Incoming call ringtone**

### Expose to Home Assistant

1. Turn on **Expose to Home Assistant** (same settings page)
2. Reload the ESPHome integration in Home Assistant after Ava updates
3. The initiating Ava device must be the ESPHome node you call `voice_action` on

---

## Voice Messages

### Sending from the app

1. Open the Voice overlay (sidebar button)
2. Select one or more target devices from the nearby list
3. Switch to **Message** mode if needed
4. Press and hold the large button to record
5. Release to send

### Receive modes

| Mode | Description |
|------|-------------|
| Dismiss after playback | Play once and close automatically |
| Keep on message board | Stay visible until dismissed; replay available |

### Message delivery delay

Set **Delivery delay** (minutes) before the receiver gets the voice message. `0` means immediate.

Configured in the app or via the HA entity `number.<device>_voice_message_delay_minutes` (0–1440).

**Use case:** Leave a delayed reminder for a child's room — the message stays available for replay.

---

## Voice Calls

### Starting a call from the app

1. Open the Voice overlay
2. Switch to **Call** mode
3. Select target device(s)
4. Press and hold to talk live; release to listen (push-to-talk)

### Video calls

During a live call, tap **Video** to enable local camera streaming. Video quality is set under **Settings → Interaction → Video call quality** (Smooth / HD / Ultra HD).

When started from Home Assistant with `mode: video_call`, video is enabled automatically after the call connects.

### Incoming calls

When someone calls your device:

- A ringtone plays if **Answer incoming calls** is enabled
- Tap **Answer** to connect or **Decline** to reject
- If **Answer incoming calls** is off, calls connect automatically
- After about 1 minute with no answer, the caller is notified the device is unavailable

### Call status

| Status | Description |
|--------|-------------|
| Ringing | Calling the other device |
| Connected | Live voice channel active |
| No answer | The other device did not answer in time |
| Declined | The other device declined |
| Call ended | The other party hung up |

---

## Settings reference

**Settings → Interaction → Voice Messages & Calls**

| Setting | Description | Default |
|---------|-------------|---------|
| Enable send shortcut | Show voice button in sidebar | Off |
| Receive voice messages | Accept messages from other Ava devices | Off |
| Display after receipt | Auto-dismiss or message board | Dismiss after playback |
| Sender display name | Name shown to recipients | Device name |
| Delivery delay | Minutes before delivery (0 = immediate) | 0 |
| Answer incoming calls | Require tap to answer calls | Off |
| Incoming call ringtone | Ringtone for incoming calls | Default |
| Video call quality | Resolution/FPS for video calls | Smooth (480p) |
| Expose to Home Assistant | Expose entities and `voice_action` service | Off |

---

## Home Assistant integration

### Prerequisites

| Requirement | Why |
|-------------|-----|
| Ava connected via **ESPHome** integration | Services and entities are exposed through ESPHome protocol |
| **Enable send shortcut** on the initiating device | HA calls open the sender UI on that device |
| **Expose to Home Assistant** on the initiating device | Registers `voice_action` and related entities |
| Target devices on the **same LAN** with **Receive voice messages** on | Discovery and delivery |
| Reload ESPHome after Ava updates | Picks up new service schema |

### Service name

```
esphome.<initiator_node_name>_voice_action
```

Example: if your kitchen tablet's ESPHome node is `kitchen`:

```
esphome.kitchen_voice_action
```

Find the exact name under **Developer tools → Services** (search `voice_action`).

> **Important:** The service runs on the **initiating** Ava device — the one that shows the sender overlay and places the call. `targets` are the **other** Ava devices to reach.

### Exposed entities

When **Expose to Home Assistant** is enabled:

| Entity | Object ID | Purpose |
|--------|-----------|---------|
| Voice Messages | `voice_message_display` | Switch — show/hide the sender overlay |
| Voice message delay | `voice_message_delay_minutes` | Number — delivery delay (0–1440 min) |
| Nearby devices | `voice_target` | **Diagnostic, view only** — lists discovered LAN devices |
| *(service)* | `voice_action` | Start calls/messages, hang up, redial |

Entity IDs follow the pattern `select.<node>_voice_target`, `switch.<node>_voice_message_display`, etc.

### Nearby devices entity (view only)

The **Nearby devices** select entity shows which Ava devices are currently visible on the LAN. Use it to **check device names** when writing automations.

| Behavior | Detail |
|----------|--------|
| Updates automatically | Reflects live LAN discovery |
| **Not a call target** | Changing the select in HA has **no effect** on `voice_action` |
| State display | One device shows its name; multiple devices show a comma-separated list |
| Write ignored | The entity is diagnostic — always set `targets` in your automation |

### `voice_action` service reference

**Parameters (3 fields only):**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `action` | Yes | `start` \| `hang_up` \| `redial` |
| `mode` | No | Only for `start`. Default: `call` |
| `targets` | No | String array of device names. Omit to reach **all** nearby devices |

**`action` values:**

| Value | Aliases | Effect |
|-------|---------|--------|
| `start` | — | Begin a voice session (opens sender UI on initiating device) |
| `hang_up` | `hangup` | End the current HA-initiated session |
| `redial` | `redial_call` | Repeat the last `start` (same mode and targets) |

**`mode` values (only when `action: start`):**

| Value | Aliases | Effect |
|-------|---------|--------|
| `call` | — | Live voice call (default) |
| `video_call` | `video` | Live call; video enabled automatically after connect |
| `message` | `intercom` | Voice message; auto-records ~10 seconds on initiating device |

**`targets` resolution:**

| Input | Result |
|-------|--------|
| `targets: [bedroom]` | Call/message `bedroom` only |
| `targets: [bedroom, hallway]` | Multiple devices (simultaneous) |
| `targets` omitted or empty | **All** currently discovered nearby devices |
| Name matching | Exact name or ID match first; partial name match as fallback |

**Not in service parameters (use app / entities instead):**

| Setting | Where |
|---------|-------|
| Message record length | Fixed ~10 s for HA-initiated messages |
| Delivery delay | App **Delivery delay** or `number.<node>_voice_message_delay_minutes` |

### What happens on the initiating device

When Home Assistant calls `voice_action` with `action: start`:

1. Ava ensures voice send/receive is enabled and refreshes LAN discovery (~2 s if list was empty)
2. The **sender overlay opens** on the initiating device (not headless)
3. Targets from `targets` are pre-selected; mode is applied automatically
4. **Call / video_call:** Push-to-talk UI; user holds button to speak
5. **Message:** Recording starts automatically (~10 s), then sends with configured delivery delay
6. **video_call:** Video turns on automatically after the callee answers
7. Closing the overlay or calling `hang_up` tears down the session on both sides

Only **one** HA voice session per initiating device at a time.

---

## Home Assistant automations

### Create an automation (UI)

1. **Settings → Automations & scenes → Create automation**
2. Add a **trigger** (button, motion, time, etc.)
3. Add action **Call service**
4. Service: `esphome.<your_node>_voice_action`
5. Service data (switch to YAML mode if needed):

```yaml
action: start
mode: call
targets:
  - bedroom
```

6. Save and enable

### Basic examples

**Single-device voice call**

```yaml
alias: Call bedroom
triggers:
  - platform: state
    entity_id: input_button.call_bedroom
    to: pressed
actions:
  - service: esphome.kitchen_voice_action
    data:
      action: start
      mode: call
      targets:
        - bedroom
mode: single
```

**Multi-target call (several rooms)**

```yaml
alias: Announce dinner
triggers:
  - platform: time
    at: "18:30:00"
actions:
  - service: esphome.kitchen_voice_action
    data:
      action: start
      mode: call
      targets:
        - living_room
        - bedroom
        - hallway
mode: single
```

**Broadcast to all nearby devices**

Omit `targets` to reach every Ava device currently visible on the LAN from the initiating device:

```yaml
alias: Nearby broadcast
triggers:
  - platform: state
    entity_id: input_button.broadcast_nearby
    to: pressed
actions:
  - service: esphome.kitchen_voice_action
    data:
      action: start
      mode: call
      # no targets → all discovered nearby devices
mode: single
```

**Leave a voice message**

```yaml
alias: Door motion message
triggers:
  - platform: state
    entity_id: binary_sensor.front_door_motion
    to: "on"
actions:
  - service: esphome.hallway_voice_action
    data:
      action: start
      mode: message
      targets:
        - bedroom
mode: single
```

**Video call**

```yaml
actions:
  - service: esphome.kitchen_voice_action
    data:
      action: start
      mode: video_call
      targets:
        - front_door
```

**Hang up**

```yaml
actions:
  - service: esphome.kitchen_voice_action
    data:
      action: hang_up
```

No `mode` or `targets` needed.

**Redial last session**

```yaml
actions:
  - service: esphome.kitchen_voice_action
    data:
      action: redial
```

Repeats the previous `start` call (mode + targets). Fails silently if no prior session or targets are offline.

---

### Complex automation patterns

#### 1. Doorbell → video call → auto hang-up

Ring the door panel, give time to talk, then end the call:

```yaml
alias: Doorbell video intercom
triggers:
  - platform: state
    entity_id: binary_sensor.doorbell
    to: "on"
actions:
  - service: esphome.hallway_voice_action
    data:
      action: start
      mode: video_call
      targets:
        - door_panel
  - delay:
      minutes: 3
  - service: esphome.hallway_voice_action
    data:
      action: hang_up
mode: single
```

#### 2. Motion → message only if someone is home

```yaml
alias: Away motion alert message
triggers:
  - platform: state
    entity_id: binary_sensor.garage_motion
    to: "on"
conditions:
  - condition: state
    entity_id: person.family
    state: home
actions:
  - service: esphome.garage_voice_action
    data:
      action: start
      mode: message
      targets:
        - kitchen
        - bedroom
mode: single
```

#### 3. Different targets by time of day

```yaml
alias: Night call vs day message
triggers:
  - platform: state
    entity_id: input_button.help_button
    to: pressed
actions:
  - choose:
      - conditions:
          - condition: time
            after: "22:00:00"
            before: "07:00:00"
        sequence:
          - service: esphome.bedroom_voice_action
            data:
              action: start
              mode: message
              targets:
                - parents_room
      - conditions: []
        sequence:
          - service: esphome.bedroom_voice_action
            data:
              action: start
              mode: call
              targets:
                - parents_room
mode: single
```

#### 4. Escalation: message first, then call if still occupied

```yaml
alias: Bathroom occupancy escalation
triggers:
  - platform: state
    entity_id: binary_sensor.bathroom_occupied
    to: "on"
    for:
      minutes: 15
actions:
  - service: esphome.bathroom_voice_action
    data:
      action: start
      mode: message
      targets:
        - hallway
  - delay:
      minutes: 2
  - condition: state
    entity_id: binary_sensor.bathroom_occupied
    state: "on"
  - service: esphome.bathroom_voice_action
    data:
      action: start
      mode: call
      targets:
        - hallway
mode: single
```

#### 5. Script with parameters (reusable caller)

Define a script once, pass target and mode from any automation:

```yaml
# scripts/ava_voice_notify.yaml
ava_voice_notify:
  alias: Ava voice notify
  fields:
    targets:
      description: Device name or list of names
      example: bedroom
    mode:
      description: call | video_call | message
      default: message
  sequence:
    - service: esphome.kitchen_voice_action
      data:
        action: start
        mode: "{{ mode }}"
        targets: "{{ targets if targets is iterable and targets is not string else [targets] }}"
```

Call from an automation:

```yaml
actions:
  - service: script.ava_voice_notify
    data:
      targets:
        - bedroom
        - kids_room
      mode: message
```

#### 6. Dashboard button row: call / hang up / redial

Use three `input_button` helpers:

```yaml
# automation: call
triggers:
  - platform: state
    entity_id: input_button.voice_call_bedroom
    to: pressed
actions:
  - service: esphome.kitchen_voice_action
    data:
      action: start
      mode: call
      targets: [bedroom]

---
# automation: hang up
triggers:
  - platform: state
    entity_id: input_button.voice_hang_up
    to: pressed
actions:
  - service: esphome.kitchen_voice_action
    data:
      action: hang_up

---
# automation: redial
triggers:
  - platform: state
    entity_id: input_button.voice_redial
    to: pressed
actions:
  - service: esphome.kitchen_voice_action
    data:
      action: redial
```

#### 7. Show overlay, then start call from same device

Useful when you want the UI visible before the automation fires:

```yaml
actions:
  - service: switch.turn_on
    target:
      entity_id: switch.kitchen_voice_message_display
  - delay:
      seconds: 1
  - service: esphome.kitchen_voice_action
    data:
      action: start
      mode: call
      targets:
        - living_room
```

#### 8. Set delay remotely, then send delayed message

```yaml
actions:
  - service: number.set_value
    target:
      entity_id: number.kitchen_voice_message_delay_minutes
    data:
      value: 30
  - service: esphome.kitchen_voice_action
    data:
      action: start
      mode: message
      targets:
        - kids_room
  # Optional: reset delay afterward
  - delay:
      seconds: 5
  - service: number.set_value
    target:
      entity_id: number.kitchen_voice_message_delay_minutes
    data:
      value: 0
```

#### 9. Guard: only call if targets are visible

Check the diagnostic entity state contains the target name (optional safety):

```yaml
conditions:
  - condition: template
    value_template: >
      {{ 'bedroom' in states('select.kitchen_voice_target') }}
actions:
  - service: esphome.kitchen_voice_action
    data:
      action: start
      mode: call
      targets:
        - bedroom
```

> This is optional. `voice_action` already skips unresolved names; the condition avoids opening the sender UI when nobody is nearby.

#### 10. Parallel rooms from multiple initiators

Each Ava device has its own `voice_action` service. Automations can run in parallel on different nodes:

```yaml
# Kitchen calls living room
- service: esphome.kitchen_voice_action
  data:
    action: start
    mode: call
    targets: [living_room]

# Bedroom calls hallway (independent session)
- service: esphome.bedroom_voice_action
  data:
    action: start
    mode: call
    targets: [hallway]
```

Each initiating device manages its own session and overlay.

---

### Legacy overlay control

These entities still work alongside `voice_action`:

```yaml
# Show sender overlay
service: switch.turn_on
target:
  entity_id: switch.your_device_voice_message_display

# Hide sender overlay
service: switch.turn_off
target:
  entity_id: switch.your_device_voice_message_display

# Set message delivery delay (minutes)
service: number.set_value
target:
  entity_id: number.your_device_voice_message_delay_minutes
data:
  value: 5
```

---

## Home use cases

| Scenario | Approach |
|----------|----------|
| Kitchen → living room | Sidebar message or `mode: message` automation |
| Dinner announcement | `targets: [room1, room2, …]` or omit for broadcast |
| Doorbell intercom | `mode: video_call` + optional `hang_up` delay |
| Delayed child reminder | Set `voice_message_delay_minutes`, then `mode: message` |
| Bedroom → hallway panel | `mode: call` from initiating room's `voice_action` |
| Missed call retry | Dashboard **redial** button (`action: redial`) |

---

## FAQ

### Cannot find nearby Ava devices?

1. Ensure all devices are on the same LAN
2. Check firewall settings (voice uses UDP ports 19848–19850)
3. Ensure **Receive voice messages** is enabled on target devices
4. Check **Nearby devices** entity in HA for live names
5. Restart the voice satellite service on both devices if needed

### Voice message not received?

1. Check **Receive voice messages** on the target device
2. Check network connectivity
3. Microphone may be busy (voice assistant active on sender)

### Voice call doesn't connect?

1. Both devices must be on the same LAN
2. Callee may require manual answer (**Answer incoming calls** enabled)
3. No answer within ~1 minute ends the ring

### `voice_action` does nothing?

1. Confirm **Expose to Home Assistant** is on
2. Reload ESPHome integration
3. Use **Nearby devices** to verify target names
4. Initiating device must have **Enable send shortcut** on
5. Check HA logs and Ava logcat (`AvaVoiceHa`)

### Does the Nearby devices select set my call target?

**No.** It is diagnostic only. Always set `targets` in your automation (or omit `targets` to broadcast to all nearby).

### Can I run headless without the sender UI?

No. HA-initiated sessions always open the sender overlay on the initiating device so you can speak or confirm the message.

### Microphone busy error?

The voice assistant is active. Finish that session first. The microphone cannot be shared between the voice assistant and voice messages/calls simultaneously.

### HA schema still shows old parameters?

Reload the ESPHome integration. Current `voice_action` exposes only `action`, `mode`, and `targets`.

---

*Back to [Home](Home)*
