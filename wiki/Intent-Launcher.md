# Intent Launcher

The Android Intent Executor allows you to remotely execute Android Intents from Home Assistant, opening apps or system settings on the Ava device.

---

## Overview

The Intent Launcher exposes a service in Home Assistant that can execute Android Intents remotely. This lets you open apps, launch system settings, or trigger any intent-based action from HA automations.

**What It Does:**
- Execute Android Intents from Home Assistant automations
- Open apps with custom URI schemes
- Launch system settings pages
- Trigger deep links in apps
- Control device behavior through intent actions

**Why Use Intent Launcher:**
- Automate app launching based on time, sensors, or voice commands
- Create custom dashboards that launch specific apps
- Integrate third-party apps into your smart home workflows
- Remote control of Ava device without physical interaction

---

## Setup

### Enable Intent Launcher

1. Go to **Settings** -> **Experimental**
2. Turn on **Intent Launcher**
3. Enable **Show in Home Assistant** to expose the service

---

## How to Call

```yaml
action: esphome.device_name_launch_intent
data:
  intent_uri: "spotify://"
```

---

## Supported Formats

### Open App

```
intent_uri: "spotify://com.spotify.music"
```

### System Settings

```
intent_uri: "intent:#Intent;action=android.settings.SETTINGS;end"
```

### Other Examples

| Intent | Description |
|--------|-------------|
| `spotify://` | Open Spotify |
| `youtube://` | Open YouTube |
| `intent:#Intent;action=android.settings.WIFI_SETTINGS;end` | Open WiFi settings |
| `intent:#Intent;action=android.settings.BLUETOOTH_SETTINGS;end` | Open Bluetooth settings |
| `intent:#Intent;action=android.settings.DISPLAY_SETTINGS;end` | Open display settings |

---

## Use Cases & Scenarios

### 1. Morning Routine Automation

**Scenario:** Start your day with personalized apps and settings

```yaml
automation:
  - alias: "Morning Device Setup"
    trigger:
      - platform: time
        at: "07:00:00"
    action:
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "spotify://"
      - delay: "00:00:05"
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "intent:#Intent;action=android.settings.DISPLAY_SETTINGS;end"
```

### 2. Voice-Controlled App Launcher

**Scenario:** Use voice commands to launch specific apps

```yaml
automation:
  - alias: "Launch Netflix by Voice"
    trigger:
      - platform: conversation
        command: "launch netflix"
    action:
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "netflix://"
      - service: tts.speak
        data:
          message: "Launching Netflix"
```

### 3. Guest Mode Configuration

**Scenario:** Automatically configure device for guests

```yaml
automation:
  - alias: "Activate Guest Mode"
    trigger:
      - platform: input_boolean
        entity_id: input_boolean.guest_mode
        to: "on"
    action:
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "intent:#Intent;action=android.settings.WIFI_SETTINGS;end"
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "kiosk://com.example.kiosk"
```

### 4. Emergency Quick Actions

**Scenario:** Launch emergency apps or contacts

```yaml
automation:
  - alias: "Emergency Call"
    trigger:
      - platform: event
        event_type: emergency_triggered
    action:
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "tel:911"
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "com.google.android.apps.maps"
```

### 5. Media Control Integration

**Scenario:** Control media apps based on presence or time

```yaml
automation:
  - alias: "Evening Music"
    trigger:
      - platform: sun
        event: sunset
    action:
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "spotify://playlist/37i9dQZF1DXcBWIGoYBM5M"
```

---

## App Integration Examples

### Entertainment Apps

| App | Intent URI | Use Case |
|-----|------------|----------|
| Spotify | `spotify://` | Launch music player |
| Netflix | `netflix://` | Open streaming service |
| YouTube | `youtube://` | Launch video platform |
| Plex | `plex://` | Open media server |
| VLC | `vlc://` | Launch media player |

### Communication Apps

| App | Intent URI | Use Case |
|-----|------------|----------|
| WhatsApp | `whatsapp://` | Open messaging |
| Telegram | `tg://` | Launch chat app |
| Discord | `discord://` | Open communication |
| Zoom | `zoomus://` | Start video call |

### Utility Apps

| App | Intent URI | Use Case |
|-----|------------|----------|
| Google Maps | `com.google.android.apps.maps://` | Open navigation |
| Chrome | `googlechrome://` | Launch browser |
| Calculator | `calculator://` | Open calculator |
| Camera | `camera://` | Launch camera |

---

## Advanced Usage

### Custom Intent Actions

```yaml
# Launch specific activity
action: esphome.ava_launch_intent
data:
  intent_uri: "intent:#Intent;component=com.spotify.music/.MainActivity;end"

# Pass extra data
action: esphome.ava_launch_intent
data:
  intent_uri: "intent:#Intent;action=android.intent.action.VIEW;data=https://example.com;end"
```

### Conditional App Launching

```yaml
automation:
  - alias: "Smart App Launcher"
    trigger:
      - platform: state
        entity_id: sensor.time_of_day
    action:
      - choose:
          - conditions:
              - condition: state
                entity_id: sensor.time_of_day
                state: "morning"
            sequence:
              - service: esphome.ava_launch_intent
                data:
                  intent_uri: "spotify://playlist/morning"
          - conditions:
              - condition: state
                entity_id: sensor.time_of_day
                state: "evening"
            sequence:
              - service: esphome.ava_launch_intent
                data:
                  intent_uri: "netflix://"
```

---

## Path Discovery

### Finding App Intent URIs

1. **App Documentation**: Check app's developer documentation
2. **Android Logcat**: Monitor logs when opening apps manually
3. **Third-party Tools**: Use tools like "Intent Interceptor"
4. **Community Resources**: Search HA community forums
5. **Trial and Error**: Test common patterns (appname://, com.package.name)

### System Intent Actions

Common system settings intents:
- `android.settings.SETTINGS` - Main settings
- `android.settings.WIFI_SETTINGS` - WiFi settings
- `android.settings.BLUETOOTH_SETTINGS` - Bluetooth settings
- `android.settings.DISPLAY_SETTINGS` - Display settings
- `android.settings.SOUND_SETTINGS` - Sound settings
- `android.settings.APPLICATION_DETAILS_SETTINGS` - App info

---

## Automation Strategies

### 1. Multi-App Workflows

Create sequences of app launches for complex routines:

```yaml
automation:
  - alias: "Work Mode Setup"
    trigger:
      - platform: time
        at: "09:00:00"
    action:
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "slack://"
      - delay: "00:00:03"
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "calendar://"
      - delay: "00:00:03"
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "gmail://"
```

### 2. Context-Aware Launching

Launch apps based on sensor data:

```yaml
automation:
  - alias: "Weather-Appropriate Entertainment"
    trigger:
      - platform: state
        entity_id: sensor.weather_condition
    action:
      - choose:
          - conditions:
              - condition: state
                entity_id: sensor.weather_condition
                state: "rainy"
            sequence:
              - service: esphome.ava_launch_intent
                data:
                  intent_uri: "netflix://"
          - conditions:
              - condition: state
                entity_id: sensor.weather_condition
                state: "sunny"
            sequence:
              - service: esphome.ava_launch_intent
                data:
                  intent_uri: "spotify://playlist/outdoor"
```

### 3. Voice Integration

Combine with Ava voice commands:

```yaml
automation:
  - alias: "Voice App Launcher"
    trigger:
      - platform: conversation
        command: "open {{ app_name }}"
    action:
      - service: esphome.ava_launch_intent
        data_template:
          intent_uri: "{{ app_name }}://"
```

---

## Working with Other Automation Tools

### Combining with Tasker for Advanced Workflows

Use Intent Launcher as the bridge between Home Assistant and Tasker's powerful Android automation capabilities. While Tasker handles complex device-level operations like sensor monitoring, file operations, and advanced system interactions, Intent Launcher provides the smart home context and remote access that Tasker lacks.

**Example Integration**: Create a Tasker profile that monitors battery level and triggers a Home Assistant automation via Intent Launcher to launch a specific app when battery drops below 20%, while HA simultaneously adjusts smart home lighting to indicate charging status.

### Enhancing Automate with Smart Home Context

Automate excels at complex flow-based Android automation but operates in isolation from your smart home. Use Intent Launcher to inject real-world context into Automate flows - trigger Automate flows based on HA sensor states, or have Automate execute actions that launch specific apps through HA automations.

**Example Integration**: Set up an Automate flow that controls device brightness based on ambient light sensors, then use Intent Launcher to launch different entertainment apps based on the time of day detected by Home Assistant's sun sensor.

### Extending Android Shortcuts with Automation

Android Shortcuts provide convenient manual access but lack automation intelligence. Use Intent Launcher to transform static shortcuts into dynamic, context-aware launchers that adapt to your current situation, location, and smart home state.

**Example Integration**: Create a "Movie Time" shortcut that normally launches Netflix, but through Intent Launcher and HA automation, it launches Disney+ when kids are detected home, or launches Prime Video when it's raining outside.

---

## Home Assistant Integration

### Status Entity

```
sensor.your_device_name_intent_launcher_status
```

### Service Call Format

```yaml
service: esphome.ava_launch_intent
data:
  intent_uri: "app://"
```

### Automation Examples

#### Time-Based Launch
```yaml
automation:
  - alias: "Open Spotify in the morning"
    trigger:
      - platform: time
        at: "07:00:00"
    action:
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "spotify://"
```

#### Sensor-Based Launch
```yaml
automation:
  - alias: "Launch entertainment when presence detected"
    trigger:
      - platform: state
        entity_id: binary_sensor.living_room_presence
        to: "on"
    action:
      - service: esphome.ava_launch_intent
        data:
          intent_uri: "netflix://"
```

#### Voice-Activated Launch
```yaml
automation:
  - alias: "Voice app launcher"
    trigger:
      - platform: conversation
        command: "launch {{ app }}"
    action:
      - service: esphome.ava_launch_intent
        data_template:
          intent_uri: "{{ app }}://"
```

---

## Troubleshooting

### Common Issues

#### Intent Not Executing
1. Check if Intent Launcher is enabled
2. Verify Show in Home Assistant is enabled
3. Confirm intent URI format is correct
4. Check if target app is installed

#### App Not Opening
1. Ensure app is installed on the device
2. Verify correct intent URI for the app
3. Some apps use different URI schemes
4. Check if app supports intent launching

#### Service Not Found
1. Restart Ava voice satellite
2. Check HA log for errors
3. Verify device is online
4. Re-enable Intent Launcher

### Debugging Tips

1. **Check Logs**: Monitor HA logs for intent execution errors
2. **Test Manually**: Try launching apps manually first
3. **Use Simple Intents**: Start with basic app URIs
4. **Verify App Installation**: Confirm apps are installed
5. **Check Permissions**: Ensure Ava has necessary permissions

---

## Best Practices

1. **Test Intents**: Always test intent URIs before adding to automations
2. **Use Delays**: Add delays between multiple app launches
3. **Error Handling**: Include fallback actions in automations
4. **Document URIs**: Keep a list of working intent URIs
5. **Monitor Status**: Use the status entity to verify execution
6. **Version Compatibility**: Check app updates don't break intent URIs

---

*Back to [Home](Home)*
