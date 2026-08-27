# System Requirements

This page lists the system requirements for running Ava.

---

## Android Device Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| Android Version | 5.0 (API 21) | 10.0 or higher |
| CPU Architecture | armeabi-v7a | arm64-v8a |
| RAM | 1GB | 2GB or higher |
| Storage | 100MB | 200MB |

Ava supports Android 5-16, including 32-bit devices. Cheap tablets and old phones work reliably.

### Supported Languages

| Language | Status |
|----------|--------|
| English | Full |
| Chinese | Full |
| Russian | Full |
| Portuguese | Full |
| Vietnamese | Full |
| German | Full (added in 0.5.2) |

System language automatically selects the UI language.

---

## Network Requirements

| Requirement | Description |
|-------------|-------------|
| WiFi | Device must be on same LAN as Home Assistant |
| Port | 6053 (ESPHome default port) must be accessible |
| Stability | Stable WiFi connection recommended |

---

## Home Assistant Requirements

| Component | Description |
|-----------|-------------|
| Home Assistant | Must be installed and running |
| ESPHome Integration | Must be installed in HA |
| Voice Assistant | Voice assistant pipeline required (optional) |

### Recommended Voice Assistant Components

| Component | Purpose | Description |
|-----------|---------|-------------|
| Whisper | Speech-to-Text | Runs locally, privacy protected |
| Piper | Text-to-Speech | Runs locally, fast |
| Home Assistant Conversation Agent | Intent Processing | Built-in, no extra config |

---

## Permission Requirements

### Required Permissions

| Permission | Purpose |
|------------|---------|
| RECORD_AUDIO | Microphone recording for voice input |
| SYSTEM_ALERT_WINDOW | Overlay for screensaver, notifications, floating windows |
| FOREGROUND_SERVICE | Keep service running |
| INTERNET | Network access for HA communication |
| WAKE_LOCK | Keep device awake |

### Optional Permissions

| Permission | Purpose |
|------------|---------|
| CAMERA | Camera features (photos, video, face detection) |
| BLUETOOTH | Bluetooth presence detection and BLE proxy |
| ACCESS_FINE_LOCATION | Required for Bluetooth scanning |
| WRITE_SETTINGS | Adjust screen brightness |

### Elevated Permissions (Optional)

Ava works without root, but some features benefit from elevated permissions:

| Method | Purpose | Description |
|--------|---------|-------------|
| Shizuku | Elevated permissions without root | Screen control, display resolution, intent execution |
| Root | Full system access | Boot scripts, screen control, reboot, background protection |
| Device Admin | Lock screen without root | Screen lock control |

See [Permissions](Permissions) for detailed setup guide.

---

## Recommended Devices

### Devices Suitable as Control Panels

| Device Type | Pros | Cons |
|-------------|------|------|
| Old phones | Cheap, easy to obtain | Small screen |
| Tablets | Large screen, good display | Needs fixed placement |
| Touch Screen Speakers | Good audio, high quality microphone | Dedicated device |
| Car head units | Built-in screen | Limited compatibility |
| Smart mirrors | Integrated display | Custom setup |
| Echo Show / Portal | Built-in screen and mic | Limited Android version |

### Device Selection Tips

1. **Screen Size**: 7-10 inches is ideal for control panels
2. **Microphone**: Ensure good microphone quality for voice recognition
3. **Speaker**: Ensure acceptable speaker quality
4. **Power**: Keep device plugged in for 24/7 operation

---

## Device Capability Detection

Ava automatically detects device capabilities and adjusts features based on hardware:

| Detection | Purpose |
|-----------|--------|
| Camera | Auto-detect front/back cameras |
| Light Sensor | Light sensor screen off feature |
| Proximity Sensor | Proximity sensor wake feature |
| Environment Sensors | Temperature/humidity/pressure data |
| BLE Capabilities | Bluetooth proxy capability tier |

---

## Known Compatibility Issues

| Issue | Affected Devices | Solution |
|-------|-----------------|----------|
| WebView rendering issues | Some old devices | Switch to software rendering or GeckoView engine |
| Inaccurate wake word detection | Devices with poor microphones | Speak closer to device, adjust sensitivity |
| Overlay permission | Some custom ROMs | Manually grant in settings |
| Bluetooth proxy limited | Low-end BLE chips | Compatibility mode, root recommended |
| Background service killed | MIUI, EMUI, ColorOS | Disable battery optimization, see [FAQ](FAQ) |

---

*Back to [Home](Home)*
