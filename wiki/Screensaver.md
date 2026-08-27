# Screensaver

The screensaver system displays beautiful content when your device is idle, while protecting the screen.

---

## Overview

When the device is idle for a period of time, Ava automatically displays a screensaver. Touch the screen to exit.

**Screensaver Types:**
- Xiaomi Wallpaper (recommended)
- Web Screensaver
- Image Screensaver
- Simple Clock (floating window, see [Floating Windows](Floating-Windows))

---

## Xiaomi Wallpaper

Xiaomi Wallpaper is Ava's built-in beautiful screensaver, mimicking Xiaomi phone's wallpaper style.

### Display Content

```
┌─────────────────────────────────────┐
│                                     │
│           12:34                     │
│                                     │
│     Thursday, January 9, 2026       │
│                                     │
│                                     │
│                                     │
└─────────────────────────────────────┘
```

### Features by Language

| Feature | Chinese Environment | English Environment |
|---------|--------------------|--------------------|
| Time | 24-hour format | 24-hour format |
| Date | 2026年1月9日 星期四 | Thursday, January 9, 2026 |
| Poetry | Shown (changes on refresh) | Hidden |

### How to Enable

1. Go to **Settings** -> **Screensaver**
2. Turn on **Screensaver** switch
3. Turn on **Xiaomi Wallpaper** switch
4. Set **Timeout** (how long before screensaver shows)

---

## Web Screensaver

Web screensaver can display any web content, such as weather, calendar, dashboards, etc.

### Use Cases

- Display Home Assistant dashboard
- Display weather website
- Display custom web page

### How to Set Up

1. Go to **Settings** -> **Screensaver**
2. Turn on **Screensaver** switch
3. Turn off **Xiaomi Wallpaper** switch
4. Enter web address in **Screensaver URL**
5. Set **Timeout**

> **Tip:** To use Home Assistant as screensaver, first enable Browser Display, enter your HA URL in Remote Browser URL and login to sync cookies, then the screensaver can auto-login to HA dashboard.

### Show Screensaver URL in HA

Enable **Show Screensaver URL** to expose the URL entity in Home Assistant.

---

## Image Screensaver

Image screensaver can display network images, supports MJPEG video streams.

### Supported Formats

- JPEG/JPG
- PNG
- GIF
- MJPEG video stream

### How to Set Up

Set image URL via Home Assistant service:

```yaml
service: esphome.your_device_name_screensaver_image
data:
  url: "http://example.com/image.jpg"
```

---

## Clock Wallpaper

### Wallpaper URL

Load a remote wallpaper image behind the simple clock.

1. Go to **Settings** -> **Screensaver**
2. Enter **Clock Wallpaper URL** (direct image URL)
3. Leave blank for plain black background

### Wallpaper Refresh

Reload the wallpaper at a set interval (in seconds). Default: 30 seconds.

### Dual Pane Wallpaper

Fetch the same wallpaper URL twice and show it in left/right split view. Useful for wide screens.

### Dark Wallpaper Overlay

Add a black translucent overlay above the wallpaper. When enabled, text shadow stays off for better readability.

---

## Screensaver Controls

### Timeout

Set how long before screensaver shows when idle.

| Option | Description |
|--------|-------------|
| 15 seconds | Quick screensaver |
| 30 seconds | Default |
| 60 seconds | Longer wait |
| 120 seconds | Much longer wait |
| 300 seconds | 5 minutes |

### Touch to Wake

Touch anywhere on screen to exit screensaver.

### Turn Off in Dark

When ambient light is very low (e.g., lights off at night), automatically turn off screen to save power.

**How to Enable:**
1. Go to **Settings** -> **Screensaver**
2. Turn on **Turn off in dark** switch

**How It Works:**
- Screen turns off when light below threshold (≤ 1.5 lx)
- Screen turns on when light restored (≥ 10 lx)
- Both transitions are debounced to prevent flickering

**Device-Specific Screen Control (Mod Hook):**

On devices where the standard Android screen-off API doesn't work (e.g., Echo Show with Fire OS), Ava delegates to a device support mod if one is installed and enabled.

**Data flow:**

```
darkOffEnabled + light sensor ≤ 1.5 lx
  → hide screensaver (remove KEEP_SCREEN_ON)
  → ModDeviceSupport.trySleepScreenForDark()
       → echo-show-support mod (if isSupported)
            → Shizuku setDisplayPower(0)
            → root input keyevent 223
            → root input keyevent 26
            → fallback: minimum brightness 10
  → if mod returns false → ScreenControlUtils.setScreenOn(false)
```

**Without a mod** or on unsupported devices, `trySleepScreenForDark` returns `false` immediately and Ava uses the standard `ScreenControlUtils` — identical to behavior without any mod installed.

### Echo Show Support Mod

The `echo-show-support` mod (v1.1.0+) adds Shizuku/root-based screen control for Echo Show devices (crown and similar) where the standard Android API cannot turn off the display.

**Requirements:**
- Mod installed and enabled
- Device must have **root** and/or **Shizuku** authorization
- Screensaver enabled + "Turn off in dark" enabled

**Enable via ADB (headless):**

```bash
adb shell am broadcast -a com.example.ava.ACTION_SET_MOD_ENABLED \
  --es mod_id echo-show-support --ez mod_enabled true
```

**Sleep strategy (dark detected):**

1. Check Shizuku or root availability — if neither, return `false` (fall back to core)
2. Shizuku `setDisplayPower(0)` — turns off display panel
3. Root `input keyevent 223` (KEYCODE_SLEEP)
4. Root `input keyevent 26` (KEYCODE_POWER) + verify `!isInteractive`
5. Fallback: set brightness to minimum (10)

**Wake strategy (light restored):**

1. Shizuku `setDisplayPower(2)` — turns on display panel
2. Root `input keyevent 26` (KEYCODE_POWER)
3. Restore cached brightness

**Limitations:**
- Complete darkness may still be affected by ROM-level Ambient Display or clock widgets
- The mod attempts `display power off` which is darker than brightness-only approaches
- Strategy updates can be deployed via mod update without changing the main APK

**Debug logging:**

```bash
adb logcat -s ScreensaverController EchoShowSupport ModDeviceSupport
```

### Proximity Wake

When someone approaches the device, automatically wake the screen.

**How to Enable:**
1. Go to **Settings** -> **Screensaver**
2. Turn on **Proximity Wake** switch

### Background Pause

Pause the screensaver when another app is in foreground. This prevents the screensaver from interfering with other apps.

---

## Settings Summary

| Setting | Location | Description | Default |
|---------|----------|-------------|---------|
| Enable Screensaver | Screensaver | Enable/disable screensaver | Off |
| Xiaomi Wallpaper | Screensaver | Use built-in Xiaomi Wallpaper | Off |
| Screensaver URL | Screensaver | Custom web screensaver address | Empty |
| Show Screensaver URL | Screensaver | Show URL entity in HA | Off |
| Show in HA | Screensaver | Show screensaver control in HA | Off |
| Timeout | Screensaver | How long before screensaver shows | 30s |
| Turn off in dark | Screensaver | Auto screen off in dark | Off |
| Background Pause | Screensaver | Pause when another app is foreground | Off |
| Proximity Wake | Screensaver | Auto wake when approached | Off |
| Clock Wallpaper URL | Screensaver | Remote wallpaper behind clock | Empty |
| Wallpaper Refresh | Screensaver | Reload interval in seconds | 30 |
| Dual Pane Wallpaper | Screensaver | Split view for wide screens | Off |
| Dark Wallpaper Overlay | Screensaver | Black translucent overlay | Off |

---

## Home Assistant Control

Screensaver is controlled via switch and text entities.

### Show/Hide Screensaver

```yaml
# Show screensaver
service: switch.turn_on
target:
  entity_id: switch.your_device_name_screensaver

# Hide screensaver
service: switch.turn_off
target:
  entity_id: switch.your_device_name_screensaver
```

### Set Screensaver URL

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_screensaver_url
data:
  value: "http://example.com/image.jpg"
```

---

## FAQ

### Screensaver not showing?

1. Check if screensaver switch is on
2. Check if overlay permission is granted
3. Check timeout setting
4. If using web screensaver, check if URL is correct

### Screensaver shows blank?

1. Check network connection
2. Check if URL is accessible
3. Try using built-in Xiaomi Wallpaper
4. Try switching render mode

### Light sensor not working?

1. Check if device has light sensor
2. Make sure sensor isn't blocked
3. Check permission settings

### Wallpaper not loading?

1. Check if Clock Wallpaper URL is a direct image URL
2. Check network connection
3. Check wallpaper refresh interval
4. Try a different image URL

---

*Back to [Home](Home)*
