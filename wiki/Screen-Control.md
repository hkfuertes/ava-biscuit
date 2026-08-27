# Screen Control

Ava provides screen control features including brightness, orientation, keep screen on, and display resolution adjustment.

---

## Overview

Screen control features allow you to manage the display remotely from Home Assistant or locally from Ava settings.

---

## Keep Screen On

Prevent the screen from dimming or turning off.

### How to Enable

1. Go to **Settings** -> **Device Controls** -> **Display**
2. Turn on **Keep Screen On**

---

## Screen Brightness

Sync screen brightness bidirectionally between Ava and Home Assistant.

### How to Enable

1. Go to **Settings** -> **Device Controls** -> **Display**
2. Turn on **Screen Brightness**
3. Brightness syncs bidirectionally with HA

### Home Assistant Control

```yaml
service: number.set_value
target:
  entity_id: number.your_device_name_screen_brightness
data:
  value: 128  # 0-255
```

---

## Screen Power Controls

Show screen toggle and screen lock entities in Home Assistant.

### How to Enable

1. Go to **Settings** -> **Device Controls** -> **Display**
2. Turn on **Screen Power Controls**

### Home Assistant Entities

| Entity | Description |
|--------|-------------|
| `switch.your_device_name_screen_toggle` | Toggle screen on/off |
| `switch.your_device_name_lock_screen` | Lock display |

### Lock Screen Without Root

Ava can lock the screen without root using Device Admin. See [Permissions](Permissions) for setup.

---

## Force Orientation

Lock screen orientation to portrait, landscape, or follow system.

### How to Enable

1. Go to **Settings** -> **Device Controls** -> **Display**
2. Turn on **Force Orientation**
3. Choose orientation mode

| Mode | Description |
|------|-------------|
| Portrait | Lock to portrait |
| Landscape | Lock to landscape |
| Follow System | Use system setting |

---

## Display Resolution

Adjust display resolution (requires ADB or Shizuku).

### How to Set

1. Go to **Settings** -> **Device Controls** -> **Display**
2. Find **Display Resolution**
3. Set the desired resolution

> **Note:** This requires WRITE_SECURE_SETTINGS permission. Use Shizuku or ADB to grant. If it fails, authorize WRITE_SECURE_SETTINGS.

---

## Display Scale

Adjust the display scale. Lower value shows more content.

### How to Set

1. Go to **Settings** -> **Device Controls** -> **Display**
2. Find **Display Scale**
3. Adjust the slider

---

## Auto Unlock

Auto-unlock the swipe lock (experimental).

### How to Enable

1. Go to **Settings** -> **Device Controls** -> **Display**
2. Turn on **Auto Unlock Swipe Lock**

---

## Settings Summary

| Setting | Location | Description | Default |
|---------|----------|-------------|---------|
| Keep Screen On | Display | Prevent screen from dimming | Off |
| Screen Brightness | Display | Sync brightness bidirectionally | Off |
| Screen Power Controls | Display | Show screen toggle and lock in HA | Off |
| Force Orientation | Display | Lock screen orientation | Off |
| Orientation Mode | Display | Portrait/Landscape/Follow System | Follow System |
| Display Resolution | Display | Adjust resolution (requires ADB) | - |
| Display Scale | Display | Lower shows more content | - |
| Auto Unlock | Display | Auto-unlock swipe lock | Off |

---

## FAQ

### Brightness not syncing?

1. Enable Screen Brightness in settings
2. Check WRITE_SETTINGS permission
3. Check if device supports brightness control

### Screen won't lock?

1. Enable Screen Power Controls
2. Set up Device Admin for lock without root
3. Or use Shizuku/Root for lock control

### Display resolution change failed?

1. This requires WRITE_SECURE_SETTINGS permission
2. Set up Shizuku (recommended) or ADB
3. See [Permissions](Permissions) for setup guide

### Force orientation not working?

1. Enable Force Orientation
2. Choose the desired mode
3. Some devices may override orientation settings

---

*Back to [Home](Home)*
