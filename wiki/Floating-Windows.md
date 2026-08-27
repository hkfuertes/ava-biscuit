# Floating Windows

Floating windows overlay on top of any app, displaying clocks, weather, notifications, and music controls while a full-screen HA dashboard is visible.

---

## Overview

This is what makes Ava visually unique. Floating windows overlay on top of any app - display a full-screen HA dashboard while still seeing clock, weather, and notifications overlaid on top.

Other apps force you to choose: dashboard or clock. With Ava, you get both.

**Features:**
- Always visible - overlays on any app, including full-screen browsers
- HA control - remotely toggle each window from Home Assistant
- Non-intrusive - designed to complement, not block the main interface
- Global overlay back control (0.5.4+) - auto-hiding oval back button on all fullscreen overlays

### Global Overlay Back Control

In Weather, Dream Clock, Minimal Clock, and Quick Entities, a light tap on the screen reveals a floating oval back button at the top edge. It auto-hides after a few seconds of inactivity. One tap returns you to the main dashboard or exits the current overlay.

This is especially critical for Facebook Portal, tablets, and wall-mounted panels running 24/7, where browser overlays and fullscreen layers previously trapped the user with no obvious escape path.

---

## Available Floating Windows

| Window | Description | Settings Location |
|--------|-------------|-------------------|
| Dream Clock | Elegant analog clock | Interaction -> Scenes |
| Simple Clock | Minimal digital clock | Interaction -> Scenes |
| Vinyl Cover | Rotating record cover when playing music | Interaction -> Playback |
| Floating Subtitle | Shows what you said and AI responses | Interaction -> Interface |
| Weather Overlay | Immersive weather UI | Interaction -> Scenes |
| Light Switch Overlay | Beautiful reminder when controlling lights by voice | Interaction -> Interface |
| Quick Entity Panel | Stream Deck style entity control panel | Interaction -> Interface |

---

## Dream Clock

An elegant analog clock floating window.

### How to Enable

1. Go to **Settings** -> **Interaction** -> **Scenes**
2. Turn on **Dream Clock**

### Home Assistant Control

```yaml
# Show
service: switch.turn_on
target:
  entity_id: switch.your_device_name_dream_clock_display

# Hide
service: switch.turn_off
target:
  entity_id: switch.your_device_name_dream_clock_display
```

---

## Simple Clock

A minimal digital clock floating window.

### How to Enable

1. Go to **Settings** -> **Interaction** -> **Scenes**
2. Turn on **Simple Clock**

### Double Tap Toggle

Enable **Double Tap Toggle** to double tap the bottom area to hide or show the clock, mapped to the HA entity.

### Home Assistant Control

```yaml
# Show
service: switch.turn_on
target:
  entity_id: switch.your_device_name_simple_clock_display

# Hide
service: switch.turn_off
target:
  entity_id: switch.your_device_name_simple_clock_display
```

---

## Vinyl Cover

Rotating record cover displayed when playing music.

### How to Enable

1. Go to **Settings** -> **Interaction** -> **Playback**
2. Turn on **Home Assistant Media Controls** or **Music Assistant Media Controls**
3. Set the HA Media Player entity ID for album art

See [Music Playback](Music-Playback) for details.

---

## Floating Subtitle

Shows what you said and Ava's AI response on screen as a floating subtitle.

### How to Enable

1. Go to **Settings** -> **Interaction** -> **Interface**
2. Turn on **Floating Subtitle**

---

## Weather Overlay

An immersive weather UI showing real-time weather information.

### How to Enable

1. Go to **Settings** -> **Interaction** -> **Scenes**
2. Turn on **Weather Display**
3. Set the **Weather Entity** (e.g., `weather.home`)

### Display Content

- Temperature
- Weather condition (sunny, cloudy, rain, etc.)
- Humidity
- Wind direction and speed
- Air quality
- Pressure

### Home Assistant Control

```yaml
# Show
service: switch.turn_on
target:
  entity_id: switch.your_device_name_weather_display

# Hide
service: switch.turn_off
target:
  entity_id: switch.your_device_name_weather_display
```

---

## Light Switch Overlay

Shows a beautiful reminder when you control lights by voice.

### How to Enable

1. Go to **Settings** -> **Interaction** -> **Interface**
2. Turn on **Light Switch Button Overlay**

### Voice Feedback Colors

Customize glow and ripple colors for each wake word:
- Custom color picker
- Default green (Wake Word 1) / blue (Wake Word 2)
- 7 rainbow presets (red through purple)

---

## Quick Entity Panel

A Stream Deck style entity quick control panel. See [Quick Entity](Quick-Entity) for details.

---

## Settings Summary

| Setting | Location | Description |
|---------|----------|-------------|
| Dream Clock | Interaction -> Scenes | Show analog clock |
| Simple Clock | Interaction -> Scenes | Show digital clock |
| Weather Display | Interaction -> Scenes | Show weather overlay |
| Vinyl Cover | Interaction -> Playback | Show music cover |
| Floating Subtitle | Interaction -> Interface | Show conversation text |
| Light Switch Overlay | Interaction -> Interface | Show light control reminder |
| Quick Entity Panel | Interaction -> Interface | Show entity control panel |

---

## FAQ

### Floating windows not showing?

1. Check overlay permission
2. Enable the specific floating window in settings
3. Try restarting service
4. Check if another overlay is blocking

### Weather overlay shows no data?

1. Set the Weather Entity in settings (e.g., `weather.home`)
2. Check if the entity exists in Home Assistant
3. Check network connection

### Dream Clock / Simple Clock overlap?

Only one clock should be enabled at a time. Disable one if both are showing.

---

*Back to [Home](Home)*
