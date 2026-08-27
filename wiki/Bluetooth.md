# Bluetooth

Ava turns an Android device into a full Bluetooth gateway for Home Assistant — presence detection, BLE proxy, and room-level triangulation, all without an ESP32.

Compatible with Android 5-16.

---

## Why Ava's Bluetooth Is Different

Most Bluetooth-to-Home-Assistant solutions require dedicated ESP32 nodes or USB dongles. Ava uses the Android device's built-in Bluetooth chip, which is already there, already powered, and already connected to WiFi.

- **Native ESPHome Bluetooth proxy** — HA sees Ava as a native proxy, no custom integration needed
- **BTHome advertisement forwarding** — BLE advertisements from BTHome devices (thermometers, plant sensors, scales, etc.) are forwarded to HA in real time with deduplication
- **Multi-device claim coordination** — when multiple Ava devices are on the same network, they coordinate to avoid double-claiming the same BLE device
- **Chip-aware scanning** — Ava detects the Bluetooth chip at runtime and applies vendor-specific workarounds for known issues
- **Watchdog with self-healing** — if BLE advertisements stop arriving, Ava detects the stall and automatically restarts scanning
- **Wake word arbitration** — when multiple Ava devices hear the same wake word, they arbitrate so only one device responds
- **Low-end device support** — even Echo Show devices and cheap tablets with broken BLE chips can run in compatibility mode with root

> **Note:** The Bluetooth proxy feature is not open source and is only available in release builds.

---

## Feature Overview

### Bluetooth Presence Detection

Detect if phones, wearables, and other Bluetooth devices are nearby. Ava scans for tracked devices at regular intervals and reports presence/absence to Home Assistant.

- Track up to 5 devices by MAC address
- RSSI threshold filtering (ignore weak signals)
- Configurable away delay (prevent flicker when device briefly goes out of range)
- Bonded device support (automatically picks up system-paired devices)
- Real-time presence state in HA

### Bluetooth Proxy (Key Feature)

Ava acts as a BLE proxy, forwarding all Bluetooth Low Energy advertisements to Home Assistant as if HA had a direct Bluetooth connection.

- Forwards all BLE advertisements to HA (not just tracked devices)
- Native HA Bluetooth integration — auto-discovered, zero config
- BTHome device detection with automatic slot allocation
- Active and passive scan modes
- Adjustable scan power (High / Balanced / Low)
- Advertisement deduplication (prevents duplicate data flooding HA)
- Scan session rotation (prevents Android BLE stack from going stale)

### BLE Advertising

Ava can advertise itself as a BLE device, broadcasting its presence to nearby Bluetooth scanners. This is useful for:

- Bermuda room presence triangulation
- Detecting which room Ava is in
- Multi-device proximity sensing

---

## How It Works

### Presence Detection

Ava runs a continuous scan loop:

1. Scans for BLE devices at a fixed interval (6 seconds on normal devices, 30 seconds on low-end chips)
2. For each tracked device found, records the strongest RSSI seen during the scan cycle
3. If RSSI exceeds the threshold, marks the device as "present" and resets the away timer
4. If a tracked device is not seen for the configured away delay, marks it as "away"
5. Presence state is published to Home Assistant as a binary sensor

**Presence logic:**

| Status | Condition |
|--------|-----------|
| Present | Device detected with RSSI above threshold |
| Away | Device not detected for the configured away delay |

When the RSSI threshold or away delay is changed in settings, Ava immediately re-evaluates all tracked devices — no need to wait for the next scan cycle.

### BLE Proxy

The proxy scan runs independently from presence detection:

1. Ava continuously scans for BLE devices and forwards all advertisements to HA
2. A watchdog monitors the advertisement stream — if no data arrives within a stale threshold, scanning is automatically restarted
3. Scan sessions are periodically rotated to keep the BLE stack healthy
4. Duplicate advertisements are filtered to prevent data flooding
5. BTHome devices are automatically detected and allocated to connection slots

### Multi-Device Claim Coordination

When multiple Ava devices are on the same LAN, they coordinate BLE device claims:

- Each Ava device has a unique ID
- When a device claims a BLE peripheral, it notifies other Ava devices on the network
- Other Ava devices respect existing claims and do not attempt to connect to the same peripheral
- Up to 5 BLE peripherals can be claimed per Ava device
- This prevents duplicate data and connection conflicts when deploying multiple Ava devices in the same area

### Wake Word Arbitration

When multiple Ava devices are within earshot, they arbitrate wake word events:

1. Each Ava device generates a unique random ID at startup
2. When a wake word is detected, the device broadcasts an arbitration signal
3. All Ava devices that heard the same wake word compare IDs
4. The device with the lowest ID wins and responds; others stay silent
5. Arbitration completes within 80ms — no perceptible delay
6. If arbitration fails (network error, no response), the device responds as fallback

This means placing 2-3 Ava devices in different rooms will not cause all of them to respond to a single wake word — only the closest one (or the one that wins the arbitration) responds.

---

## Settings

Go to **Settings** -> **Bluetooth**

### Presence Detection Settings

| Setting | Description | Default |
|---------|-------------|---------|
| Device Detection | Turn on Bluetooth presence detection | Off |
| RSSI Threshold | Signal strength threshold (left = near, right = far) | -80 dBm |
| Away Delay | Seconds before marking as away (min: scan interval + 1) | 120s |
| Add Bluetooth Device | Add devices to track by MAC address | — |

### Presence Alert Sound

Each tracked Bluetooth device can have a custom alert sound that plays when the device's presence state changes. This is useful for notifications like "phone arrived home" or "watch left range".

**Setup:** Tap the hollow bell icon next to a tracked device to open the alert sound picker.

**Trigger options:**

| Trigger | Description |
|---------|-------------|
| Nearby | Play sound when the device comes into range (was away → present) |
| Not nearby | Play sound when the device goes out of range (was present → away) |

**Sound sources:**

- **System ringtones:** All system notification sounds are listed in the picker
- **External audio:** Select any audio file from device storage via the system file picker (`+ Select External Audio`)
- **Custom sounds:** Up to 24 custom audio URIs are persisted across sessions; previously used custom sounds appear in the picker list
- **None:** No alert sound (default)

**Behavior:**

- When the trigger condition is met, the selected sound is played through the TTS audio player
- A toast notification is shown with the device name and state (e.g., "iPhone is nearby" / "iPhone is not nearby")
- Alert sound and trigger are stored per-device in the tracked device data
- Custom URIs require persistable URI permission (granted automatically on file selection)

**i18n:** All alert-related UI text and toast messages are localized in 6 languages (English, Chinese, German, Russian, Portuguese, Vietnamese).

### Bluetooth Proxy Settings

| Setting | Description | Default |
|---------|-------------|---------|
| BLE Gateway / Mesh | Turn on Bluetooth proxy | Off |
| Scan Mode | Active or Passive scanning | Active |
| Scan Power | High Performance, Balanced, or Low Power | High |

### Scan Mode

| Mode | Description |
|------|-------------|
| Active Scan | Actively request scan responses — more data, higher power consumption |
| Passive Scan | Listen only — lower power, may miss some advertisement types |

### Scan Power

| Power | Description | Use When |
|-------|-------------|----------|
| High Performance | Maximum scan frequency, lowest latency | Device is plugged in, need real-time data |
| Balanced | Moderate scan frequency | Typical use, battery-powered |
| Low Power | Reduced scan frequency | Battery conservation is priority |

---

## Capability Tiers

Ava detects the device's BLE capabilities at runtime and classifies it into a tier. This is not based on the Bluetooth version number reported by apps — it is based on what the Android BLE stack actually supports at the API level.

| Tier | Description | Typical Devices |
|------|-------------|-----------------|
| Full | All BLE features available, maximum connections | Modern phones (Pixel, Samsung flagship), Android 8+ |
| Balanced | Most features work, some limitations | Mid-range devices, older flagships |
| Compatibility | Limited mode, reduced scan rate, may need root | Echo Show, cheap tablets, low-end chips |

### Capability Detection

| Capability | What It Means |
|------------|---------------|
| Offloaded Filtering | Hardware can filter scan results — saves CPU, enables targeted scanning |
| Multiple Advertisement | Can broadcast multiple BLE ad slots simultaneously |
| Extended Advertising | BLE 5.0 extended advertising — larger payload, longer range |
| LE Coded PHY | Long-range coded PHY — up to 4x range in open space |
| LE 2M PHY | 2M PHY — high-speed data transfer |

Tier assignment:
- **Full**: Offloaded filtering + Multiple advertisement + at least one advanced PHY
- **Balanced**: At least 2 basic capabilities, or any advanced BLE 5 signal
- **Compatibility**: Everything else

> Some devices report BLE 5.0 in specs but do not expose all features through Android APIs. Ava tests at runtime, not by reading spec sheets.

---

## Low-End BLE Compatibility

Devices with limited BLE chips (Echo Show, cheap tablets, some MediaTek/Spreadtrum/Allwinner devices) run in compatibility mode:

- Scan interval is increased to 30 seconds (vs 6 seconds on normal devices) to maintain WiFi stability
- Bermuda may show skull or exclamation icons — this is expected, not a bug
- Bluetooth media playback is not supported
- Root access is typically required for stable operation
- Built-in Bluetooth presence detection may have reduced accuracy
- Ava can auto-grant location permission via root on pre-Android 12 devices with low-end BLE chips (Android requires location permission for BLE scanning)

### Chip-Specific Behavior

Ava detects the Bluetooth chip at runtime and applies appropriate behavior:

| Vendor | Known Issues | Ava's Response |
|--------|-------------|----------------|
| MediaTek | Scan stalls after several minutes | Watchdog detects stall, restarts scan |
| Spreadtrum | Scan filters unreliable, RSSI inaccurate | Falls back to unfiltered scanning |
| Allwinner | Passive scan broken, batch scan broken | Forces active scan, disables batching |
| Qualcomm | Generally stable | Standard scan mode |
| Samsung (Exynos) | Generally stable | Standard scan mode |

### Watchdog Self-Healing

On non-low-end devices, Ava runs a watchdog that monitors the BLE advertisement stream:

1. If no advertisements arrive within a stale threshold, the scan is considered stalled
2. Ava forces a clean restart of the Bluetooth stack
3. After Bluetooth recovers (typically 5 seconds), scanning resumes automatically
4. Scan sessions are also periodically rotated to keep the BLE stack healthy

This means Ava's Bluetooth proxy is self-maintaining — it does not require manual intervention or app restarts to recover from BLE stack issues.

---

## Permission Requirements

| Permission | Android Version | Description |
|------------|-----------------|-------------|
| BLUETOOTH_SCAN | 12+ | BLE scanning |
| BLUETOOTH_CONNECT | 12+ | Connect to BLE devices |
| BLUETOOTH | 5-11 | Basic Bluetooth access |
| ACCESS_FINE_LOCATION | All | Required by Android for BLE scanning |

> On pre-Android 12 devices with low-end BLE chips, Ava can auto-grant location permission via root if it is missing. Android requires location permission for BLE scanning even when the app does not use location data.

---

## Home Assistant Integration

### Presence Entity

```
binary_sensor.your_device_name_bluetooth_presence
```

### Bluetooth Proxy Integration

Bluetooth proxy natively integrates with Home Assistant's Bluetooth integration:

1. Enable BLE Gateway in Ava Settings -> Bluetooth
2. Add the Bluetooth integration in HA (Settings -> Devices & Services -> Add Integration -> Bluetooth)
3. Ava will be automatically discovered as a Bluetooth proxy
4. BLE devices in range of Ava will appear in HA as if connected to a local Bluetooth dongle

### Bermuda Integration

Ava works with the [Bermuda](https://github.com/agittins/bermuda) integration for Bluetooth triangulation and room presence. Ava broadcasts BLE advertisements that Bermuda can use to estimate which room the device is in.

**Setup:**
1. Install Bermuda from HACS
2. Ava's BLE advertisements will be picked up by Bermuda
3. Configure Bermuda to use Ava devices as beacons
4. Room presence sensors will appear in HA

**Supported BLE Device Types (via proxy):**
- Bluetooth thermometers/hygrometers (Xiaomi, Govee, etc.)
- Bluetooth scales (Mi Scale, etc.)
- Bluetooth plant sensors (Xiaomi Mi Flora, etc.)
- Bluetooth smart locks
- BTHome devices (buttons, sensors, etc.)
- Any BLE device supported by HA's Bluetooth integration

---

## Scenarios

### Presence Detection

**Arrival — turn on lights and climate:**
```yaml
automation:
  - alias: "Arrival Home"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_bluetooth_presence
        to: "on"
    action:
      - service: light.turn_on
        target:
          entity_id: light.living_room
      - service: climate.set_temperature
        target:
          entity_id: climate.living_room
        data:
          temperature: 22
```

**Departure — away mode:**
```yaml
automation:
  - alias: "Departure - Away Mode"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_bluetooth_presence
        to: "off"
        for:
          minutes: 5
    action:
      - service: light.turn_off
        target:
          entity_id: group.all_lights
      - service: climate.set_temperature
        target:
          entity_id: climate.living_room
        data:
          temperature: 16
      - service: media_player.turn_off
        target:
          entity_id: media_player.living_room
```

**Multi-device presence — track family members:**

Track multiple phones by adding each as a separate tracked device. Each gets its own presence state:

```yaml
automation:
  - alias: "Everyone Left"
    trigger:
      - platform: template
        value_template: >
          {{ is_state('binary_sensor.ava_bluetooth_presence', 'off') }}
        for:
          minutes: 10
    condition:
      - condition: state
        entity_id: binary_sensor.ava_bedroom_bluetooth_presence
        state: "off"
        for:
          minutes: 10
    action:
      - service: alarm_control_panel.alarm_arm_away
        target:
          entity_id: alarm_control_panel.home
```

### Room Presence with Bermuda

**Turn on devices when entering a room:**
```yaml
automation:
  - alias: "Entered Kitchen"
    trigger:
      - platform: state
        entity_id: sensor.phone_room_presence
        to: "kitchen"
        for:
          seconds: 5
    action:
      - service: light.turn_on
        target:
          entity_id: light.kitchen
```

**Follow-me audio — play music in the room you are in:**
```yaml
automation:
  - alias: "Follow Me Music"
    trigger:
      - platform: state
        entity_id: sensor.phone_room_presence
    action:
      - service: media_player.join
        target:
          entity_id: >
            {% set room = states('sensor.phone_room_presence') %}
            {% if room == 'living_room' %} media_player.living_room
            {% elif room == 'kitchen' %} media_player.kitchen
            {% elif room == 'bedroom' %} media_player.bedroom
            {% endif %}
```

### BLE Proxy — Sensor Data

**Auto-discovered sensors:**

When BLE devices are in range of Ava's proxy, they appear in HA automatically. No automation needed — just use the sensor data:

```yaml
automation:
  - alias: "Plant Needs Water"
    trigger:
      - platform: numeric_state
        entity_id: sensor.plant_sensor_moisture
        below: 20
    action:
      - service: notify.mobile_app
        data:
          message: "The plant needs water (moisture: {{ states('sensor.plant_sensor_moisture') }}%)"
```

**Temperature log:**
```yaml
automation:
  - alias: "Log Room Temperature"
    trigger:
      - platform: time_pattern
        minutes: "/30"
    action:
      - service: input_number.set_value
        target:
          entity_id: input_number.living_room_temp_log
        data:
          value: "{{ states('sensor.ble_thermometer_temperature') }}"
```

### Multi-Device Deployment

**Place Ava devices in different rooms:**

Each Ava device acts as an independent BLE proxy. HA will see all BLE devices from all proxies. With Bermuda, you can determine which room a BLE device is closest to:

```yaml
automation:
  - alias: "Door Sensor in Kitchen"
    trigger:
      - platform: state
        entity_id: binary_sensor.ble_door_sensor
        to: "on"
    condition:
      - condition: state
        entity_id: sensor.ble_door_sensor_room
        state: "kitchen"
    action:
      - service: notify.mobile_app
        data:
          message: "Kitchen door opened"
```

### Wake Word Arbitration

No automation needed — this works automatically when multiple Ava devices are on the same network. Only the winning device responds to a wake word. This prevents the "all devices respond" problem common with multiple smart speakers.

---

## Device Recommendations

### Best Experience (Full Tier)

- Google Pixel 4a+
- Samsung Galaxy S10+ / Note 10+
- Xiaomi Mi 9+
- Any device with Qualcomm Snapdragon 700+ series

### Good Experience (Balanced Tier)

- Samsung Galaxy A series
- Xiaomi Redmi Note series
- Most mid-range Android 8+ devices

### Supported but Limited (Compatibility Tier)

- Amazon Echo Show (all generations)
- Cheap Android tablets (Lenovo, Amazon Fire, etc.)
- Devices with MediaTek/Spreadtrum/Allwinner chips
- May require root for stable BLE proxy operation

### Not Recommended

- Devices without Bluetooth LE (very old Android 5 devices)
- Devices with broken Bluetooth stacks that crash on BLE scan

---

## FAQ

### Bluetooth detection inaccurate?

1. Ensure the target device's Bluetooth is on
2. Check if the MAC address is correct
3. Adjust RSSI threshold — move slider left for near (stricter), right for far (looser)
4. Increase away delay to prevent flicker
5. Ensure location permission is granted (Android requires it for BLE scanning)
6. Try changing scan power to High Performance
7. Some devices reduce Bluetooth visibility in sleep mode — check the target device's battery settings

### High battery usage?

1. Change scan power to Low Power or Balanced
2. Increase away delay
3. Only enable detection when needed (e.g., disable at night)
4. If running proxy + presence simultaneously, consider using Balanced power

### Cannot scan device?

1. Check Bluetooth permissions (Settings -> Permissions)
2. Check location permission (required by Android for BLE)
3. Ensure the target device is discoverable (not hidden)
4. Check if the device is in compatibility mode — low-end chips may need root
5. Try resetting Bluetooth: `adb shell am broadcast -a com.example.ava.ACTION_GRANT_BLUETOOTH`

### Bermuda shows skull icons?

This is expected in compatibility mode. The device has limited BLE capabilities. The skull icon means Bermuda is receiving data but the signal quality is reduced. For full functionality, use a device with complete BLE proxy support (Full tier).

### Bluetooth proxy not working?

1. Check if BLE Gateway is enabled in Settings -> Bluetooth
2. Check Bluetooth and location permissions
3. Check capability tier in Settings -> Bluetooth — if Compatibility, root may be needed
4. Restart the Ava service
5. If scanning stalls, the watchdog should self-heal within 30 seconds — if not, restart the app

### Multiple Ava devices all respond to wake word?

Wake word arbitration is enabled by default. Check:
1. All Ava devices are on the same WiFi network
2. Multi-Device Arbitration is enabled in Settings -> Voice
3. UDP port 19847 is not blocked by the router/firewall

### Bluetooth proxy stops after a while?

Ava has a built-in watchdog that detects stalled scans and restarts them. If the problem persists:
1. Check if the device is in compatibility mode — low-end chips may need periodic Bluetooth adapter resets
2. On rooted devices, Ava can kill and restart the Bluetooth process automatically
3. If the issue persists, try restarting the Ava service or the device

### Can I use Bluetooth media playback and BLE proxy at the same time?

On Full and Balanced tier devices, yes. On Compatibility tier devices, Bluetooth media playback is not supported — the BLE chip cannot handle simultaneous audio streaming and scanning.

---

*Back to [Home](Home)*
