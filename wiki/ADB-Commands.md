# ADB Commands

Ava exposes a set of ADB-controllable broadcast actions and shell commands for headless setup, automation, and remote control. These are useful for kiosk deployments, batch provisioning, and integrating Ava into existing device management workflows.

---

## Recommended ADB Tools

The raw `adb` command line works, but a GUI makes kiosk management far easier. These are the tools we recommend.

### AYA (Primary Recommendation)

[AYA](https://github.com/liriliri/aya) is an open-source cross-platform desktop app that wraps ADB in a clean GUI. Windows, macOS, and Linux are all supported.

- Screen mirror and remote control
- File explorer (push/pull without typing paths)
- Application manager (install/uninstall/grant permissions)
- Process monitor
- Layout inspector
- CPU, memory, and FPS monitor
- Logcat viewer with filtering
- Interactive shell

For Ava kiosk deployments, AYA is the fastest way to:
- Watch `logcat` while testing voice wake
- Push config files without `adb push` syntax
- Grant permissions visually
- Mirror the screen to verify overlay rendering

Download: [github.com/liriliri/aya/releases](https://github.com/liriliri/aya/releases)

### scrcpy

[scrcpy](https://github.com/Genymobile/scrcpy) is a lightweight screen mirroring tool. No GUI chrome, just the device screen in a window with low latency. Use it when you only need to see and interact with the device screen.

- Minimal resource usage
- Audio forwarding (Android 11+)
- Recording
- Copy-paste
- Physical keyboard injection

```bash
# macOS
brew install scrcpy

# Linux
apt install scrcpy

# Windows: download from GitHub releases
```

### QtScrcpy

[QtScrcpy](https://github.com/barry-ran/QtScrcpy) is a Qt-based GUI fork of scrcpy with extra features for batch device management.

- Multi-device simultaneous control
- Group management
- Screenshot and recording
- Custom keymapping

Useful when managing multiple Ava devices on a desk for testing.

### Built-in ADB

If you prefer the terminal, the Android SDK Platform Tools include `adb` and `fastboot`. This is what the rest of this page uses.

- macOS: `brew install --cask android-platform-tools`
- Linux: `apt install adb` or `pacman -S android-tools`
- Windows: download from [developer.android.com](https://developer.android.com/tools/releases/platform-tools)

---

## Prerequisites

- ADB installed on your computer (`adb` command available), or one of the GUI tools above
- Device connected via USB or network ADB
- USB debugging enabled on the device
- Ava installed and the package name known (default: `com.example.ava`)

### Verify Connection

```bash
adb devices
```

You should see your device listed. If not, check USB debugging is enabled in Developer Options.

### Find Your Package Name

If you installed a custom build, the package name may differ. Find it:

```bash
adb shell pm list packages | grep ava
```

The rest of this page uses `com.example.ava` as the default. Replace it with your actual package name if different.

---

## Voice Control Commands

These trigger voice actions remotely via ADB broadcast. The receiver is exported, so no root or Shizuku is needed.

### Manual Wake

Trigger a wake event as if the user said the wake word:

```bash
adb shell am broadcast -a com.example.ava.ACTION_WAKE
```

### Stop Voice Session

Stop the current voice session (equivalent to saying "stop"):

```bash
adb shell am broadcast -a com.example.ava.ACTION_STOP
```

### Toggle Microphone Mute

```bash
adb shell am broadcast -a com.example.ava.ACTION_TOGGLE_MIC
```

### Mute Microphone

```bash
adb shell am broadcast -a com.example.ava.ACTION_MUTE_MIC
```

### Unmute Microphone

```bash
adb shell am broadcast -a com.example.ava.ACTION_UNMUTE_MIC
```

---

## Service Control Commands

### Start Voice Satellite Service

```bash
adb shell am broadcast -a com.example.ava.ACTION_START_SERVICE
```

### Stop Voice Satellite Service

```bash
adb shell am broadcast -a com.example.ava.ACTION_STOP_SERVICE
```

### Start Service Directly (Root)

If you have root, you can start the service directly without the broadcast receiver:

```bash
adb shell su -c "am startservice -n com.example.ava/com.example.ava.services.VoiceSatelliteService"
```

---

## Permission Grant Commands

These broadcast actions trigger Ava to attempt granting itself permissions via Shizuku or root. They require either Shizuku or root to be active on the device.

### Grant Record Audio (Microphone)

```bash
adb shell am broadcast -a com.example.ava.ACTION_GRANT_RECORD_AUDIO
```

Equivalent direct ADB command:
```bash
adb shell pm grant com.example.ava android.permission.RECORD_AUDIO
```

### Grant Camera

```bash
adb shell am broadcast -a com.example.ava.ACTION_GRANT_CAMERA
```

Equivalent direct ADB command:
```bash
adb shell pm grant com.example.ava android.permission.CAMERA
```

### Grant Location (GPS)

```bash
adb shell am broadcast -a com.example.ava.ACTION_GRANT_LOCATION
```

Equivalent direct ADB commands:
```bash
adb shell pm grant com.example.ava android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.example.ava android.permission.ACCESS_COARSE_LOCATION
adb shell settings put secure location_mode 3
adb shell settings put secure location_providers_allowed +gps,network
```

### Grant Bluetooth

```bash
adb shell am broadcast -a com.example.ava.ACTION_GRANT_BLUETOOTH
```

Equivalent direct ADB commands:
```bash
adb shell pm grant com.example.ava android.permission.BLUETOOTH_SCAN
adb shell pm grant com.example.ava android.permission.BLUETOOTH_CONNECT
adb shell pm grant com.example.ava android.permission.BLUETOOTH_ADVERTISE
adb shell pm grant com.example.ava android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.example.ava android.permission.ACCESS_FINE_LOCATION
adb shell settings put secure location_mode 3
```

### Grant Notifications (Android 13+)

```bash
adb shell am broadcast -a com.example.ava.ACTION_GRANT_NOTIFICATIONS
```

Equivalent direct ADB command:
```bash
adb shell pm grant com.example.ava android.permission.POST_NOTIFICATIONS
```

### Grant Overlay (Floating Windows)

```bash
adb shell am broadcast -a com.example.ava.ACTION_GRANT_OVERLAY
```

Equivalent direct ADB command:
```bash
adb shell appops set com.example.ava SYSTEM_ALERT_WINDOW allow
```

### Grant Write Settings

```bash
adb shell am broadcast -a com.example.ava.ACTION_GRANT_WRITE_SETTINGS
```

Equivalent direct ADB command:
```bash
adb shell appops set com.example.ava WRITE_SETTINGS allow
```

### Grant Secure Settings

```bash
adb shell am broadcast -a com.example.ava.ACTION_GRANT_SECURE_SETTINGS
```

Equivalent direct ADB command:
```bash
adb shell pm grant com.example.ava android.permission.WRITE_SECURE_SETTINGS
```

### Grant Install Packages

```bash
adb shell am broadcast -a com.example.ava.ACTION_GRANT_INSTALL_PACKAGES
```

Equivalent direct ADB command:
```bash
adb shell appops set com.example.ava REQUEST_INSTALL_PACKAGES allow
```

### Activate Device Admin

```bash
adb shell am broadcast -a com.example.ava.ACTION_ACTIVATE_DEVICE_ADMIN
```

---

## All Permissions in One Script

For headless kiosk provisioning, run all permission grants at once via direct ADB commands (no root needed for standard permissions, root/Shizuku needed for appops):

```bash
#!/bin/bash
PKG=com.example.ava

# Standard permissions (no root needed)
adb shell pm grant $PKG android.permission.RECORD_AUDIO
adb shell pm grant $PKG android.permission.CAMERA
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS

# AppOps (needs root or Shizuku)
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
adb shell appops set $PKG WRITE_SETTINGS allow
adb shell appops set $PKG REQUEST_INSTALL_PACKAGES allow

# Secure settings (needs root or Shizuku or ADB)
adb shell pm grant $PKG android.permission.WRITE_SECURE_SETTINGS

# Bluetooth (Android 12+)
adb shell pm grant $PKG android.permission.BLUETOOTH_SCAN
adb shell pm grant $PKG android.permission.BLUETOOTH_CONNECT
adb shell pm grant $PKG android.permission.BLUETOOTH_ADVERTISE

# Location mode
adb shell settings put secure location_mode 3
adb shell settings put secure location_providers_allowed +gps,network

# Battery optimization whitelist (needs root or Shizuku)
adb shell dumpsys deviceidle whitelist +$PKG

echo "All permissions granted for $PKG"
```

---

## Screen Control Commands

These are used internally by Ava when root or Shizuku is available. You can also run them directly via ADB.

### Power On Screen (Keycode 26 = POWER)

```bash
adb shell input keyevent 26
```

### Power Off Screen (Keycode 223 = SLEEP)

```bash
adb shell input keyevent 223
```

### Wake Screen (Keycode 224 = WAKEUP)

```bash
adb shell input keyevent 224
```

### Set Screen Brightness

```bash
adb shell settings put system screen_brightness 128
```

Value range: 0-255. Ava uses this when root/Shizuku is available for smooth brightness transitions.

---

## Boot Script (Root Only)

Ava can install a Magisk-style boot script that automatically starts the app and service on boot. This is useful for kiosk devices that should launch Ava without user interaction.

### Install Boot Script

This is done from within Ava's settings (Settings -> Voice -> Boot Start). The script is installed to `/data/adb/service.d/ava_boot.sh` and does the following on boot:

1. Wait 45 seconds for the system to stabilize
2. Launch Ava's MainActivity
3. Wait 20 seconds
4. Start VoiceSatelliteService (with retry, up to 2 attempts)
5. Verify the process is running

The script content:
```bash
#!/system/bin/sh
sleep 45
/system/bin/am start -n com.example.ava/.MainActivity
sleep 20
for i in 1 2; do
  /system/bin/am startservice -n com.example.ava/com.example.ava.services.VoiceSatelliteService
  sleep 5
  pgrep -f com.example.ava > /dev/null && break
done
```

### Remove Boot Script

```bash
adb shell su -c "rm /data/adb/service.d/ava_boot.sh"
```

---

## Battery Optimization Whitelist

Prevent Android from killing Ava in the background:

### Via ADB (needs root or Shizuku)

```bash
adb shell dumpsys deviceidle whitelist +com.example.ava
```

### Via Ava Broadcast

```bash
adb shell am broadcast -a com.example.ava.ACTION_START_SERVICE
```

Ava automatically attempts to whitelist itself when the service starts, if root or Shizuku is available.

---

## Gecko Engine Setup (Android 7 Kiosk Devices)

On Android 7 kiosk devices using the Gecko engine pack, Ava automatically applies these shell commands when root or Shizuku is available:

### Grant Overlay to Gecko Pack

```bash
adb shell appops set com.example.ava.gecko SYSTEM_ALERT_WINDOW allow
```

### Add to Kill-List Whitelist

Prevents the vendor ROM from killing the Gecko engine in the background:

```bash
adb shell settings put system kill_background_services_list "com.example.ava,com.example.ava.gecko"
```

Ava does this automatically when it detects the Gecko pack is installed. You only need to run these manually if the automatic setup fails.

### Check Gecko Pack Installation

```bash
adb shell pm list packages | grep ava
```

Check version numbers:

```bash
adb shell dumpsys package com.example.ava | grep -E "versionName|versionCode"
adb shell dumpsys package com.example.ava.gecko | grep -E "versionName|versionCode"
```

### Show Browser (Manual Handoff Test)

Same signal the main app sends to the Gecko pack. Useful when the UI is stuck but ADB still works:

```bash
adb shell am start \
  -a com.example.ava.gecko.action.SHOW_BROWSER \
  -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge \
  --es url "https://homeassistant.local:8123/lovelace/0"
```

Replace the URL with your HA dashboard URL. The overlay stays visible until you send HIDE or DESTROY.

### Hide Browser (Overlay Off, Renderer Kept Alive)

```bash
adb shell am start \
  -a com.example.ava.gecko.action.HIDE_BROWSER \
  -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge
```

### Destroy Browser (Full Teardown)

```bash
adb shell am start \
  -a com.example.ava.gecko.action.DESTROY_BROWSER \
  -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge
```

### Navigate to Another URL (Browser Already Open)

```bash
adb shell am start \
  -a com.example.ava.gecko.action.NAVIGATE \
  -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge \
  --es url "https://example.com/new-page"
```

### Force Reload Current Page

```bash
adb shell am start \
  -a com.example.ava.gecko.action.FORCE_REFRESH \
  -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge
```

### Clear Browsing Data

```bash
adb shell am start \
  -a com.example.ava.gecko.action.CLEAR_CACHE \
  -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge
```

### Restore Browser After Returning from Settings

```bash
adb shell am start \
  -a com.example.ava.gecko.action.RESTORE_FROM_SETTINGS \
  -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge
```

### Refresh Dark Mode

```bash
adb shell am start \
  -a com.example.ava.gecko.action.REFRESH_DARK_MODE \
  -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge
```

### Uninstall Gecko Engine Pack Only

Keeps the main app and all settings intact:

```bash
adb shell pm uninstall com.example.ava.gecko
```

Then reinstall via Ava: Browser settings -> Gecko engine -> download and install.

### Force-Stop Gecko Process

```bash
adb shell am force-stop com.example.ava.gecko
```

The main app will restart it on the next browser show signal.

### Gecko Debug Logcat

```bash
adb logcat -s BrowserEngine WebViewService VoiceSatelliteService GeckoBrowserBridge
```

### Check Crash Log

```bash
adb shell cat /sdcard/ava_crash_log.txt
```

---

## Echo Show Specific Commands

On Amazon Echo Show devices, the overlay permission cannot be granted through the standard UI. Ava handles this automatically when root is available:

```bash
adb shell su -c "appops set com.example.ava SYSTEM_ALERT_WINDOW allow"
```

Echo Show devices also have a minimum brightness of 10 (not 0) to prevent the screen from going completely black. Ava handles this automatically.

---

## Notification Scene Commands

Notification scenes are triggered via Home Assistant's `select` entity, but the underlying service also accepts ADB commands. Note: the `NotificationOverlayService` is not exported, so these only work from `adb shell` on the device itself (not from a remote computer via ADB).

### Show Scene by ID

```bash
adb shell am startservice -n com.example.ava/com.example.ava.services.NotificationOverlayService \
  -a com.example.ava.SHOW_NOTIFICATION_SCENE \
  --es scene_id "doorbell"
```

### Show Scene by Title

```bash
adb shell am startservice -n com.example.ava/com.example.ava.services.NotificationOverlayService \
  -a com.example.ava.SHOW_NOTIFICATION_SCENE_TITLE \
  --es scene_title "Someone rings the doorbell"
```

### Show Scene by Index

```bash
adb shell am startservice -n com.example.ava/com.example.ava.services.NotificationOverlayService \
  -a com.example.ava.SHOW_NOTIFICATION_SCENE_INDEX \
  --ei scene_index 0
```

### Hide Notification

```bash
adb shell am startservice -n com.example.ava/com.example.ava.services.NotificationOverlayService \
  -a com.example.ava.HIDE_NOTIFICATION
```

---

## Browser and Overlay Commands

### Sync Browser Visibility

Used internally by the Gecko engine pack to sync the browser display state:

```bash
adb shell am broadcast -a com.example.ava.action.SYNC_BROWSER_VISIBLE --ez browser_visible true
```

### Reassert Foreground Overlays

When overlays lose their z-order (common after screen rotation or system UI changes):

```bash
adb shell am broadcast -a com.example.ava.action.REASSERT_FOREGROUND_OVERLAYS
```

### Request Sidebar Settings

Trigger the Gecko pack to request sidebar settings from the host app:

```bash
adb shell am broadcast -a com.example.ava.action.REQUEST_SIDEBAR_SETTINGS
```

### Send Sidebar Command

```bash
adb shell am broadcast -a com.example.ava.action.SIDEBAR_COMMAND \
  --es command "toggle_browser" \
  --es payload ""
```

---

## Headless Settings Commands

Apply Ava settings remotely via ADB broadcast — no UI interaction needed. Supports JSON patch, file-based, and single-value modes. Useful for kiosk provisioning and automation.

### Apply Settings from JSON String

Merge a partial JSON patch into Ava's settings stores. Only specified keys are overwritten; others remain unchanged.

```bash
adb shell am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS \
  --es settings_json '{"microphone":{"voicePrintEnabled":true},"sendspin":{"enabled":true,"serverUrl":"ws://192.168.1.10:8927"}}'
```

**Supported top-level store keys:**

| Store Key | Description |
|-----------|-------------|
| `microphone` | Microphone settings (noise suppression, echo cancellation, gain, voiceprint, etc.) |
| `player` | Media player settings |
| `experimental` | Experimental features |
| `sendspin` | SendSpin (Music Assistant) settings |
| `voice_channel` | Voice channel settings |
| `screensaver` | Screensaver settings |
| `voice_satellite` | Voice satellite settings |

### Apply Settings from File

Push a JSON file to the device, then apply:

```bash
adb push ava_headless.json /sdcard/ava_headless.json
adb shell am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS \
  --es settings_file /sdcard/ava_headless.json
```

Allowed file locations: app data dir, external app dir, `/sdcard/`, `/storage/emulated/0/`. Paths containing `..` are rejected.

### Apply Single Setting by Path

Set one value using a dot path (`store.field`):

```bash
# Boolean
adb shell am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS \
  --es setting_path microphone.voicePrintEnabled --ez setting_bool true

# Integer
adb shell am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS \
  --es setting_path sendspin.volumeLevel --ei setting_int 80

# Float
adb shell am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS \
  --es setting_path microphone.gain --ef setting_float 1.5

# String
adb shell am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS \
  --es setting_path sendspin.serverUrl --es setting_string "ws://192.168.1.10:8927"

# JSON value (lists, maps, enums)
adb shell am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS \
  --es setting_path microphone.wakeWords --es setting_json '["okay_nabu","hey_jarvis"]'
```

Alternatively, use `setting_store` + `setting_key` instead of `setting_path`:

```bash
adb shell am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS \
  --es setting_store microphone --es setting_key voicePrintEnabled --ez setting_bool true
```

### Restart Control

By default, Ava restarts the voice satellite and/or SendSpin session automatically when settings changes require it. Control this behavior:

| Extra | Type | Default | Description |
|-------|------|---------|-------------|
| `no_restart` | Boolean | `false` | Skip all restarts |
| `restart_satellite` | Boolean | auto | Force restart satellite (or skip if `false`) |
| `restart_sendspin` | Boolean | auto | Force restart SendSpin (or skip if `false`) |

```bash
# Apply settings without restarting
adb shell am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS \
  --es settings_json '{"microphone":{"gain":2.0}}' --ez no_restart true

# Force satellite restart even if store doesn't normally trigger it
adb shell am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS \
  --es settings_json '{"player":{"volume":50}}' --ez restart_satellite true
```

**Auto-restart rules:**
- `microphone`, `experimental`, `voice_channel`, `voice_satellite` changes → satellite restart
- `sendspin` changes → SendSpin session restart
- `player`, `screensaver` changes → no restart needed

---

## Mod Control Commands

Enable, disable, and reload mods remotely via ADB broadcast. Useful for headless provisioning and automation.

### Enable a Mod

```bash
adb shell am broadcast -a com.example.ava.ACTION_SET_MOD_ENABLED \
  --es mod_id echo_dot_led --ez mod_enabled true
```

### Disable a Mod

```bash
adb shell am broadcast -a com.example.ava.ACTION_SET_MOD_ENABLED \
  --es mod_id echo_dot_led --ez mod_enabled false
```

### Disable a Mod Without Restarting Satellite

```bash
adb shell am broadcast -a com.example.ava.ACTION_SET_MOD_ENABLED \
  --es mod_id echo_dot_led --ez mod_enabled false --ez no_restart true
```

### Reload a Specific Mod

Forces ClassLoader refresh by disabling and re-enabling the mod. Only works on enabled mods.

```bash
adb shell am broadcast -a com.example.ava.ACTION_RELOAD_MOD \
  --es mod_id echo_dot_led
```

### Reload All Enabled Mods

```bash
adb shell am broadcast -a com.example.ava.ACTION_RELOAD_MOD
```

### Reload Without Restarting Satellite

```bash
adb shell am broadcast -a com.example.ava.ACTION_RELOAD_MOD --ez no_restart true
```

**Behavior:**
- `SET_MOD_ENABLED` refreshes the registry from disk first, so manual `adb push` of mod files into `/data/data/com.example.ava/files/mods/` is picked up
- `RELOAD_MOD` disables and re-enables the mod to force `DexClassLoader` refresh
- Unless `no_restart=true`, the voice satellite service restarts automatically
- If the service is not running, changes apply on next start

See [Mod Store](Mod-Store) for available mod IDs and [Mod Development](Mod-Store-Development) for the full mod API.

---

## Update Checker Commands

Trigger the in-app update dialog remotely via ADB. Useful for kiosk devices where the screen is not easily accessible.

### Check for Update (Broadcast)

Triggers the update check and pops up the update dialog if a new version is available:

```bash
adb shell am broadcast -a com.example.ava.ACTION_CHECK_UPDATE -n com.example.ava/com.example.ava.receivers.AvaControlReceiver
```

### Show Update Dialog (Activity)

Directly launches the update dialog via activity intent:

```bash
adb shell am start -a com.example.ava.action.SHOW_UPDATE -n com.example.ava/.MainActivity
```

### Show Update Dialog (Extra)

Alternative method using an extra flag:

```bash
adb shell am start -n com.example.ava/.MainActivity --ez show_update true
```

All three methods achieve the same result — the update dialog appears on screen, showing the latest version info and a download button if an update is available.

---

## Debugging and Diagnostics

### Check Ava Logs

```bash
adb logcat -s VoiceSatelliteService:AvaControlReceiver:NotificationOverlay:SendspinManager:Ava
```

### Check Granted Permissions

```bash
adb shell dumpsys package com.example.ava | grep permission
```

### Check AppOps Status

```bash
adb shell appops get com.example.ava
```

### Check Battery Whitelist

```bash
adb shell dumpsys deviceidle whitelist | grep ava
```

### Check if Service is Running

```bash
adb shell dumpsys activity services com.example.ava
```

### Force Stop Ava

```bash
adb shell am force-stop com.example.ava
```

### Clear Ava Data (Factory Reset)

```bash
adb shell pm clear com.example.ava
```

---

## Quick Provisioning Script

For a new kiosk device, here is the fastest path to a fully working Ava:

```bash
#!/bin/bash
PKG=com.example.ava
ADB="adb"

echo "=== Ava Quick Provisioning ==="

# 1. Install Ava APK
$ADB install -r ava.apk

# 2. Grant all permissions
$ADB shell pm grant $PKG android.permission.RECORD_AUDIO
$ADB shell pm grant $PKG android.permission.CAMERA
$ADB shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
$ADB shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
$ADB shell pm grant $PKG android.permission.POST_NOTIFICATIONS
$ADB shell pm grant $PKG android.permission.WRITE_SECURE_SETTINGS
$ADB shell pm grant $PKG android.permission.BLUETOOTH_SCAN
$ADB shell pm grant $PKG android.permission.BLUETOOTH_CONNECT
$ADB shell pm grant $PKG android.permission.BLUETOOTH_ADVERTISE

# 3. Grant AppOps (needs ADB shell, no root)
$ADB shell appops set $PKG SYSTEM_ALERT_WINDOW allow
$ADB shell appops set $PKG WRITE_SETTINGS allow

# 4. Battery whitelist
$ADB shell dumpsys deviceidle whitelist +$PKG

# 5. Location mode
$ADB shell settings put secure location_mode 3

# 6. Start the service
$ADB shell am broadcast -a com.example.ava.ACTION_START_SERVICE

echo "=== Provisioning Complete ==="
echo "Ava should now appear in Home Assistant ESPHome integrations."
```

---

## Command Reference Table

| Action | ADB Command | Needs Root/Shizuku |
|--------|-------------|---------------------|
| Wake voice | `am broadcast -a com.example.ava.ACTION_WAKE` | No |
| Stop voice | `am broadcast -a com.example.ava.ACTION_STOP` | No |
| Toggle mic | `am broadcast -a com.example.ava.ACTION_TOGGLE_MIC` | No |
| Mute mic | `am broadcast -a com.example.ava.ACTION_MUTE_MIC` | No |
| Unmute mic | `am broadcast -a com.example.ava.ACTION_UNMUTE_MIC` | No |
| Start service | `am broadcast -a com.example.ava.ACTION_START_SERVICE` | No |
| Stop service | `am broadcast -a com.example.ava.ACTION_STOP_SERVICE` | No |
| Grant mic | `am broadcast -a com.example.ava.ACTION_GRANT_RECORD_AUDIO` | Yes |
| Grant camera | `am broadcast -a com.example.ava.ACTION_GRANT_CAMERA` | Yes |
| Grant location | `am broadcast -a com.example.ava.ACTION_GRANT_LOCATION` | Yes |
| Grant bluetooth | `am broadcast -a com.example.ava.ACTION_GRANT_BLUETOOTH` | Yes |
| Grant notifications | `am broadcast -a com.example.ava.ACTION_GRANT_NOTIFICATIONS` | Yes |
| Grant overlay | `am broadcast -a com.example.ava.ACTION_GRANT_OVERLAY` | Yes |
| Grant write settings | `am broadcast -a com.example.ava.ACTION_GRANT_WRITE_SETTINGS` | Yes |
| Grant secure settings | `am broadcast -a com.example.ava.ACTION_GRANT_SECURE_SETTINGS` | Yes |
| Grant install packages | `am broadcast -a com.example.ava.ACTION_GRANT_INSTALL_PACKAGES` | Yes |
| Activate device admin | `am broadcast -a com.example.ava.ACTION_ACTIVATE_DEVICE_ADMIN` | No |
| Screen on | `input keyevent 26` | No (via ADB) |
| Screen off | `input keyevent 223` | No (via ADB) |
| Screen wake | `input keyevent 224` | No (via ADB) |
| Set brightness | `settings put system screen_brightness N` | No (via ADB) |
| Battery whitelist | `dumpsys deviceidle whitelist +com.example.ava` | Yes |
| Boot script install | Via Ava settings (Root) | Yes |
| Boot script remove | `rm /data/adb/service.d/ava_boot.sh` | Yes |
| Check for update | `am broadcast -a com.example.ava.ACTION_CHECK_UPDATE` | No |
| Show update dialog | `am start -a com.example.ava.action.SHOW_UPDATE -n com.example.ava/.MainActivity` | No |
| Show update dialog (extra) | `am start -n com.example.ava/.MainActivity --ez show_update true` | No |
| Gecko: show browser | `am start -a com.example.ava.gecko.action.SHOW_BROWSER -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge --es url "URL"` | No |
| Gecko: hide browser | `am start -a com.example.ava.gecko.action.HIDE_BROWSER -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge` | No |
| Gecko: destroy browser | `am start -a com.example.ava.gecko.action.DESTROY_BROWSER -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge` | No |
| Gecko: navigate | `am start -a com.example.ava.gecko.action.NAVIGATE -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge --es url "URL"` | No |
| Gecko: force refresh | `am start -a com.example.ava.gecko.action.FORCE_REFRESH -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge` | No |
| Gecko: clear cache | `am start -a com.example.ava.gecko.action.CLEAR_CACHE -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge` | No |
| Gecko: restore from settings | `am start -a com.example.ava.gecko.action.RESTORE_FROM_SETTINGS -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge` | No |
| Gecko: refresh dark mode | `am start -a com.example.ava.gecko.action.REFRESH_DARK_MODE -n com.example.ava.gecko/com.example.ava.GeckoBrowserBridge` | No |
| Gecko: uninstall pack | `pm uninstall com.example.ava.gecko` | No |
| Gecko: force-stop | `am force-stop com.example.ava.gecko` | No |
| Gecko: grant overlay | `appops set com.example.ava.gecko SYSTEM_ALERT_WINDOW allow` | No (via ADB) |
| Sync browser visible | `am broadcast -a com.example.ava.action.SYNC_BROWSER_VISIBLE --ez visible true` | No |
| Reassert overlays | `am broadcast -a com.example.ava.action.REASSERT_FOREGROUND_OVERLAYS` | No |
| Request sidebar settings | `am broadcast -a com.example.ava.action.REQUEST_SIDEBAR_SETTINGS` | No |
| Sidebar command | `am broadcast -a com.example.ava.action.SIDEBAR_COMMAND --es command "CMD" --es payload ""` | No |
| Apply settings (JSON) | `am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS --es settings_json '{"microphone":{"gain":2.0}}'` | No |
| Apply settings (file) | `am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS --es settings_file /sdcard/ava_headless.json` | No |
| Apply settings (single) | `am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS --es setting_path microphone.voicePrintEnabled --ez setting_bool true` | No |
| Enable mod | `am broadcast -a com.example.ava.ACTION_SET_MOD_ENABLED --es mod_id MOD_ID --ez mod_enabled true` | No |
| Disable mod | `am broadcast -a com.example.ava.ACTION_SET_MOD_ENABLED --es mod_id MOD_ID --ez mod_enabled false` | No |
| Reload mod | `am broadcast -a com.example.ava.ACTION_RELOAD_MOD --es mod_id MOD_ID` | No |
| Reload all mods | `am broadcast -a com.example.ava.ACTION_RELOAD_MOD` | No |

---

*Back to [Home](Home)*
