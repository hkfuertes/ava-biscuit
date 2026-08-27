# Mod Development Guide

Complete reference for building Ava mods. Covers manifest format, entity types, manager API, voice pipeline hooks, device compatibility, build, and publishing.

Source: [github.com/knoop7/ava-mods](https://github.com/knoop7/ava-mods)

---

## Architecture

Mods are lightweight Java modules loaded at runtime via `DexClassLoader`. They run inside Ava's process and communicate with Home Assistant through ESPHome protocol entities. No APK rebuild required.

```
ava-mods/
├── store.json                     # Store index consumed by Ava
├── mods/                          # Release packages (downloaded by Ava)
│   ├── devices/                   # Device-specific mods
│   └── features/                  # Feature-oriented mods
├── sources/                       # Source code and build scripts
│   ├── devices/
│   └── features/
├── docs/                          # Documentation
└── examples/                      # Minimal example manifests
```

- `mods/` — release-ready packages only (manifest.json + libs/)
- `sources/` — source code, build scripts, build output
- `devices/` — model-specific or hardware-specific mods
- `features/` — reusable functional mods not tied to one device family

---

## Manifest Specification

### Root Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique identifier (lowercase, hyphens, max 64 chars, `[A-Za-z0-9._-]`) |
| `name` | string | Yes | Display name |
| `version` | string | Yes | Semantic version (e.g. `"1.0.0"`) |
| `author` | string | No | Author name |
| `description` | string | No | Short one-line description shown in mod list |
| `detail_description` | string | No | Longer description shown in mod detail view |
| `icon` | string | No | MDI icon name (default: `mdi:puzzle`) |
| `libs` | array | No | JAR/SO files to load (e.g. `["libs/mydevice.jar"]`) |
| `manager` | string | No | Manager class fully-qualified name |
| `voice_pipeline` | boolean | No | Opt-in to voice pipeline events (default: `false`) |
| `overlay_z_order` | boolean | No | Opt-in to overlay z-order hook for global tint overlays (default: `false`) |
| `overlay_below_voice` | boolean | No | Opt-in to media-style overlay above dashboard but below voice/notification layers (default: `false`) |
| `playback_reference` | boolean | No | Opt-in to playback-reference hooks for software AEC (default: `false`) |
| `ble_adv_proxy` | boolean | No | Opt-in to BLE ADV proxy protocol (see BLE ADV Proxy API below, default: `false`) |
| `permissions` | array | No | Android permission tokens (aliases or full names) |
| `config` | array | No | Configuration items (see Config Items below) |
| `status_panel` | array | No | Status panel items for model download / management UI (see Status Panel below) |
| `entities` | array | Yes | Entity definitions (see Entity Types below) |
| `jar_hash` | string | No | MD5 hash of JAR file (used for update detection) |

### Minimal Manifest

```json
{
  "id": "my-mod",
  "name": "My Mod",
  "version": "1.0.0",
  "entities": [
    {
      "type": "switch",
      "id": "power",
      "name": "Power"
    }
  ]
}
```

### Full Manifest Example

```json
{
  "id": "my-device-mod",
  "name": "My Device Controller",
  "version": "1.0.0",
  "author": "Your Name",
  "description": "Control my custom device",
  "icon": "mdi:chip",
  "libs": ["libs/mydevice.jar"],
  "manager": "com.mydevice.DeviceManager",
  "voice_pipeline": true,
  "permissions": ["camera", "android.permission.READ_LOGS"],
  "entities": [
    {
      "type": "switch",
      "id": "power",
      "name": "Power",
      "icon": "mdi:power",
      "on": "setPower:1",
      "off": "setPower:0",
      "read": "isPowerOn"
    },
    {
      "type": "binary_sensor",
      "id": "motion",
      "name": "Motion",
      "icon": "mdi:motion-sensor",
      "class": "motion",
      "gpio": 4,
      "read": "isMotionDetected"
    },
    {
      "type": "sensor",
      "id": "temperature",
      "name": "Temperature",
      "icon": "mdi:thermometer",
      "unit": "°C",
      "class": "temperature",
      "accuracy_decimals": 1,
      "read": "getTemperature",
      "refresh_interval_ms": 5000
    },
    {
      "type": "number",
      "id": "brightness",
      "name": "Brightness",
      "icon": "mdi:brightness-6",
      "min": 0,
      "max": 100,
      "step": 1,
      "set": "setBrightness",
      "read": "getBrightness"
    },
    {
      "type": "button",
      "id": "reboot",
      "name": "Reboot",
      "icon": "mdi:restart",
      "press": "reboot"
    },
    {
      "type": "select",
      "id": "mode",
      "name": "Mode",
      "icon": "mdi:format-list-bulleted",
      "options": ["auto", "manual", "off"],
      "set": "setMode",
      "read": "getMode"
    },
    {
      "type": "text",
      "id": "label",
      "name": "Label",
      "icon": "mdi:form-textbox",
      "set": "setLabel",
      "read": "getLabel"
    },
    {
      "type": "text_sensor",
      "id": "status",
      "name": "Status",
      "icon": "mdi:text-box-outline",
      "category": "diagnostic",
      "read": "getStatusText"
    }
  ],
  "config": [
    {
      "type": "switch",
      "key": "enable_feature_x",
      "label": "Feature X",
      "description": "Enable feature X entities",
      "defaultValue": "false"
    },
    {
      "type": "select",
      "key": "mode",
      "label": "Operating Mode",
      "defaultValue": "auto",
      "options": ["auto", "manual"]
    },
    {
      "type": "number",
      "key": "threshold",
      "label": "Threshold",
      "defaultValue": "50",
      "min": 0,
      "max": 100,
      "step": 1
    },
    {
      "type": "text",
      "key": "api_key",
      "label": "API Key",
      "defaultValue": "",
      "placeholder": "Paste key here"
    }
  ]
}
```

---

## Entity Types

### Common Entity Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | string | Yes | Entity type (see below) |
| `id` | string | Yes | Unique entity ID within mod |
| `name` | string | Yes | Display name |
| `icon` | string | No | MDI icon name |
| `category` | string | No | `config`, `diagnostic`, or empty (default: none) |
| `enabledWhen` | string | No | Config key that must be `"true"` for entity to appear |
| `enabledByConfig` | string | No | Same as `enabledWhen` — entity only shows when config key is `"true"` |

### switch

On/off control with optional state read.

| Field | Description |
|-------|-------------|
| `on` | Action for turning on (e.g. `setPower:1`) |
| `off` | Action for turning off (e.g. `setPower:0`) |
| `read` | Method name to read current state (returns boolean/number/string) |

Switch state is persisted across restarts via `ModStateStore`.

### binary_sensor

Binary state sensor (motion, door, etc).

| Field | Description |
|-------|-------------|
| `class` | Device class: `motion`, `door`, `window`, `smoke`, `moisture`, `occupancy`, `illuminance` |
| `gpio` | GPIO pin number (informational) |
| `read` | Method to read state (returns boolean/number/string) |

### sensor

Numeric or text value sensor.

| Field | Description |
|-------|-------------|
| `unit` | Unit of measurement (e.g. `°C`, `lx`, `%`) |
| `class` | HA device class (e.g. `temperature`, `illuminance`) |
| `accuracy_decimals` | Decimal places (default: 1) |
| `read` | Method to read value (returns Number) |
| `refresh_interval_ms` | Polling interval in ms (default: 30000, min: 50) |

### number

Adjustable numeric value.

| Field | Description |
|-------|-------------|
| `min` | Minimum value (default: 0) |
| `max` | Maximum value (default: 100) |
| `step` | Step size (default: 1) |
| `unit` | Unit of measurement |
| `set` | Method to set value (called as `set:value`) |
| `read` | Method to read current value |

### button

Trigger action on press.

| Field | Description |
|-------|-------------|
| `press` | Action method name (e.g. `reboot`) |

### select

Dropdown option selector.

| Field | Description |
|-------|-------------|
| `options` | Array of string options |
| `set` | Method to set value (called as `set:value`) |
| `read` | Method to read current selection |

### text

Text input field.

| Field | Description |
|-------|-------------|
| `set` | Method to set value (called as `set:value`) |
| `read` | Method to read current value |

### text_sensor

Read-only text state sensor.

| Field | Description |
|-------|-------------|
| `read` | Method to read value (returns String/Number/Boolean) |
| `refresh_interval_ms` | Polling interval in ms |

---

## Config Items

Configuration items appear in Ava's mod settings UI. Users interact with them before the voice service restarts to apply changes.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | string | Yes | `switch`, `select`, `number`, or `text` |
| `key` | string | Yes | Unique config key (used in `enabledWhen`/`enabledByConfig` and passed to `applyConfig`) |
| `label` | string | Yes | Display label |
| `description` | string | No | Help text |
| `dialogHint` | string | No | Hint text in input dialog |
| `placeholder` | string | No | Placeholder for text input |
| `defaultValue` | string | No | Default value (defaults: switch=`"true"`, select=first option, number=min, text=`""`) |
| `enabledWhen` | string | No | Config key that must be `"true"` for this item to be editable |
| `options` | array | No | For `select` type — available options |
| `min` | float | No | For `number` type — minimum |
| `max` | float | No | For `number` type — maximum |
| `step` | float | No | For `number` type — step size |

Config values are resolved as `defaults + userOverrides`, sanitized per type, and passed to the manager via `applyConfig`.

---

## Status Panel

The `status_panel` array defines rich UI cards in the mod settings screen. Currently supports `download_strip` type — a progress card for model downloads (e.g. STT model, TTS voices) with phase-aware action buttons.

### Status Panel Item Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | string | Yes | `"download_strip"` |
| `label` | string | Yes | Card title (e.g. `"SenseVoice Model"`) |
| `description` | string | No | Card subtitle (e.g. `"~230MB · zh / en / ja / ko / Cantonese"`) |
| `detail_description` | string | No | Longer description |
| `read` | string | No | Manager method returning progress value (0-100 for download) |
| `listener_id` | string | No | State listener ID for progress updates |
| `status_read` | string | No | Manager method returning status text (e.g. `"ready"`, `"downloading"`) |
| `status_listener_id` | string | No | State listener ID for status text updates |
| `ready_keywords` | array | No | Substrings in status text meaning download complete (case-insensitive) |
| `downloading_keywords` | array | No | Substrings meaning active download |
| `paused_keywords` | array | No | Substrings meaning paused |
| `error_keywords` | array | No | Substrings meaning error |
| `inline_config_keys` | array | No | Config keys rendered below the strip in the same card (e.g. `["recognition_language"]`) |
| `layout` | string | No | `"progress_primary"` for large % display, or omit for compact strip |
| `actions` | array | No | Action buttons (see below) |

### Action Button Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `label` | string | No | Button text (can be empty for icon-only) |
| `press` | string | Yes | Manager method to call on tap |
| `style` | string | No | `"primary"`, `"secondary"`, or `"destructive"` |
| `show_when` | array | No | Phases when button is visible: `idle`, `downloading`, `paused`, `ready`, `error`. Empty = always. |

### Example: Model Download Card

```json
{
  "status_panel": [
    {
      "type": "download_strip",
      "label": "SenseVoice Model",
      "description": "~230MB · zh / en / ja / ko / Cantonese",
      "read": "getDownloadProgress",
      "listener_id": "download_progress",
      "status_read": "getModelStatusDisplay",
      "status_listener_id": "model_status",
      "ready_keywords": ["ready"],
      "downloading_keywords": ["downloading"],
      "paused_keywords": ["paused"],
      "error_keywords": ["error", "failed"],
      "inline_config_keys": ["recognition_language"],
      "actions": [
        {
          "label": "",
          "press": "downloadModel",
          "style": "primary",
          "show_when": ["idle", "paused", "error"]
        },
        {
          "label": "",
          "press": "pauseDownload",
          "style": "secondary",
          "show_when": ["downloading"]
        },
        {
          "label": "",
          "press": "deleteModel",
          "style": "destructive",
          "show_when": ["ready", "idle", "paused", "error"]
        }
      ]
    }
  ]
}
```

### Manager Methods for Status Panel

| Method | Signature | Description |
|--------|-----------|-------------|
| `getDownloadProgress` | `() → float` | Download progress 0-100 |
| `getModelStatusDisplay` | `() → String` | Status text for phase detection (matched against keywords) |
| `getModelStatus` | `() → String` | Raw status (optional, for diagnostics) |
| `downloadModel` | `()` | Start or resume download |
| `pauseDownload` | `()` | Pause active download |
| `deleteModel` | `()` | Delete downloaded model |

Ava polls `read` (progress) and `status_read` (status) at regular intervals. The status text is matched against `ready_keywords`, `downloading_keywords`, `paused_keywords`, and `error_keywords` to determine the current phase. Action buttons are shown/hidden based on `show_when` matching the current phase.

---

## Action Format

Actions use `methodName:arg1,arg2,arg3` format. Ava coerces the argument type based on the value:

| Argument | Java Method Signature |
|----------|----------------------|
| (none) | `method()` |
| `true` / `false` | `method(boolean)` |
| Integer (e.g. `42`) | `method(int)` |
| Float (e.g. `3.14`) | `method(float)` |
| Other string | `method(String)` |

Examples:
- `setPower:1` → `manager.setPower(1)`
- `setPower:true` → `manager.setPower(true)`
- `setRGB:255,0,0` → `manager.setRGB("255", "0", "0")` (multi-arg not auto-typed; use String)
- `reboot` → `manager.reboot()`

---

## Manager Class

The manager is a Java class loaded via `DexClassLoader`. It must follow the singleton pattern:

```java
public class MyManager {
    private static MyManager instance;

    public static MyManager getInstance(Context context) {
        if (instance == null) instance = new MyManager();
        return instance;
    }
}
```

### Optional Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `applyConfig` | `(Context, String key, String value)` or `(String key, String value)` | Called for each config item on load |
| `registerStateListener` | `(String entityId, ModStateCallback callback)` | Register push-based state updates |
| `setLastError` | `(String message)` | Report errors to Ava (shown in UI) |
| `onDestroy` | `()` | Called when mod is disabled/uninstalled — cleanup resources |

### State Listener

For push-based updates instead of polling, implement `registerStateListener`:

```java
public boolean registerStateListener(String entityId, ModStateCallback callback) {
    // Store callback, invoke callback.onStateChanged(value) when state changes
    // Return true to indicate listener registered (Ava will skip initial read)
    return true;
}
```

`ModStateCallback` is an abstract class:

```java
@Keep
public abstract class ModStateCallback {
    @Keep
    public abstract void onStateChanged(Object value);
}
```

Value type handling:
- **switch/binary_sensor**: `Boolean`, `Number` (0=false), or `String` ("true"/"on"/"1" = true)
- **sensor**: `Number` → `toFloat()`
- **text_sensor**: `String`, `Number`, or `Boolean` → `toString()`

### State Persistence

Switch states are automatically persisted by `ModStateStore`. On restart, the last known state is restored before the manager's `read` method is called. No action needed from the mod author.

---

## Device Compatibility Hooks

Mods with a `manager` class can optionally expose device-level hooks consumed by Ava core. These are useful for model-specific behavior without modifying the main APK.

| Method | Signature | Description |
|--------|-----------|-------------|
| `isSupported` | `()` or `(Context)` → `boolean` | Whether this mod applies to the current device |
| `getMinBrightness` | `()` or `(Context)` → `int` | Override minimum brightness for the display |
| `isLowEndBleChip` | `()` or `(Context)` → `boolean` | Flag for BLE tuning on low-end chips |
| `grantOverlayPermissionIfNeeded` | `()` or `(Context)` → `boolean` | Root-based overlay permission grant |
| `onKeyDown` | `(Context, int keyCode, KeyEvent)` → `boolean` | Intercept physical key presses |
| `onKeyUp` | `(Context, int keyCode, KeyEvent)` → `boolean` | Intercept physical key releases |

All methods are optional. Mods that only expose entities do not need to implement them. Ava checks for method existence by name — if none of these methods exist on the manager class, the mod is skipped for device support.

---

## Voice Pipeline API

Opt-in hook for reacting to satellite lifecycle events (e.g. LED ring on wake, TTS playback). Zero cost when unused: no ClassLoader load, no broadcast, no thread.

### Opt-in (both required)

1. Set `"voice_pipeline": true` in manifest.json
2. Manager implements `onVoicePipelineEvent(Context, String, Bundle)`

### Manifest

```json
{
  "id": "echo-dot-led",
  "name": "Echo Dot LED",
  "voice_pipeline": true,
  "manager": "com.example.EchoDotLedManager",
  "libs": ["libs/echo-dot-led.jar"]
}
```

### Java Manager

```java
public class EchoDotLedManager {
    private static EchoDotLedManager instance;

    public static EchoDotLedManager getInstance(Context context) {
        if (instance == null) instance = new EchoDotLedManager();
        return instance;
    }

    public void onVoicePipelineEvent(Context context, String event, Bundle extras) {
        switch (event) {
            case "wake_detected":
                // extras: wake_word, wake_word_id, wake_confidence, synthetic_wake
                break;
            case "listening_started":
                // extras: accent_color
                break;
            case "stt_vad_start":
            case "stt_vad_end":
            case "stt_end":
                // extras: stt_text
                break;
            case "processing_started":
            case "responding":
                break;
            case "tts_start":
                // extras: tts_text (optional)
                break;
            case "tts_playback_started":
            case "tts_finished":
                break;
            case "session_ended":
            case "run_start":
            case "run_end":
                break;
            case "pipeline_error":
                // extras: error_code, error_message
                break;
        }
    }
}
```

### Event Reference

| Event | Extras | Description |
|-------|--------|-------------|
| `wake_detected` | `wake_word`, `wake_word_id`, `wake_confidence`, `synthetic_wake` | Wake word triggered |
| `listening_started` | `accent_color` | Recording started |
| `stt_vad_start` | — | VAD detected speech start |
| `stt_vad_end` | — | VAD detected speech end |
| `stt_end` | `stt_text` | Speech-to-text completed |
| `processing_started` | — | HA processing intent |
| `responding` | — | Response generation started |
| `tts_start` | `tts_text` (optional) | TTS synthesis started |
| `tts_playback_started` | — | TTS audio playback started |
| `tts_finished` | — | TTS playback completed |
| `session_ended` | — | Voice session fully ended |
| `run_start` | — | Pipeline run started |
| `run_end` | — | Pipeline run ended |
| `pipeline_error` | `error_code`, `error_message` | Error occurred |

### External App (Broadcast Only)

Other installed apps can receive voice pipeline events via broadcast without a mod:

```xml
<receiver android:name=".AvaVoiceReceiver" android:exported="true">
  <intent-filter>
    <action android:name="com.example.ava.VOICE_PIPELINE_EVENT" />
  </intent-filter>
</receiver>
```

Intent extras: `event` (string) plus event-specific keys. Ava checks for external receivers via `PackageManager.queryBroadcastReceivers` — if none found, no broadcast is sent.

---

## BLE ADV Proxy API

Opt-in hook for building a raw BLE advertising proxy mod (Android port of [esphome-ble_adv_proxy](https://github.com/NicoIIT/esphome-ble_adv_proxy) for [ha-ble-adv](https://github.com/NicoIIT/ha-ble-adv)).

When enabled, Ava:
- Registers ESPHome services (`setup_svc_v0`, `adv_svc`, `adv_svc_v1`) on behalf of the mod
- Forwards BLE scan results to the mod
- Provides a `BleAdvHostApi` object for firing HA events and running exclusive BLE transmit windows
- Pauses its own BLE scanning and presence advertising during the mod's transmit windows

### Opt-in (both required)

1. Set `"ble_adv_proxy": true` in manifest.json
2. Manager implements the lifecycle and service-call methods below

### Manifest

```json
{
  "id": "ble-adv-proxy",
  "name": "BLE ADV Proxy",
  "version": "0.1.2",
  "ble_adv_proxy": true,
  "manager": "com.ava.mods.bleadv.BleAdvProxyManager",
  "libs": ["libs/ble-adv-proxy-manager.jar"],
  "permissions": [
    "bluetooth",
    "bluetooth_scan",
    "bluetooth_connect",
    "bluetooth_advertise"
  ]
}
```

### BleAdvHostApi

Ava passes a `BleAdvHostApi` instance to the manager's `onEspHomeConnected` method. The mod stores it and calls its methods to fire HA events and run BLE transmit.

| Method | Signature | Description |
|--------|-----------|-------------|
| `fireHomeassistantEvent` | `(String service, Map<String, String> data)` | Fire a HA event (e.g. `esphome.ble_adv.raw_adv`) with key-value data |
| `runExclusiveTransmit` | `(Runnable task)` | Run a blocking BLE transmit with proxy scan and presence ADV paused. Blocks the calling thread until the exclusive window completes. |
| `enqueueExclusiveTransmit` | `(Runnable task, Runnable onComplete)` | Queue async exclusive BLE work. Returns immediately; `onComplete` runs after the window closes. |
| `isExclusiveActive` | `() → boolean` | Whether an exclusive BLE window is currently active |

### Manager Methods (called by Ava via reflection)

| Method | Signature | When Called |
|--------|-----------|-------------|
| `getInstance` | `(Context) → BleAdvProxyManager` | Singleton access — Ava gets the manager instance |
| `isBleAdvProxySupported` | `(Context) → boolean` | During binding — return `false` to disable the mod |
| `isFeatureEnabled` | `(Context) → boolean` | During binding — return `false` if user disabled the feature in config |
| `onEspHomeConnected` | `(Context, String deviceName, BleAdvHostApi hostApi)` | ESPHome session established — store `hostApi` for later use |
| `onEspHomeDisconnected` | `(Context)` | ESPHome session ended — clear state, stop transmit |
| `onHomeassistantServicesSubscribed` | `(Context)` | HA subscribed to homeassistant services — scan forwarding can begin |
| `onScanResult` | `(Context, String mac, int rssi, byte[] raw)` | Each BLE scan result with raw ADV bytes (≥5 bytes, ≤31 bytes). Only called when HA services are subscribed and no exclusive transmit is active. |
| `onServiceCall` | `(Context, String serviceName, Map<String, Object> args)` | ESPHome service call from HA — dispatch to `setup_svc_v0`, `adv_svc`, or `adv_svc_v1` |
| `getAdapterName` | `(Context) → String` | Resolve adapter name for logging (optional — falls back to device name) |
| `applyConfig` | `(String key, String value)` | Config key-value sync from Ava |
| `onDestroy` | `()` | Mod being unloaded — clean up resources |

### ESPHome Services (auto-registered by Ava core)

Ava registers these services on behalf of the mod. HA calls them via `esphome.<service_name>`:

| Service | Args | Description |
|---------|------|-------------|
| `setup_svc_v0` | `ignored_duration` (float), `ignored_cids` (int[]), `ignored_macs` (string[]) | Configure dedup filter: ignore duration in ms, company IDs to skip, MAC addresses to skip |
| `adv_svc` | `raw` (string hex), `duration` (float ms) | Legacy transmit — internally mapped to `adv_svc_v1` with `repeat=3` |
| `adv_svc_v1` | `raw` (string hex), `duration` (float ms), `repeat` (float), `ignored_advs` (string[]), `ignored_duration` (float ms) | Full transmit: raw ADV hex, burst duration, repeat count, echo-ignore list, echo-ignore duration |

### HA Event

The mod fires `esphome.ble_adv.raw_adv` events for scanned BLE broadcasts:

| Key | Value |
|-----|-------|
| `raw` | Hex string of raw ADV bytes |
| `orig` | Source MAC address (uppercase) |

### Exclusive BLE Window

Android cannot safely overlap BLE scanning, presence advertising, and raw ADV transmit. Use `runExclusiveTransmit` or `enqueueExclusiveTransmit` for all ADV transmit — Ava handles pausing and resuming its own BLE operations around the call. During an exclusive window, `onScanResult` is not forwarded to the mod.

### Java Manager Example

```java
public class BleAdvProxyManager {
    private static volatile BleAdvProxyManager instance;
    private volatile Object hostApi; // BleAdvHostApi, passed by Ava
    private volatile boolean haServicesReady = false;
    private volatile boolean setupDone = false;

    public static BleAdvProxyManager getInstance(Context context) {
        if (instance == null) {
            synchronized (BleAdvProxyManager.class) {
                if (instance == null) instance = new BleAdvProxyManager();
            }
        }
        return instance;
    }

    public boolean isBleAdvProxySupported(Context ctx) { return true; }
    public boolean isFeatureEnabled(Context ctx) { return true; }
    public boolean isProxyReady() { return haServicesReady && setupDone; }

    public void onEspHomeConnected(Context ctx, String deviceName, Object hostApi) {
        this.hostApi = hostApi;
    }

    public void onEspHomeDisconnected(Context ctx) {
        hostApi = null;
        haServicesReady = false;
        setupDone = false;
    }

    public void onHomeassistantServicesSubscribed(Context ctx) {
        haServicesReady = true;
    }

    public void onScanResult(Context ctx, String mac, int rssi, byte[] raw) {
        if (!haServicesReady || !setupDone) return;
        // Dedup, parse, fire esphome.ble_adv.raw_adv event
        Map<String, String> payload = new HashMap<>();
        payload.put("raw", toHex(raw));
        payload.put("orig", mac.toUpperCase());
        fireHomeassistantEvent("esphome.ble_adv.raw_adv", payload);
    }

    public void onServiceCall(Context ctx, String serviceName, Map<String, Object> args) {
        switch (serviceName) {
            case "setup_svc_v0": handleSetup(args); break;
            case "adv_svc":      handleAdvertiseV0(args); break;
            case "adv_svc_v1":   handleAdvertiseV1(args); break;
        }
    }

    private void handleSetup(Map<String, Object> args) {
        // Configure dedup cache from ignored_duration, ignored_cids, ignored_macs
        setupDone = true;
    }

    private void handleAdvertiseV1(Map<String, Object> args) {
        String raw = String.valueOf(args.get("raw"));
        int durationMs = Math.max(32, (int) readFloat(args, "duration", 100f));
        int repeat = Math.max(1, (int) readFloat(args, "repeat", 1f));
        byte[] rawBytes = fromHex(raw);
        for (int i = 0; i < repeat; i++) {
            enqueueTransmit(rawBytes, durationMs);
        }
    }

    private void enqueueTransmit(byte[] raw, int durationMs) {
        Object api = hostApi;
        if (api == null) return;
        try {
            Method m = api.getClass().getMethod("enqueueExclusiveTransmit",
                Runnable.class, Runnable.class);
            m.invoke(api, (Runnable) () -> transmitBlocking(raw, durationMs), null);
        } catch (Exception e) {
            // Fallback: run without exclusive window
            transmitBlocking(raw, durationMs);
        }
    }

    private String transmitBlocking(byte[] raw, int durationMs) {
        // Use BluetoothLeAdvertiser with AdvertiseData from raw bytes
        // Return error string or "" on success
        return "";
    }

    private void fireHomeassistantEvent(String service, Map<String, String> data) {
        Object api = hostApi;
        if (api == null) return;
        try {
            Method m = api.getClass().getMethod("fireHomeassistantEvent",
                String.class, Map.class);
            m.invoke(api, service, data);
        } catch (Exception e) {
            // hostApi unavailable
        }
    }

    public void onDestroy() {
        hostApi = null;
        haServicesReady = false;
        setupDone = false;
    }
}
```

### Android Limitations

ESP32 uses `esp_ble_gap_config_adv_data_raw()`. Android maps raw AD bytes to `AdvertiseData` (manufacturer / service data). Most ceiling-fan protocols work; exotic layouts may need device-specific hooks.

---

## Permissions

### Manifest Permission Tokens

Use aliases or full Android permission names in the `permissions` array:

| Alias | Resolved Permissions |
|-------|---------------------|
| `gps` / `location` | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` |
| `fine_location` | `ACCESS_FINE_LOCATION` |
| `coarse_location` | `ACCESS_COARSE_LOCATION` |
| `background_location` | `ACCESS_BACKGROUND_LOCATION` |
| `camera` | `CAMERA` |
| `microphone` / `record_audio` | `RECORD_AUDIO` |
| `bluetooth` | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` (API 31+) or `ACCESS_FINE_LOCATION` (older) |
| `bluetooth_scan` | `BLUETOOTH_SCAN` |
| `bluetooth_connect` | `BLUETOOTH_CONNECT` |
| `bluetooth_advertise` | `BLUETOOTH_ADVERTISE` |
| `notifications` / `post_notifications` | `POST_NOTIFICATIONS` |

Full names (e.g. `android.permission.READ_LOGS`) are also accepted. Unknown tokens are logged and skipped.

### Permission Categories

- **Install-time** (auto-granted): `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE*`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`
- **Runtime** (prompt user): `CAMERA`, `RECORD_AUDIO`, `ACCESS_FINE_LOCATION`, etc.
- **Privileged** (require ADB/root): `READ_LOGS`, `WRITE_SECURE_SETTINGS`, `DUMP`, `PACKAGE_USAGE_STATS`

If a mod requires a runtime permission that hasn't been granted, it cannot be enabled. If denied permanently, the user must grant it via Android app settings.

---

## Build

### Requirements

- Android SDK (platform 34+)
- Java 11+
- `d8` tool (in Android SDK build-tools)

### Steps

```bash
cd sources/features/your-mod/
chmod +x build.sh
./build.sh
```

The build script:
1. Compiles Java sources with `javac`
2. Converts `.class` to DEX format using `d8`
3. Packages DEX into a JAR in `libs/`

Copy the built JAR to the release directory:

```bash
cp libs/your-manager.jar ../../mods/features/your-mod/libs/
```

### Native Libraries

Mods can include `.so` files for native code. List them in `libs` alongside JARs:

```json
{
  "libs": [
    "libs/mymanager.jar",
    "libs/jni/arm64-v8a/libnative.so",
    "libs/jni/armeabi-v7a/libnative.so"
  ]
}
```

Ava sets the native library path automatically when creating the `DexClassLoader`.

---

## Testing

1. Copy `manifest.json` and `libs/` to device: `/data/data/com.example.ava/files/mods/your-mod/`
2. Enable in Ava settings (Settings → Advanced → Mod Store → Installed)
3. Grant required permissions
4. Check Home Assistant for entities
5. Monitor logcat: `adb logcat -s ModManager ModEntityFactory ModVoicePipeline`

---

## Publishing

1. Create source files in `sources/devices/` or `sources/features/`
2. Build and copy release package to `mods/devices/` or `mods/features/`
3. Add or update the entry in `store.json`
4. Submit Pull Request to [ava-mods](https://github.com/knoop7/ava-mods)

### Store Index Format (`store.json`)

```json
{
  "version": 1,
  "baseUrl": "https://raw.githubusercontent.com/knoop7/ava-mods/main/",
  "mods": [
    {
      "id": "my-mod",
      "name": "My Mod",
      "version": "1.0.0",
      "author": "Your Name",
      "description": "Description here",
      "path": "mods/features/my-mod/",
      "jar_hash": "md5hashofjarfile"
    }
  ]
}
```

| Field | Description |
|-------|-------------|
| `id` | Unique mod ID (must match manifest `id`) |
| `name` | Display name |
| `version` | Current version |
| `author` | Author name |
| `description` | Short description |
| `path` | Relative path to mod directory (trailing `/`) |
| `jar_hash` | MD5 hash of JAR file (used for update detection — if hash matches, download is skipped) |

---

## Headless Mod Control (Broadcast)

External apps or ADB can enable/disable/reload mods via broadcast, without opening the Ava UI. This is useful for automation scripts, provisioning, and headless deployments.

### Actions

| Action | Description |
|--------|-------------|
| `com.example.ava.ACTION_SET_MOD_ENABLED` | Enable or disable a specific mod |
| `com.example.ava.ACTION_RELOAD_MOD` | Reload one mod (or all enabled mods if `mod_id` is omitted) |

### Extras

| Extra | Type | Required | Description |
|-------|------|----------|-------------|
| `mod_id` | String | Yes for `SET_MOD_ENABLED`, optional for `RELOAD_MOD` | Mod ID to control. If omitted in `RELOAD_MOD`, all enabled mods are reloaded. |
| `mod_enabled` | Boolean | No (default: `true`) | For `SET_MOD_ENABLED` — `true` to enable, `false` to disable |
| `no_restart` | Boolean | No (default: `false`) | If `true`, skip voice satellite restart after mod change |

### ADB Examples

```bash
# Enable a mod
adb shell am broadcast -a com.example.ava.ACTION_SET_MOD_ENABLED --es mod_id echo_dot_led --ez mod_enabled true

# Disable a mod without restarting satellite
adb shell am broadcast -a com.example.ava.ACTION_SET_MOD_ENABLED --es mod_id echo_dot_led --ez mod_enabled false --ez no_restart true

# Reload a specific mod
adb shell am broadcast -a com.example.ava.ACTION_RELOAD_MOD --es mod_id echo_dot_led

# Reload all enabled mods
adb shell am broadcast -a com.example.ava.ACTION_RELOAD_MOD
```

### Behavior

- `SET_MOD_ENABLED` refreshes the registry from disk first, so manual `adb push` of mod files is picked up.
- `RELOAD_MOD` disables and re-enables the mod to force `DexClassLoader` refresh. Only enabled mods are reloaded; disabled mods are left unchanged.
- Unless `no_restart=true`, the voice satellite service is restarted automatically to apply changes.
- If the service is not running, mod state applies on next start.

---

## Self-Update API

Mods can trigger self-updates via reflection on `ModManager`. Available methods:

| Method | Returns | Description |
|--------|---------|-------------|
| `updateModSync(modId)` | `"ok"` or error message | Refreshes store, downloads latest version |
| `reloadModSync(modId)` | `"ok"` or error message | Disables and re-enables mod to force ClassLoader refresh |
| `updateAndReloadModSync(modId)` | `"ok"` or error message | Downloads + reloads in one call |

```java
ModManager modManager = ModManager.getInstance(context);
String result = modManager.updateAndReloadModSync("my-mod");
```

---

## TWRP Provisioning

Bundle Ava APK, mods, and configuration in a single TWRP zip for factory deployment. No new API needed — Ava persists all settings to DataStore via `ACTION APPLY SETTINGS` broadcast.

### Zip Structure

```
twrp-ava-echo.zip
├── system/priv-app/Ava/Ava.apk
├── ava_mods/
│   ├── echo-show-support/
│   │   ├── manifest.json
│   │   └── libs/echo-show-support.jar
│   └── (other mods...)
├── ava_provision.json
└── META-INF/com/google/android/update-binary
```

### Provisioning JSON

The `ava_provision.json` file is a partial JSON patch merged into Ava's DataStore. Only include keys you want to set — missing keys keep their defaults.

**Supported stores:** `microphone`, `player`, `experimental`, `sendspin`, `voice_channel`, `screensaver`, `voice_satellite`

```json
{
  "microphone": {
    "wakeWord": "okay_nabu",
    "wakeWords": ["okay_nabu"],
    "micGainDb": 12,
    "muted": false
  },
  "voice_satellite": {
    "name": "echo_show_crown",
    "serverPort": 6503,
    "serverPortUserConfigured": true
  },
  "player": {
    "enableAutoRestart": true
  },
  "screensaver": {
    "darkOffEnabled": true
  },
  "experimental": {
    "environmentSensorEnabled": true
  },
  "sendspin": {
    "enabled": false
  }
}
```

**File path whitelist:** `/sdcard/`, `/storage/emulated/0/`, app data dir, app filesDir. The file must be in one of these or the broadcast will be rejected.

### Mod Files

Copy mod files directly to Ava's data directory (same structure as UI install):

```
/data/data/com.example.ava/files/mods/
├── registry.json
└── echo-show-support/
    ├── manifest.json
    └── libs/echo-show-support.jar
```

`registry.json`:

```json
{
  "version": 1,
  "mods": [
    { "id": "echo-show-support", "version": "1.1.0", "enabled": true }
  ]
}
```

Fix ownership after copying (get UID from `dumpsys package com.example.ava`):

```bash
AVA_UID=$(dumpsys package com.example.ava | grep userId= | head -1 | cut -d= -f2 | tr -d ' ')
chown -R ${AVA_UID}:${AVA_UID} /data/data/com.example.ava/files/mods/
chmod -R 755 /data/data/com.example.ava/files/mods/
```

### First-Boot Script

Place in `/data/adb/service.d/` (Magisk) or `post-fs-data.d`. Idempotent — runs once, marks completion.

```bash
#!/system/bin/sh
MARKER=/data/local/ava_provisioned
[ -f "$MARKER" ] && exit 0

# Wait for boot
while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done
while ! pm path com.example.ava >/dev/null 2>&1; do sleep 2; done
sleep 5

# Permissions
am broadcast -a com.example.ava.ACTION_GRANT_OVERLAY com.example.ava
am broadcast -a com.example.ava.ACTION_GRANT_BLUETOOTH com.example.ava
am broadcast -a com.example.ava.ACTION_GRANT_RECORD_AUDIO com.example.ava

# Apply settings (persists to DataStore)
am broadcast -a com.example.ava.ACTION_APPLY_SETTINGS \
  --es settings_file /sdcard/ava_provision.json com.example.ava

# Enable mods
am broadcast -a com.example.ava.ACTION_SET_MOD_ENABLED \
  --es mod_id echo-show-support --ez mod_enabled true com.example.ava

# Start service
am broadcast -a com.example.ava.ACTION_START_SERVICE com.example.ava

touch "$MARKER"
```

### What Happens After

- Settings merge into DataStore JSON files at `/data/data/com.example.ava/files/*_settings.json` — survives reboot
- `player.enableAutoRestart: true` means `BootReceiver` auto-starts the voice satellite on every boot
- Mods load from `registry.json` on service start
- Re-running the script without the marker is safe — `ACTION APPLY SETTINGS` is a merge, not a replace

### Gaps

| Item | Status | Workaround |
|------|--------|------------|
| Bluetooth Proxy toggle | Stored in `bluetooth_settings` SharedPreferences, not in applier | Write XML directly: `/data/data/com.example.ava/shared_prefs/bluetooth_settings.xml` with `detect_enabled` boolean |
| iptables / WiFi / priv-app | Not Ava's responsibility | Handle in TWRP zip install script |

---

*Back to [Mod Store](Mod-Store)*
