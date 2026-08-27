# Notification Scenes

Notification Scenes are Ava's signature feature. When a smart home event occurs, Ava displays a beautiful full-screen animated notification overlay with themed colors, icons, and live sensor data, all rendered on top of whatever is currently on screen.

---

## Overview

When someone rings your doorbell, Ava doesn't just play a sound. It displays a full-screen animated notification with a bell icon, amber-themed gradient background, pulsing light beam, and your custom text. When a water leak is detected, the screen fills with a cyan-themed water drop animation. When you get home, a green welcome scene greets you.

**What a scene displays:**
- Full-screen gradient background with aurora-like light beam animation
- Animated FontAwesome icon with glow effect
- Title, description, and sub-description text
- Live Home Assistant sensor values (via template placeholders)
- Themed divider line with glow
- Pulsing dot indicator
- Optional notification sound (per-scene or global)

**How scenes are triggered:**
- From Home Assistant via a `select` entity dropdown
- From HA automations using `select.select_option` service
- Programmatically via ESPHome text commands

---

## Enable Notification Scenes

1. Go to **Settings** -> **Interaction** -> **Notification Scenes**
2. Toggle **Notification Scene** to On
3. **Restart the Voice Satellite service** for the entity to appear in Home Assistant
4. A `select` entity named `notification_scene` will appear under your Ava device in HA

> **Important:** Changes to notification scene settings require a voice service restart to take effect. After enabling or disabling scenes, go to Settings -> Voice and tap Restart Service.

---

## How to Trigger a Scene

### From Home Assistant UI

1. Go to **Settings** -> **Devices & Services** -> ESPHome -> your Ava device
2. Find the **Notification Scene** dropdown (`select.your_device_notification_scene`)
3. Select a scene title from the dropdown
4. The scene appears immediately on the Ava device's screen

### From an Automation (YAML)

```yaml
service: select.select_option
target:
  entity_id: select.your_device_name_notification_scene
data:
  option: "Someone rings the doorbell"
```

### Quick Local Test from HA Developer Tools

You can test any scene immediately without writing an automation:

1. Open Home Assistant in your browser
2. Go to **Developer Tools** -> **Services**
3. Search for `select.select_option`
4. Set entity_id to your Ava's notification scene select entity
5. Set option to a scene title (e.g., "Good morning, new day")
6. Click **Call Service**
7. The scene appears on the Ava device within 1 second

This is the fastest way to preview scenes and test custom scenes before building automations.

### Finding Your Entity ID

Your notification scene entity ID follows this pattern:
```
select.<device_name>_notification_scene
```

Find it in HA: **Settings** -> **Devices & Services** -> **ESPHome** -> click your Ava device -> look for "Notification Scene" in the entities list.

---

## Built-in Scenes

Ava includes **90 built-in scenes** covering common smart home events. Scenes are loaded automatically based on your language (Chinese or English) and cached locally for offline use.

### Scene Categories

**Security & Safety**

| Scene ID | Title | Icon | Trigger Example |
|----------|-------|------|-----------------|
| doorbell | Someone rings the doorbell | fa-bell | Doorbell binary sensor ON |
| door_open | Doors and windows are open | fa-door-open | Door/window sensor ON |
| smoke | Smoke alarm | fa-fire | Smoke sensor ON |
| gas_leak | Gas leak alarm | fa-biohazard | Gas sensor ON |
| water_leak | Water leak alarm | fa-droplet | Water leak sensor ON |
| security | Security is armed | fa-shield-halved | Alarm panel armed |
| door_lock | The door is locked | fa-lock | Door lock locked |
| door_unlock | Door lock is unlocked | fa-lock-open | Door lock unlocked |
| motion | Movement detected | fa-person-walking | Motion sensor ON |
| offline | Device is offline | fa-plug-circle-xmark | Device availability |

**Climate & Environment**

| Scene ID | Title | Icon | Trigger Example |
|----------|-------|------|-----------------|
| temp_high | Temperature too high | fa-temperature-high | Temperature > threshold |
| weather | Severe weather warning | fa-cloud-bolt | Weather alert |
| typhoon | Typhoon warning | fa-hurricane | Weather alert |
| earthquake | Earthquake early warning | fa-house-crack | Earthquake sensor |
| air_quality_warning | Air quality warning | fa-smog | AQI sensor > threshold |
| air_purifier | Air purification in progress | fa-wind | Air purifier ON |
| humidifier | Humidifier is running | fa-smog | Humidifier ON |
| fresh_air | Fresh air system running | fa-leaf | Fresh air system ON |
| floor_heating | Floor heating is on | fa-temperature-arrow-up | Floor heating ON |

**Daily Life & Routine**

| Scene ID | Title | Icon | Trigger Example |
|----------|-------|------|-----------------|
| morning | Good morning, new day | fa-sun | Morning time trigger |
| goodnight | Good night and sweet dreams | fa-moon | Bedtime trigger |
| alarm | The alarm clock rang | fa-clock | Alarm trigger |
| coffee | Coffee is ready | fa-mug-hot | Coffee maker done |
| rice_cooker | Rice is cooked | fa-fire-burner | Rice cooker done |
| microwave | Microwave oven completed | fa-radiation | Microwave done |
| dishwasher | Dishwasher finished | fa-sink | Dishwasher done |
| laundry_done | Washing machine is finished | fa-shirt | Washer done |
| cooking_time | Cooking | fa-utensils | Cooking timer |
| water_heater | Hot water is ready | fa-hot-tub-person | Water heater done |

**Appliances & Devices**

| Scene ID | Title | Icon | Trigger Example |
|----------|-------|------|-----------------|
| ac_on | The air conditioner is on | fa-snowflake | AC turned ON |
| tv | TV is on | fa-tv | TV power ON |
| speaker | Speaker playing | fa-volume-high | Media player playing |
| projector | Projector is on | fa-film | Projector ON |
| printer | Printing completed | fa-print | Print job done |
| nas | NAS backup completed | fa-hard-drive | Backup task done |
| router | Network is connected | fa-tower-broadcast | Network status |
| ev_charging | Electric car charging | fa-charging-station | EV charger ON |
| ev_charged | EV charging completed | fa-bolt | EV charger done |

**Reminders & Health**

| Scene ID | Title | Icon | Trigger Example |
|----------|-------|------|-----------------|
| battery_low | Low battery | fa-battery-quarter | Battery < threshold |
| medicine_reminder | Medication reminder | fa-pills | Time trigger |
| hydration_reminder | Drink water reminder | fa-glass-water | Time trigger |
| sleep_reminder | Time to sleep | fa-bed | Bedtime trigger |
| sleep_report | Sleep report | fa-bed | Morning trigger |
| workout | Time to exercise | fa-person-running | Schedule trigger |
| stand_reminder | Get up and walk | fa-person | Sedentary reminder |
| eye_protection | Look at poetry and the distance | fa-eye | Screen time reminder |
| posture_reminder | Sit up straight | fa-person-arrow-up-from-line | Posture sensor |
| weight_scale | Weight has been recorded | fa-weight-scale | Scale reading |

**Social & Emotional**

| Scene ID | Title | Icon | Trigger Example |
|----------|-------|------|-----------------|
| welcome_home | Master, thank you for your hard work | fa-house-user | Presence detected |
| love_reminder | Baby, I miss you | fa-heart | Schedule trigger |
| anniversary | Wedding anniversary | fa-ring | Date trigger |
| birthday | Happy birthday | fa-cake-candles | Date trigger |
| new_year | Happy new year | fa-champagne-glasses | Date trigger |
| go_out_reminder | You're going out, be safe | fa-door-open | Presence away |
| food_delivery | The takeaway is here | fa-motorcycle | Delivery trigger |
| package | Express arrives | fa-box | Delivery sensor |
| visitor | Visitors arrive | fa-user | Doorbell/Pir |

**Modes**

| Scene ID | Title | Icon | Trigger Example |
|----------|-------|------|-----------------|
| eco_mode | Energy saving mode | fa-seedling | Mode switch |
| party_mode | Party mode | fa-champagne-glasses | Mode switch |
| cinema_mode | Cinema mode | fa-clapperboard | Mode switch |
| reading_mode | Reading mode | fa-book-open | Mode switch |
| gaming_mode | Game mode | fa-gamepad | Mode switch |
| karaoke_mode | Karaoke mode | fa-microphone | Mode switch |
| focus_time | Focus time | fa-bullseye | Mode switch |

---

## Scene Settings

Go to **Settings** -> **Interaction** -> **Notification Scenes**

| Setting | Description | Default |
|---------|-------------|---------|
| Notification Scene | Enable/disable the feature | Off |
| Display Duration | How long the scene stays on screen (5-999999 seconds) | 10s |
| Notification Sound | Play sound when scene appears | On |
| Custom Scene URL | URL to load custom scenes from | Empty |
| Change Alarm Sound | Select timer alarm sound | Default |

> **Remember:** After changing any setting, restart the voice service for changes to take effect.

---

## Sensor Placeholders

You can embed live Home Assistant sensor values directly into scene text. Values update in real time while the scene is on screen. Unknown or unsubscribed entities show "--".

### Syntax

| Syntax | Result | Example |
|--------|--------|---------|
| `{{sensor.living_room_temp}}` | Entity current state | `23.5` |
| `{{sensor.living_room_temp\|unit}}` | unit_of_measurement (shortcut) | `°C` |
| `{{sensor.living_room_temp\|state}}` | Explicit state (same as no pipe) | `23.5` |
| `{{sensor.living_room_temp\|battery}}` | Any attribute name | `87` |

### Placeholder Rules

- Spaces around the pipe are allowed: `{{ sensor.foo | unit }}`
- Entity ID must match HA naming: `domain.entity_id` (lowercase, dots, underscores)
- Non-matching placeholders are left as-is (not replaced)
- Values refresh automatically when the entity state changes in HA

### Example Scene with Placeholders

```json
{
  "id": "temp_alert",
  "icon": "fa-temperature-high",
  "iconColor": "text-red-400",
  "title": "Temperature Alert",
  "desc": "Living room: {{sensor.living_room_temperature}}{{sensor.living_room_temperature|unit}}",
  "subDesc": "Threshold exceeded, current: {{sensor.living_room_humidity}}{{sensor.living_room_humidity|unit}}",
  "themeColors": ["#ef4444", "#dc2626", "#991b1b", "#7f1d1d"],
  "beamColor": "rgba(239, 68, 68, 0.8)",
  "dividerColor": "rgba(239, 68, 68, 0.8)",
  "dotColor": "bg-red-400",
  "animation": "pulse 1.5s infinite"
}
```

This renders as:
- Title: `Temperature Alert`
- Desc: `Living room: 28.3 °C`
- SubDesc: `Threshold exceeded, current: 65 %`

---

## Custom Scenes

You can create your own scenes by hosting a JSON file. Custom scenes are loaded in addition to built-in scenes.

### JSON Format

Custom scenes must be a JSON array (or an object containing a `scenes`, `data`, `items`, or `list` array):

```json
[
  {
    "id": "my_custom_scene",
    "icon": "fa-heart",
    "iconColor": "text-rose-400",
    "title": "I love you",
    "desc": "Sent from Home Assistant",
    "subDesc": "Have a wonderful day",
    "themeColors": ["#f43f5e", "#e11d48", "#9f1239", "#881337"],
    "beamColor": "rgba(244, 63, 94, 0.8)",
    "dividerColor": "rgba(244, 63, 94, 0.8)",
    "dotColor": "bg-rose-400",
    "animation": "pulse 1s infinite",
    "soundUri": "content://media/external/audio/media/42",
    "soundEnabled": true
  }
]
```

### All Fields

| Field | Required | Type | Description | Default |
|-------|----------|------|-------------|---------|
| id | Yes | string | Unique scene identifier | (none) |
| icon | Yes | string | FontAwesome icon name (e.g., `fa-bell`) | (none) |
| title | Yes | string | Scene title text | (none) |
| iconColor | No | string | Icon color (hex, rgba, or Tailwind name) | `text-amber-200` |
| desc | No | string | Description text | Empty |
| subDesc | No | string | Sub-description text | Empty |
| themeColors | No | array | 4 gradient colors (hex strings) | Amber gradient |
| beamColor | No | string | Light beam color (hex or rgba) | `rgba(251, 191, 36, 0.8)` |
| dividerColor | No | string | Divider line color (hex or rgba) | `rgba(251, 191, 36, 0.8)` |
| dotColor | No | string | Dot indicator color | `bg-amber-300` |
| animation | No | string | CSS-like animation name and timing | Empty (no animation) |
| soundUri | No | string | Scene-specific notification sound URI | Global setting |
| soundEnabled | No | boolean | Enable/disable sound for this scene | Global setting |

### Required Fields

Only three fields are required. Everything else has defaults:
- `id` — must be unique
- `icon` — a FontAwesome icon name
- `title` — the main text

### Animation Values

Animations use CSS-like syntax. Available animation names:

| Animation | Example | Effect |
|-----------|---------|--------|
| pulse | `pulse 1.5s infinite` | Scale pulsing |
| bell-ring | `bell-ring 1s ease-in-out infinite` | Bell shake |
| sun-rise | `sun-rise 3s ease-in-out infinite` | Rising motion |
| spin-slow | `spin-slow 8s linear infinite` | Slow rotation |
| (empty) | `""` | No icon animation |

### Color Formats

Ava supports three color formats:

| Format | Example | Use For |
|--------|---------|---------|
| Hex | `#f59e0b` | themeColors, dotColor |
| RGBA | `rgba(251, 191, 36, 0.8)` | beamColor, dividerColor |
| Tailwind | `text-amber-200`, `bg-cyan-400` | iconColor, dotColor |

**Supported Tailwind color families:** amber, yellow, orange, red, rose, pink, fuchsia, purple, violet, indigo, blue, sky, cyan, teal, emerald, green, lime, stone, neutral, gray, slate

**Levels:** 200, 300, 400, 500, 600, 700

### Sound Configuration

Each scene can have its own sound, or fall back to the global notification sound:

| Field | Behavior |
|-------|----------|
| `soundEnabled: false` | Scene is silent, no sound played |
| `soundEnabled: true` + `soundUri` | Play the specified sound URI |
| `soundEnabled: true` + no `soundUri` | Fall back to global notification sound |
| `soundEnabled` not set | Fall back to global notification sound setting |

Sound URIs are Android content URIs (e.g., `content://media/external/audio/media/42`) or file paths.

### Loading Custom Scenes

**Method 1: Home Assistant local file (recommended for testing)**

1. Create your JSON file (e.g., `my_scenes.json`)
2. Copy it to your Home Assistant `www` folder: `/config/www/my_scenes.json`
3. In Ava, go to **Settings** -> **Interaction** -> **Notification Scenes**
4. Enter URL: `http://your-ha-ip:8123/local/my_scenes.json`
5. **Restart the voice service**
6. Custom scenes appear with `custom_` prefix in their IDs
7. In HA, the dropdown will show custom scenes as "Custom: Your Scene Title"

This method is fast and fully local. No external server needed.

**Method 2: External URL (GitHub, web server)**

1. Host your JSON file on any web server (GitHub raw, Gist, your server)
2. Enter the full URL in Custom Scene URL setting
3. **Restart the voice service**
4. Custom scenes load and merge with built-in scenes

**Method 3: GitHub raw URL with proxy**

If GitHub is slow in your region, use a proxy:
```
https://ghfast.top/https://raw.githubusercontent.com/yourname/yourrepo/main/my_scenes.json
```

### Custom Scene JSON Validation

When loading custom scenes, Ava validates:
- JSON must be an array, or an object with `scenes`/`data`/`items`/`list` key
- Each scene must have non-empty `id`, `icon`, and `title` fields
- Invalid scenes are silently skipped (check Ava logs for warnings)
- Custom scene IDs are automatically prefixed with `custom_` to avoid conflicts with built-in scenes

---

## Complete Custom Scene Example

Here is a complete custom scenes file with 3 scenes demonstrating different color themes, animations, and placeholder usage:

```json
[
  {
    "id": "doorbell_custom",
    "icon": "fa-bell",
    "iconColor": "text-amber-300",
    "title": "Doorbell",
    "desc": "Someone is at the door",
    "subDesc": "Front door camera: {{camera.front_door|state}}",
    "themeColors": ["#fbbf24", "#f59e0b", "#b45309", "#78350f"],
    "beamColor": "rgba(251, 191, 36, 0.8)",
    "dividerColor": "rgba(251, 191, 36, 0.8)",
    "dotColor": "bg-amber-400",
    "animation": "bell-ring 1s ease-in-out infinite",
    "soundEnabled": true
  },
  {
    "id": "temp_warning",
    "icon": "fa-temperature-high",
    "iconColor": "text-red-400",
    "title": "High Temperature Warning",
    "desc": "Server room: {{sensor.server_room_temp}}{{sensor.server_room_temp|unit}}",
    "subDesc": "Threshold: 35 °C, Humidity: {{sensor.server_room_humidity}}{{sensor.server_room_humidity|unit}}",
    "themeColors": ["#ef4444", "#dc2626", "#991b1b", "#7f1d1d"],
    "beamColor": "rgba(239, 68, 68, 0.8)",
    "dividerColor": "rgba(239, 68, 68, 0.8)",
    "dotColor": "bg-red-400",
    "animation": "pulse 0.8s infinite",
    "soundEnabled": true
  },
  {
    "id": "good_night_custom",
    "icon": "fa-moon",
    "iconColor": "text-indigo-300",
    "title": "Good Night",
    "desc": "All doors locked, lights dimmed",
    "subDesc": "Tomorrow: {{sensor.weather_forecast|state}}",
    "themeColors": ["#818cf8", "#6366f1", "#4f46e5", "#3730a3"],
    "beamColor": "rgba(129, 140, 248, 0.8)",
    "dividerColor": "rgba(129, 140, 248, 0.8)",
    "dotColor": "bg-indigo-400",
    "animation": "pulse 3s infinite",
    "soundEnabled": false
  }
]
```

---

## Timer and Alarm

Ava includes a timer feature with alarm sound and scene integration.

### Timer Sound

1. Go to **Settings** -> **Interaction** -> **Notification Scenes**
2. Find **Change Alarm Sound**
3. Select a default or external audio file

### Stop Alarm

When the timer alarm is ringing:
- Say the stop word "Stop" (or "Ok Stop" with vsWakeWord engine) to dismiss it
- Tap the time on screen to dismiss
- If recognition is unreliable while the alarm plays, enable Software echo cancellation in Voice Config settings

Enable **Show Stop Alarm Button** to also show a Stop Alarm button entity in Home Assistant:

```yaml
service: button.press
target:
  entity_id: button.your_device_name_stop_alarm
```

---

## Scene Display Duration

Control how long scenes stay on screen:

1. Go to **Settings** -> **Interaction** -> **Notification Scenes**
2. Set **Display Duration** (5 to 999999 seconds)
3. **Restart the voice service**
4. Scenes auto-hide after the configured duration
5. Touch the screen to dismiss a scene immediately

You can also change the duration from Home Assistant via the `number` entity:

```yaml
service: number.set_value
target:
  entity_id: number.your_device_name_scene_display_duration
data:
  value: 15
```

---

## Automation Examples

### Doorbell Notification

```yaml
automation:
  - alias: "Doorbell Scene"
    trigger:
      - platform: state
        entity_id: binary_sensor.doorbell
        to: "on"
    action:
      - service: select.select_option
        target:
          entity_id: select.living_tablet_notification_scene
        data:
          option: "Someone rings the doorbell"
```

### Water Leak Alert with Auto-Dismiss

```yaml
automation:
  - alias: "Water Leak Scene"
    trigger:
      - platform: state
        entity_id: binary_sensor.water_leak_kitchen
        to: "on"
    action:
      - service: select.select_option
        target:
          entity_id: select.kitchen_tablet_notification_scene
        data:
          option: "water leak alarm"
```

### Morning Report with Sensor Values

```yaml
automation:
  - alias: "Morning Report"
    trigger:
      - platform: time
        at: "07:00:00"
    action:
      - service: select.select_option
        target:
          entity_id: select.bedroom_tablet_notification_scene
        data:
          option: "Good morning, new day"
```

### Custom Scene Trigger

```yaml
automation:
  - alias: "Custom Love Reminder"
    trigger:
      - platform: time
        at: "12:00:00"
    action:
      - service: select.select_option
        target:
          entity_id: select.living_tablet_notification_scene
        data:
          option: "Custom: I love you"
```

> Note: Custom scenes appear in the dropdown with "Custom: " prefix before the title.

---

## FAQ

### Scenes not loading?

1. Check network connection, scenes download from GitHub on first launch
2. Scenes cache locally after first download, subsequent loads work offline
3. Try restarting the voice service
4. Check Ava logs for scene loading errors

### Notification scene entity not appearing in HA?

1. Ensure **Notification Scene** is enabled in Settings -> Interaction -> Notification Scenes
2. **Restart the voice service** after enabling
3. Check HA Settings -> Devices & Services -> ESPHome -> your device for the select entity
4. The entity is named `notification_scene` under your device

### Custom scenes not showing?

1. Check if the URL is accessible from the Ava device
2. Verify JSON format is correct (must be an array or object with scenes/data/items/list key)
3. Required fields: `id`, `icon`, `title` (all non-empty)
4. Custom scene IDs are prefixed with `custom_`
5. Custom scene titles appear with "Custom: " prefix in the dropdown
6. **Restart the voice service** after changing the Custom Scene URL

### Sensor placeholders show "--"?

1. Check that the entity_id is correct (format: `domain.entity_id`)
2. Check that the entity exists in Home Assistant
3. Ava must be subscribed to the entity (it subscribes automatically when a scene referencing the entity is loaded)
4. Values update in real time when the entity state changes in HA
5. Unknown or unsubscribed values render as "--"

### Timer alarm doesn't stop?

1. Say "Stop" (or "Ok Stop" with vs engine) clearly
2. Tap the time on screen
3. Enable Software echo cancellation in Voice Config if recognition is unreliable during alarm
4. Enable Show Stop Alarm Button for a HA button entity

### How to hide a scene immediately?

- Touch the screen to dismiss
- Scenes auto-hide after the configured display duration
- There is no explicit "hide" service, selecting a new scene replaces the current one

### Changes to settings not taking effect?

**You must restart the voice service** after changing notification scene settings. Go to Settings -> Voice -> Restart Service. This applies to:
- Enabling/disabling notification scenes
- Changing display duration
- Changing custom scene URL
- Changing notification sound

---

*Back to [Home](Home)*
