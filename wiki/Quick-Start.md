# Quick Start

This guide helps you quickly install and configure Ava.

---

## System Requirements

| Requirement | Description |
|-------------|-------------|
| Android Version | 5.0 (API 21) or higher |
| Architecture | arm64-v8a or armeabi-v7a |
| Home Assistant | Must be installed and running |
| ESPHome Integration | Must be installed in HA |
| Network | Device and HA must be on same LAN |

---

## Step 1: Install Ava

### Download APK

Download the latest version from GitHub Releases:
- https://github.com/knoop7/Ava/releases

Ava has a built-in auto-update feature. Once installed, it checks for updates periodically.

### Install

1. Transfer APK file to your Android device
2. Tap APK file to start installation
3. If prompted about "Unknown sources", allow installation

### Grant Permissions

On first launch, Ava will request these permissions:

| Permission | Purpose | Required |
|------------|---------|----------|
| Microphone | Voice input | Yes |
| Overlay | Display screensaver, notifications, floating windows | Yes |
| Foreground Service | Keep service running | Yes |
| Camera | Photos and video | Optional |
| Bluetooth | Presence detection and BLE proxy | Optional |
| Location | Required for Bluetooth scanning | Optional |

---

## Step 2: Configure Home Assistant

### Install ESPHome Integration

1. Open Home Assistant
2. Go to **Settings** -> **Devices & Services** -> **Integrations**
3. Click **+ Add Integration** in bottom right
4. Search for **ESPHome**
5. Click to install

### Discover Ava Device

1. Launch Ava app
2. Go to **Settings** -> **Voice Config**
3. Tap **Start Service**
4. Return to Home Assistant
5. ESPHome integration will auto-discover Ava device
6. Click **Configure** to complete setup

### Manual Add (if auto-discovery fails)

1. In ESPHome integration, click **Configure**
2. Enter Ava device's IP address
3. Port defaults to 6053
4. Click Submit

---

## Step 3: Configure Voice Assistant

### In Home Assistant

1. Go to **Settings** -> **Voice Assistants**
2. Ensure voice assistant pipeline is configured
3. Recommended setup:
   - Speech-to-Text: Whisper
   - Text-to-Speech: Piper
   - Conversation Agent: Home Assistant built-in

### In Ava

1. Go to **Settings** -> **Voice Config**
2. Confirm status shows "Connected"
3. Select your preferred wake word
4. Choose wake word engine (microWakeWord or vsWakeWord)

---

## Step 4: Optional Setup

### Browser Display

To show Home Assistant dashboards on Ava:

1. Go to **Settings** -> **Browser**
2. Enable **Browser Display**
3. Enter your HA URL in **Remote Browser URL**
4. Choose render engine: System WebView or GeckoView

> **GeckoView** is recommended for old devices. It provides a modern browser engine for devices with outdated system WebView. Requires downloading the engine pack separately.

### Permissions (Optional but Recommended)

For features like screen control, display resolution, and intent execution, set up Shizuku:

1. Install [Shizuku](https://github.com/RikkaApps/Shizuku) from GitHub
2. Enable via wireless debugging or ADB
3. Go to **Settings** -> **Permissions** -> **Shizuku Authorization**
4. Tap **Authorize**

See [Permissions](Permissions) for detailed guide.

---

## Step 5: Test

### Voice Test

1. Say wake word (default "Hey Jarvis")
2. After hearing prompt sound, say "What time is it"
3. Ava should reply with current time

### Control Test

1. Say wake word
2. Say "Turn on the living room light" (assuming you have this device)
3. Light should turn on

---

## FAQ

### Device not discovered?

1. Ensure Ava and Home Assistant are on same network
2. Check if firewall is blocking port 6053
3. Try restarting Ava service
4. Try manually adding device

### Connection drops?

1. Check network stability
2. Ensure Ava isn't being killed by system
3. Disable battery optimization in settings
4. See [FAQ](FAQ) for brand-specific solutions

### Voice recognition not working?

1. Check Home Assistant voice assistant configuration
2. Ensure Whisper and other components are running
3. Check network latency

---

## Next Steps

After setup is complete, you can:

- [Configure Screensaver](Screensaver)
- [Set up Notification Scenes](Notification-Scenes)
- [Enable Floating Windows](Floating-Windows)
- [Configure Camera](Camera)
- [Set up Bluetooth Proxy](Bluetooth)
- [Configure Voice Messages & Calls](Voice-Messages-Calls)
- [Customize Quick Entity Panel](Quick-Entity)
- [Set up Sendspin / Music Assistant](Sendspin)

---

*Back to [Home](Home)*
