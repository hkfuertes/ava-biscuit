# Permissions

Ava works without root, but some features benefit from elevated permissions. This page covers Shizuku, Root, and Device Admin setup.

---

## Overview

| Method | Purpose | Root Required |
|--------|---------|---------------|
| Shizuku | Elevated permissions without root | No |
| Root | Full system access | Yes |
| Device Admin | Lock screen without root | No |

---

## Shizuku

Shizuku is an open-source permission management tool that grants apps elevated permissions without root, using ADB or wireless debugging.

### Setup

1. Download and install [Shizuku](https://github.com/RikkaApps/Shizuku) from GitHub
2. Enable Shizuku via wireless debugging or ADB
3. Open Ava, go to **Settings** -> **Permissions** -> **Shizuku Authorization**
4. Tap **Authorize**

### Shizuku Status

| Status | Description |
|--------|-------------|
| Authorized | Shizuku is running and Ava has access |
| Waiting for authorization | Shizuku is running, tap Authorize |
| Not running | Start Shizuku first |
| Not installed | Download and install Shizuku |

### What Shizuku Enables

- Screen brightness control
- Display resolution adjustment
- Screen orientation lock
- Intent execution
- Other system-level operations

> **Note:** Shizuku requires Android 7.0 (API 23) or higher. On Android 5-6, the Shizuku card is auto-hidden.

### Auto-Start Shizuku on Reboot (Without Root)

The official Shizuku does not survive device reboots — it must be manually restarted each time. For kiosk devices and always-on panels, this is impractical. A community fork solves this:

1. Download and install the [Shizuku fork by thedjchi](https://github.com/thedjchi/Shizuku/releases) (or [pixincreate's fork](https://github.com/pixincreate/Shizuku/releases))
2. Open the forked Shizuku, go to **Settings** and enable **Start on boot**
3. Turn off battery optimizations for Shizuku when prompted
4. Set up Shizuku via wireless debugging (or ADB)
5. Connect the device to a PC with USB debugging enabled and run:

```bash
adb shell pm grant moe.shizuku.privileged.api android.permission.WRITE_SECURE_SETTINGS
```

6. Reboot the device — Shizuku will start automatically within a few seconds

This grants the forked Shizuku the `WRITE_SECURE_SETTINGS` permission, allowing it to modify system settings and auto-start on boot. After reboot, Ava will detect Shizuku automatically — no manual intervention needed.

> **Tip:** For kiosk devices that reboot on power loss (e.g., car head units, smart mirrors), this ensures Ava regains elevated permissions without someone physically tapping the screen.

---

## Root

Root access provides the most stable operation for 24/7 panel use.

### Root Status

Check root status in **Settings** -> **Permissions** -> **Root Status**:
- **Granted**: Root access available
- **Not granted**: Root not available or not granted

### What Root Enables

- Better background protection
- Boot scripts
- Screen control (lock/unlock)
- Reboot device
- Stable Bluetooth proxy on low-end devices
- Automatic permission handling for mods (e.g., Meta Portal Support)

### Recommended for

- Wall-mounted panels running 24/7
- Devices with aggressive battery optimization (MIUI, EMUI, ColorOS)
- Bluetooth proxy on low-end BLE chips (compatibility mode)
- Facebook Portal devices with Meta Portal Support mod

---

## Device Admin

Device Admin allows Ava to lock the screen without root.

### Setup

1. Go to **Settings** -> **Permissions**
2. Find **Device Admin**
3. Tap to enable
4. Confirm in system dialog

### What Device Admin Enables

- Lock screen control from Home Assistant
- Screen lock entity in HA

> **Note:** On some devices (e.g., Facebook Portal), Device Admin cannot be granted through the UI. Use ADB-assisted permission paths instead.

---

## ADB Permission Grants

For devices where the system UI fails to grant permissions normally (Android 8 and below, Facebook Portal, etc.):

### GeckoView Engine Package

```bash
adb shell appops set com.example.ava.gecko SYSTEM_ALERT_WINDOW allow
adb shell settings put system kill_background_services_list "com.example.ava,com.example.ava.gecko"
```

### Meta Portal Support

Use the provision.sh script:

```bash
cd ~/Downloads
chmod +x provision.sh
adb devices
./provision.sh com.example.ava
```

See [Mod Store](Mod-Store) for Meta Portal Support details.

---

## Permission Summary

| Permission | Purpose | Required |
|------------|---------|----------|
| Microphone | Voice input | Yes |
| Overlay | Display screensaver, notifications, floating windows | Yes |
| Foreground Service | Keep service running | Yes |
| Camera | Photos and video | Optional |
| Bluetooth | Presence detection and BLE proxy | Optional |
| Location | Bluetooth scanning | Optional |
| WRITE_SETTINGS | Screen brightness | Optional (Shizuku/Root) |
| WRITE_SECURE_SETTINGS | Display resolution | Optional (Shizuku/Root/ADB) |

---

## FAQ

### Shizuku not working?

1. Ensure Shizuku is installed and running
2. Enable via wireless debugging or ADB
3. Go to Settings -> Permissions -> Shizuku Authorization
4. Tap Authorize
5. If status shows "Not running", start Shizuku first

### Shizuku card not showing?

Shizuku requires Android 7.0 (API 23) or higher. On Android 5-6, the Shizuku card is auto-hidden.

### Root status shows "Not granted"?

1. Ensure the device is rooted
2. Grant root access to Ava in your root manager (Magisk, etc.)
3. Restart Ava

### Device Admin cannot be granted on Facebook Portal?

On some devices, Device Admin cannot be granted through the UI. Use ADB-assisted permission paths:
1. Install the Meta Portal Support mod from Mod Store
2. Use the provision.sh script with ADB
3. Or use Shizuku authorization

---

*Back to [Home](Home)*
