# Sensors

Ava reads device sensor data and exposes it to Home Assistant, including environment sensors, battery, proximity, and diagnostic information.

Compatible with Android 5-16.

---

## Why Ava's Sensors Are Different

Most Android apps that expose sensor data to Home Assistant rely on third-party integrations like REST API bridges, Tasker plugins, or MQTT gateways. Ava is different:

- **Native ESPHome integration** — sensors appear as native entities in Home Assistant, no extra integration needed
- **Zero configuration** — enable in Settings, entities auto-appear in HA
- **Runs as a foreground service** — sensor updates continue even when the screen is off, unlike task-based apps that Android kills in the background
- **Combined sensor pipeline** — light, magnetic, and proximity sensors share a single update cycle with noise smoothing, reducing drift and battery drain
- **Proximity + light fusion** — proximity wake threshold adapts to ambient light, preventing false wakes in dark rooms
- **Battery whitelist** — Ava automatically requests battery optimization exemption when root or Shizuku is available, ensuring sensor updates are never throttled

---

## Sensor Categories

| Category | Description |
|----------|-------------|
| Environment Sensors | Light, magnetic field |
| Battery Monitor | Battery level, voltage, charging status |
| Proximity Sensor | Object proximity detection with screen wake |
| Diagnostic Sensors | WiFi, IP, storage, memory, uptime, microphone volume |

---

## Environment Sensors

Go to **Settings** -> **Device Controls** -> **Environment Sensors**

| Sensor | Description | Unit | HA Entity |
|--------|-------------|------|-----------|
| Light Sensor | Ambient light intensity | lux | `sensor.your_device_name_light_sensor` |
| Magnetic Sensor | Magnetic field strength (3-axis combined) | μT | `sensor.your_device_name_magnetic_sensor` |

### Settings

| Setting | Description | Default |
|---------|-------------|---------|
| Environment Sensors | Enable reading available sensors | Off |
| Light Sensor | Publish light level to HA | On (if sensor exists) |
| Magnetic Sensor | Publish magnetic field to HA | Off |
| Update Interval | How often to publish sensor data to HA (5-60s) | 35s |

### Light Sensor

Measures ambient light intensity in lux. Ava reads the sensor and publishes to Home Assistant at the configured interval with noise smoothing to filter out spikes.

**Built-in feature — Light sensor screen off:**

When enabled in Screensaver settings, Ava turns off the screen in dark environments (e.g., face-down on a table or in a pocket) and turns it back on when light returns. This saves battery and prevents accidental touches without requiring a proximity sensor.

**Scenario — Auto-adjust room lighting:**

```yaml
automation:
  - alias: "Auto Light Adjustment"
    trigger:
      - platform: numeric_state
        entity_id: sensor.ava_light_sensor
        below: 50
    action:
      - service: light.turn_on
        target:
          entity_id: light.living_room
```

**Scenario — Curtains close when too bright:**

```yaml
automation:
  - alias: "Close Curtains in Bright Sunlight"
    trigger:
      - platform: numeric_state
        entity_id: sensor.ava_light_sensor
        above: 3000
    action:
      - service: cover.close_cover
        target:
          entity_id: cover.living_room
```

### Magnetic Sensor

Measures ambient magnetic field strength. Ava combines all three axes into a single magnitude value and publishes at the configured interval with noise smoothing.

**Scenario — Key detection:**

Place the device near a door or key rack. When someone brings metal keys close, the magnetic field spikes. Use this to trigger automations:

```yaml
automation:
  - alias: "Someone Picked Up Keys"
    trigger:
      - platform: numeric_state
        entity_id: sensor.ava_magnetic_sensor
        above: 80
    action:
      - service: notify.mobile_app
        data:
          message: "Keys detected — someone is leaving"
```

**Scenario — Door/window open detection:**

If a small magnet is attached to a door or window frame, the magnetic sensor can detect when the door opens (magnet moves away) or closes (magnet returns):

```yaml
automation:
  - alias: "Door Opened"
    trigger:
      - platform: numeric_state
        entity_id: sensor.ava_magnetic_sensor
        below: 25
    action:
      - service: notify.mobile_app
        data:
          message: "Front door opened"
```

**Scenario — Remote control with built-in magnet:**

Some TV remote controls or stylus pens have built-in magnets. Moving such an object near the device causes a detectable shift in the magnetic field. This can be used as a "magnetic switch" — wave the remote near the device to trigger a scene:

```yaml
automation:
  - alias: "Magnetic Trigger - Movie Scene"
    trigger:
      - platform: numeric_state
        entity_id: sensor.ava_magnetic_sensor
        above: 60
    action:
      - service: scene.turn_on
        target:
          entity_id: scene.movie_night
```

**Note:** Earth's background magnetic field is typically 25-65 μT. Significant deviations from this baseline indicate a nearby magnetic or metal object. The baseline varies by location, so calibrate by observing the sensor value in the target environment before setting thresholds.

---

## Battery Monitor

Go to **Settings** -> **Device Controls** -> **Diagnostic Sensors**

| Sensor | Description | HA Entity |
|--------|-------------|-----------|
| Battery Level | Battery percentage | `sensor.your_device_name_battery_level` |
| Battery Voltage | Battery voltage | `sensor.your_device_name_battery_voltage` |
| Charging Status | Charging or discharging | `sensor.your_device_name_charging_status` |

**Scenario — Low battery alert:**

```yaml
automation:
  - alias: "Ava Device Low Battery"
    trigger:
      - platform: numeric_state
        entity_id: sensor.ava_battery_level
        below: 20
    action:
      - service: notify.mobile_app
        data:
          message: "Ava device battery is low ({{ states('sensor.ava_battery_level') }}%)"
```

**Scenario — Auto-dim screen when not charging:**

```yaml
automation:
  - alias: "Dim Screen on Battery"
    trigger:
      - platform: state
        entity_id: sensor.ava_charging_status
        to: "discharging"
    action:
      - service: number.set_value
        target:
          entity_id: number.ava_screen_brightness
        data:
          value: 30
```

---

## Proximity Sensor

Go to **Settings** -> **Device Controls** -> **Proximity Sensor**

| Sensor | Description | HA Entity |
|--------|-------------|-----------|
| Proximity | Object proximity detected (near/far) | `binary_sensor.your_device_name_proximity` |
| Proximity Distance | Distance value | `sensor.your_device_name_proximity_distance` |

### Settings

| Setting | Description | Default |
|---------|-------------|---------|
| Proximity Sensor | Enable proximity sensor | Off |
| Send to Home Assistant | Expose proximity distance as HA sensor | Off |
| Proximity Wake | Wake screen when object is near | On |
| Trigger Threshold | Wake screen when closer than this (auto-adjusted) | Sensor max range |
| Away Delay | Seconds before marking as away (10-120s) | 30 |
| Send Interval | How often to send proximity distance to HA (5-120s) | 20 |
| Auto Unlock | Also dismiss keyguard on wake | Off |

### How Proximity Wake Works

Ava's proximity wake is smarter than a simple threshold:

1. **Light-aware threshold** — In low light (< 20 lux), the "far" threshold lowers from 130 to 120. This prevents false wakes in dark rooms where the proximity sensor may behave differently.
2. **Debounced wake** — After waking, a 3-second cooldown prevents repeated wake triggers from the same hand wave.
3. **Away delay** — When the object moves away, Ava waits the configured delay (default 30s) before marking as "away". This prevents flicker when someone is moving around near the device.
4. **Immediate refresh** — On startup, Ava aggressively polls the proximity sensor (up to 12 attempts at 500ms intervals) to get an initial reading quickly, rather than waiting for the first sensor event.

**Scenario — Wake screen when someone approaches:**

```yaml
automation:
  - alias: "Proximity Wake"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_proximity
        to: "on"
    action:
      - service: switch.turn_off
        target:
          entity_id: switch.ava_screensaver
```

**Scenario — Turn on display for kiosk:**

```yaml
automation:
  - alias: "Kiosk Wake on Approach"
    trigger:
      - platform: state
        entity_id: binary_sensor.ava_proximity
        to: "on"
    action:
      - service: switch.turn_on
        target:
          entity_id: switch.ava_display
```

---

## Diagnostic Sensors

Go to **Settings** -> **Device Controls** -> **Diagnostic Sensors**

| Sensor | Description | HA Entity |
|--------|-------------|-----------|
| WiFi Signal | WiFi signal strength | `sensor.your_device_name_wifi_signal` |
| Device IP | Network address | `sensor.your_device_name_device_ip` |
| Storage | Remaining storage capacity | `sensor.your_device_name_storage_free` |
| Memory | Used memory | `sensor.your_device_name_memory_usage` |
| Uptime | Device running time | `sensor.your_device_name_device_uptime` |
| Microphone Volume | Microphone volume level (0-2.0) | `number.your_device_name_microphone_volume` |

### System Control Entities

| Entity | Description |
|--------|-------------|
| `button.your_device_name_kill_app` | Force close Ava app |
| `button.your_device_name_reboot_device` | Reboot the entire device (requires root) |

**Scenario — Reboot on high memory:**

```yaml
automation:
  - alias: "Reboot on Memory Pressure"
    trigger:
      - platform: numeric_state
        entity_id: sensor.ava_memory_usage
        above: 90
        for:
          minutes: 10
    action:
      - service: button.press
        target:
          entity_id: button.ava_reboot_device
```

**Scenario — Alert on storage low:**

```yaml
automation:
  - alias: "Storage Low Alert"
    trigger:
      - platform: numeric_state
        entity_id: sensor.ava_storage_free
        below: 500
    action:
      - service: notify.mobile_app
        data:
          message: "Ava device storage is low ({{ states('sensor.ava_storage_free') }} MB)"
```

---

## Intent Launcher

Go to **Settings** -> **Device Controls** -> **Android Intent Executor**

Remote intent execution via Home Assistant. See [Intent Launcher](Intent-Launcher) for detailed guide.

---

## Device Capability Detection

Ava automatically detects if the device has each sensor at startup. The detection is cached — it runs once and the result is reused for the session.

| Detection | Method | Description |
|-----------|--------|-------------|
| hasLightSensor | System sensor query | Has light sensor |
| hasMagneticSensor | System sensor query | Has magnetic sensor |
| hasProximitySensor | System sensor query | Has proximity sensor |
| hasAnySensor | Any of the above | Has any environment sensor |

If a device lacks a sensor, the corresponding entity will not be created in Home Assistant. No error, no empty entity — Ava simply skips it.

---

## FAQ

### Sensor data not updating?

1. Check if the device has that sensor (see Device Capability Detection above)
2. Check if Ava service is running
3. Check the update interval setting (Settings → Device Controls → Environment Sensors)
4. Some device ROMs restrict sensor access for background apps — try disabling battery optimization for Ava

### Light sensor screen off not working?

1. Confirm the device has a light sensor
2. Check if "Turn off in dark" is enabled in Screensaver settings
3. Make sure the sensor is not blocked (e.g., device in a case)

### Proximity sensor wake not working?

1. Confirm the device has a proximity sensor
2. Check if Proximity Wake is enabled in Settings → Device Controls → Proximity Sensor
3. Check the proximity sensor's maximum range — some devices report very short range
4. The trigger threshold auto-adjusts based on ambient light — try in a well-lit room first

### Magnetic sensor readings seem wrong?

1. Move away from large metal objects, speakers, or monitors — these affect the magnetic field
2. Calibrate the baseline by observing the sensor value in the target environment
3. Some devices have very sensitive magnetometers that drift — noise smoothing helps but cannot eliminate all noise

### Battery sensor not showing?

1. Enable Battery Level in Settings → Device Controls → Diagnostic Sensors
2. Check if the device reports battery info (some kiosk devices lack battery hardware)

### Reboot device not working?

1. Reboot requires root access
2. Check root status in Settings → Permissions
3. Alternatively, use Shizuku for elevated permissions

---

*Back to [Home](Home)*
