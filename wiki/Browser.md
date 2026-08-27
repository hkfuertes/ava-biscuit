# Browser

The built-in browser lets you display web pages inside Ava, such as Home Assistant dashboards. It supports two render engines: System WebView and GeckoView.

---

## Overview

The browser is a full-screen overlay that can display any web content on top of Ava.

**Main Uses:**
- Display Home Assistant dashboards
- Display custom control panels
- Display information websites
- Kiosk mode for old devices

---

## Comparison with Other Kiosk Browsers

Ava's browser takes a different architectural approach from standalone kiosk browsers like [Fully Kiosk Browser](https://www.fully-kiosk.com/) and [WallPanel](https://github.com/TheTimeWalker/wallpanel-android). They are designed for different scenarios and can complement each other.

### Architectural Differences

| Aspect | Ava | Fully Kiosk | WallPanel |
|--------|-----|-------------|-----------|
| **Rendering layer** | `SYSTEM_ALERT_WINDOW` overlay (Service) | Full-screen Activity | Full-screen Activity |
| **Process model** | Runs inside voice satellite process | Standalone process | Standalone process |
| **Voice integration** | Native, same process | Separate app | Separate app |
| **Overlay layers** | Multi-layer, `OverlayZOrderCoordinator` manages z-order | Single layer | Single layer |
| **Lifecycle** | Hide/show without destroying renderer | Activity lifecycle | Activity lifecycle |
| **HA communication** | ESPHome protocol (persistent TCP + protobuf) | REST API (HTTP polling) | MQTT or HTTP |
| **Latency** | Sub-100ms (push) | 1-5s (poll) | 1-5s (poll) |

### Different Design Goals

**Fully Kiosk** is a mature general-purpose kiosk browser. It excels at:
- App lockdown and MDM features (restrict apps, disable settings, kiosk mode)
- Motion detection and camera-based wake
- Multi-device fleet management with volume licensing
- Simple install-and-go setup

**WallPanel** was a community-driven kiosk browser with MQTT/HTTP control. It has been archived since May 2025 and is no longer maintained.

**Ava** is a voice satellite first, with a browser as one of its overlay layers. It is designed for the HA-native wall panel scenario where voice, dashboard, notifications, and floating widgets coexist on one device.

### Overlay vs Activity

Fully Kiosk and WallPanel use a full-screen **Activity**. The browser owns the entire screen, and backgrounding destroys the renderer.

Ava uses `WindowManager.addView()` with `TYPE_APPLICATION_OVERLAY` inside a `Service`. The browser is one layer among many. Hiding it does not destroy the renderer, and other overlays (notification scenes, floating clock, weather, Quick Entity) can appear on top without reloading the page.

This is not inherently better or worse, it is a trade-off:
- **Activity approach** (Fully Kiosk): Simpler, more isolated, better for pure kiosk lockdown
- **Overlay approach** (Ava): Enables multi-layer coexistence, better for integrated HA panels with voice and notifications

### Communication Protocol

| Feature | Ava (ESPHome) | Fully Kiosk (REST) | WallPanel (MQTT/HTTP) |
|---------|---------------|---------------------|------------------------|
| **State push** | Real-time, persistent TCP | Polled every 30s | Polled every 5-30s |
| **Command execution** | Milliseconds | Seconds (poll interval) | Seconds (poll interval) |
| **Encoding** | Protobuf (binary, compact) | JSON | JSON |
| **Encryption** | Native API encryption | Typically plaintext LAN | Typically plaintext LAN |
| **HA integration** | One ESPHome device, all entities | Separate integration + password | Separate MQTT discovery |

Ava uses ESPHome because it is a voice satellite, the ESPHome protocol is required for voice anyway. The browser control rides on the same persistent connection. Fully Kiosk and WallPanel use REST/MQTT because they are standalone apps without a voice protocol requirement.

### Dual Engine Strategy

| Feature | Ava | Fully Kiosk | WallPanel |
|---------|-----|-------------|-----------|
| **Default engine** | System WebView | System WebView | System WebView |
| **Fallback engine** | GeckoView pack (separate APK) | None | None |
| **Old device support** | Install Gecko pack for modern engine | Depends on system WebView | Depends on system WebView |

On Android 5-7 devices without Play Services, the system WebView may be outdated. Ava offers an optional GeckoView engine pack that bundles a modern Gecko engine, updated independently. This is a complementary feature for old hardware, not a replacement for system WebView on newer devices.

### Dark Mode Sync

| Feature | Ava | Fully Kiosk | WallPanel |
|---------|-----|-------------|-----------|
| **Direction** | Bidirectional (HA <-> browser) | One-way (system -> browser) | One-way (system -> browser) |
| **Mechanism** | JS bridge + `themes_updated` subscription | CSS media query | CSS media query |
| **Feedback loop prevention** | 2-second debounce with suppression flags | N/A | N/A |

Ava's bidirectional sync exists because it runs inside the voice satellite process and can push theme changes back to HA. Standalone kiosk browsers communicate via polling and typically follow the system dark mode setting.

### Remote Commands

| Command | Ava | Fully Kiosk | WallPanel |
|---------|-----|-------------|-----------|
| **Navigate to URL** | Yes (instant) | Yes (polled) | Yes (polled) |
| **Reload page** | Yes (instant) | Yes (polled) | Yes (polled) |
| **Execute JS** | Yes (`eval` command) | No | No |
| **Clear cache** | Yes (instant) | Yes (polled) | Yes (polled) |
| **Set brightness** | Yes (instant) | Yes (polled) | Yes (polled) |
| **Set volume** | Yes (instant) | Yes (polled) | Yes (polled) |
| **Fill form field** | Yes (`fillInput` with CSS selector) | No | No |
| **Extract page content** | Yes (Readability.js) | No | No |
| **Show/hide browser** | Yes (no reload) | Yes (reloads on return) | Yes (reloads on return) |

The `eval` command allows injecting JavaScript from HA:

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_browser_command
data:
  value: '{"eval": "document.body.style.animation = ..."}'
```

### Overlay Coexistence

Ava can simultaneously display multiple overlay layers on one device:

| Layer | Can coexist with browser? |
|-------|---------------------------|
| Browser (WebView) | Base layer |
| Notification scene | Yes, appears on top |
| Floating clock | Yes, appears on top |
| Vinyl cover | Yes, appears on top |
| Weather overlay | Yes, appears on top |
| Quick Entity panel | Yes, appears on top |
| Wake ripple | Yes, appears on top |
| Screensaver (web/image) | Yes, replaces browser view |

Standalone kiosk browsers display one layer. For a clock overlay or notification popup, you would need a separate app. Ava's `OverlayZOrderCoordinator` manages all layers in one process.

### Screensaver

| Feature | Ava | Fully Kiosk | WallPanel |
|---------|-----|-------------|-----------|
| **Types** | Web page, image, Xiaomi wallpaper, clock | Image, clock | Image, clock |
| **Web page screensaver** | Yes, separate `ScreensaverWebViewService` | No | No |
| **Health monitoring** | 15s timeout, auto-recovery, 45min refresh | None | None |
| **Video compatibility** | Patches `HTMLVideoElement.play()` for AbortError | N/A | N/A |
| **Wake methods** | Proximity sensor | Motion detection | Motion/face detection |

### Tampermonkey / Userscripts

| Feature | Ava | Fully Kiosk | WallPanel |
|---------|-----|-------------|-----------|
| **Userscript support** | Yes (full GM API) | No | No |
| **Script management** | Built-in UI, install from URL/GreasyFork | N/A | N/A |
| **GM_xmlhttpRequest** | Yes (CORS-free) | N/A | N/A |
| **GM_addStyle** | Yes | N/A | N/A |
| **GM_getValue/GM_setValue** | Yes (persistent) | N/A | N/A |

Ava includes a userscript manager based on [Android-WebMonkey](https://github.com/warren-bank/Android-WebMonkey) for custom CSS/JS injection without modifying HA configuration.

### Complementary Use Cases

Ava and Fully Kiosk serve different needs and can even be used together:

| Scenario | Recommended |
|----------|-------------|
| **HA wall panel with voice + dashboard + notifications** | Ava |
| **Pure kiosk lockdown (restrict apps, disable settings)** | Fully Kiosk |
| **Multi-device fleet management** | Fully Kiosk (volume licensing) |
| **Old Android 5-7 device with outdated WebView** | Ava (GeckoView pack) |
| **Dashboard with floating clock, weather, notification overlays** | Ava |
| **Digital signage with motion detection wake** | Fully Kiosk |
| **HA-integrated dashboard with real-time control** | Ava |
| **Simple install-and-go kiosk** | Fully Kiosk |

If you already have Fully Kiosk deployed and want to add voice control and notification scenes, you can run Ava alongside it. Ava's overlay browser can display on top of Fully Kiosk, or you can use Fully Kiosk as the lockdown layer and Ava as the voice/notification layer.

---

## Render Engines

Ava supports two browser engines:

| Engine | Description | Use Case |
|--------|-------------|----------|
| System WebView | Uses Android system WebView | Most devices, lightweight |
| GeckoView | Mozilla Gecko engine pack | Old devices with outdated WebView |

### GeckoView Engine

GeckoView provides a modern browser engine for devices with outdated or missing system WebView.

**Setup:**
1. Go to **Settings** -> **Browser**
2. Select **GeckoView** as render engine
3. Tap to download the engine pack
4. When the install dialog appears, tap Install
5. Wait for installation to complete

**Notes:**
- Requires downloading the engine pack (separate APK)
- Supports arm64-v8a and armeabi-v7a
- Can check for engine updates and uninstall from settings
- If the engine pack is not installed, browser falls back to System WebView
- On low-RAM devices (1 GB), the engine pack download and decompression is heavy — if the main app crashes during install, stop the service briefly via ADB, then retry
- For manual Gecko browser control, recovery commands, and debug logcat, see [ADB Commands](ADB-Commands#gecko-engine-setup-android-7-kiosk-devices)

---

## Features

### Basic Features

| Feature | Description |
|---------|-------------|
| Full Screen | Web page fills entire screen |
| Pull to Refresh | Pull down to refresh page |
| JavaScript | Full JS support |
| Local Storage | LocalStorage supported |
| Page Scale | Initial zoom (0-500, 0=auto) |
| Font Size | Page font size percentage (50-300) |
| Touch / Drag | Toggle touch and drag interaction |

### Advanced Features

| Feature | Description |
|---------|-------------|
| Render Mode | Hardware/software rendering switch |
| Dark Mode Sync | Sync dark mode with Ava and HA dashboard |
| User Agent | Switch browser identity (Android/Desktop/macOS/iOS) |
| Tampermonkey | Userscript support |
| Browser Sidebar | Slide-out menu on the edge |
| Gesture Navigation | Swipe from left edge to go back, swipe left to go forward |
| Browser Commands | Remote control via Home Assistant |
| Sync URL | Sync current browser URL back to HA |
| Clear Cache | Clear browser cache, cookies, and history |
| Scale Slider Entity | Expose a bidirectional scale slider in HA |

---

## Settings

Go to **Settings** -> **Browser**

| Setting | Description | Default |
|---------|-------------|---------|
| Render engine | System WebView or GeckoView | System WebView |
| Pull to Refresh | Allow pull-down to refresh | On |
| Page Scale | Initial zoom (0=auto) | 0 |
| Font Size | Page font size percentage | 100 |
| Touch Enabled | Allow touch interaction | On |
| Drag Enabled | Allow drag operations | On |
| Render Mode | Hardware/software rendering | Hardware |
| Follow Dark Mode | Sync dark mode with app and HA | Off |
| User Agent | Browser identity | Default (Android) |
| Tampermonkey | Enable userscript support | Off |
| Browser Sidebar | Slide-out menu on the edge | Off |
| Gesture Navigation | Swipe to navigate back/forward | Off |
| Settings Button | Show gear icon in bottom right | Off |
| Back Key Hide | Press back key to hide browser | On |
| Sync Current URL | Sync browser URL to HA entity | Off |
| Advanced Control | Enable remote browser commands | Off |
| Scale Slider Entity | Expose scale slider in HA | Off |

### Render Mode

| Mode | Description | Use Case |
|------|-------------|----------|
| Hardware | Uses GPU, better performance | Most devices |
| Software | Uses CPU, better compatibility | Old devices or rendering issues |

### User Agent Options

| Option | Description |
|--------|-------------|
| Default (Android) | Standard Android browser |
| Desktop (Windows) | Pretend to be Windows desktop |
| macOS (Safari) | Pretend to be macOS Safari |
| iOS (iPhone) | Pretend to be iPhone Safari |

---

## Home Assistant Integration

### Recommended Dashboard Setup

Create a dedicated dashboard view for Ava:

```yaml
# configuration.yaml
lovelace:
  mode: yaml
  dashboards:
    ava-dashboard:
      mode: yaml
      filename: ava_dashboard.yaml
      title: Ava
      icon: mdi:tablet
      show_in_sidebar: false
```

### Hide HA Sidebar and Header

Use custom CSS to hide HA sidebar and header:

```css
ha-sidebar { display: none !important; }
.header { display: none !important; }
home-assistant { --app-drawer-width: 0px; }
```

### Open Web Page

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_ha_remote_url
data:
  value: "http://your-ha-address:8123/lovelace/0"
```

### Close Browser

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_ha_remote_url
data:
  value: ""
```

### Show/Hide Browser

```yaml
# Show browser
service: switch.turn_on
target:
  entity_id: switch.your_device_name_browser_display

# Hide browser
service: switch.turn_off
target:
  entity_id: switch.your_device_name_browser_display
```

### Advanced Control (Browser Commands)

After enabling Advanced Control, you can remotely control the browser via Home Assistant services.

```yaml
action: esphome.xxx_webview_command
data:
  command: 'your command'
```

| Command | Format | Description |
|---------|--------|-------------|
| Reload page | `{"reload": true}` | Reload current page |
| Clear cache | `{"clearCache": true}` | Clear browser cache |
| Set brightness | `{"brightness": 128}` | Set screen brightness (0-255) |
| Set volume | `{"volume": 50}` | Set volume (0-100) |
| Execute JS | `{"eval": "alert('hello')"}` | Execute JavaScript code |
| Clear injected | `{"eval": ""}` | Clear injected effects |

---

## Advanced Features

### Tampermonkey (Userscripts)

Ava includes a lightweight userscript manager based on the [Android-WebMonkey](https://github.com/warren-bank/Android-WebMonkey) project.

**Supported GM APIs:**
- GM_getValue / GM_setValue
- GM_addStyle
- GM_xmlhttpRequest
- GM_cookie
- GM_notification

**Not Supported:**
- GM_download
- GM_webRequest
- unsafeWindow

**How to Use:**
1. Go to **Settings** -> **Browser** -> **Tampermonkey**
2. Enable Tampermonkey
3. Tap **Add Script** to install userscripts
4. Manage installed scripts from the same page

### Browser Sidebar

A slide-out menu on the edge of the browser overlay for quick access to features.

**Setup:**
1. Go to **Settings** -> **Browser** -> **Browser Sidebar**
2. Enable sidebar
3. Choose position (left or right edge)

### Using AI to Generate Animation Effects

You can use AI tools to generate custom animation effects (cherry blossoms, snow, stars, etc.) and inject them via the `eval` command.

**Example: Cherry Blossom Animation**

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_browser_command
data:
  value: '{"eval": "!function(d,b,c,s){d=document,b=d.body,c=d.createElement(\"div\"),s=d.createElement(\"style\"),c.style.cssText=\"position:fixed;top:0;left:0;width:100%;height:100%;z-index:9999;overflow:hidden;pointer-events:none\",s.textContent=\".s{position:absolute;top:-10%;background:linear-gradient(135deg,#fff 20%,#ffb7c5 100%);border-radius:100% 0 120% 0;box-shadow:1px 1px 2px rgba(0,0,0,.1);animation:f linear infinite,w ease-in-out infinite alternate}@keyframes f{0%{opacity:0;transform:translateY(0) rotateX(0) rotateZ(0)}20%{opacity:1}100%{opacity:0;transform:translateY(110vh) rotateX(360deg) rotateZ(720deg)}}@keyframes w{0%{margin-left:-50px}100%{margin-left:50px}}\",d.head.appendChild(s),b.appendChild(c),setInterval(function(p,w,t){p=d.createElement(\"div\"),w=Math.random()*20+8,t=Math.random()*10+6,p.className=\"s\",p.style.cssText=\"left:\"+Math.random()*100+\"%%;width:\"+w+\"px;height:\"+w*1.4+\"px;animation-duration:\"+t+\"s,\"+(Math.random()*4+3)+\"s;animation-delay:-\"+Math.random()*8+\"s\",c.appendChild(p),setTimeout(function(){c.removeChild(p)},t*1e3)},100)}()"}'
```

**Clear Animation Effects**

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_browser_command
data:
  value: '{"eval": ""}'
```

---

## Large Instance Dashboard Acceleration

On large Home Assistant instances with thousands of entities, the HA frontend subscribes to **every entity** at page load (`get_states` + `subscribe_entities` with no filter). This firehose makes dashboards slow to load and sluggish, especially on low-powered kiosk devices that only need to display a handful of entities.

### HA WebSocket Stripper

[HA WebSocket Stripper](https://github.com/GabrielGoldsteinAnidea/HA-Websocket-Stripper) is an open-source HA add-on that solves this. It is a lightweight reverse proxy that serves your **real** Lovelace dashboards (real frontend, real cards, Mushroom, button-card, everything) but strips the entity WebSocket down to only what each dashboard actually uses.

**How it works:**
- Passes all HTTP traffic straight through to HA (frontend bundles, auth, registries, lovelace config, custom-card resources, untouched)
- Intercepts only `/api/websocket`:
  - Rewrites `subscribe_entities` to include only `entity_ids = <allowlist>`
  - Filters the `get_states` response to the allowlist
- The allowlist is computed from each dashboard's `lovelace/config` (card tree walk + auto-entities expansion + template entity scan), plus your `always_forward` and minus your `never_forward` overrides

**Result:** A kiosk page that previously downloaded thousands of entity states now downloads only a few dozen, with zero visual difference because it is still the real frontend.

### Installation

1. In HA, go to **Settings -> Add-ons -> Add-on Store -> Repositories**, add:
   `https://github.com/GabrielGoldsteinAnidea/HA-Websocket-Stripper`
2. Install **WebSocket Stripper**, open **Configuration**, set your dashboards:
   ```yaml
   dashboards:
     - ava-dashboard
     - home-status
   always_forward: []          # e.g. ["/^sun\\./", "person.you"]
   never_forward: []           # e.g. ["/_battery$/"]
   strip_entities: true
   ```
3. Start the add-on. Browse `http://<ha-host>:8099/ava-dashboard` (port is remappable under the add-on's **Network** tab)
4. Point Ava's browser URL to this proxy address instead of the direct HA URL

### Passwordless Kiosk Login

For wall panels and kiosks, you usually want no password prompt. The add-on supports HA's `trusted_networks` auth provider with two built-in fixes:

1. **`host_network: true`** (built in) so HA sees the real browser IP, not the Docker gateway
2. **`X-Forwarded-For` normalization** (built in) that strips IPv4-mapped IPv6 prefixes

Configure HA's `configuration.yaml`:

```yaml
http:
  use_x_forwarded_for: true
  trusted_proxies:
    - 127.0.0.1
    - ::1

homeassistant:
  auth_providers:
    - type: trusted_networks
      trusted_networks:
        - 192.168.1.0/24      # your kiosk's LAN subnet
      allow_bypass_login: true
    - type: homeassistant      # keep this for password login
```

Then restart HA Core.

### Using with Ava

Once the proxy is running, simply set Ava's browser URL to the proxy address:

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_ha_remote_url
data:
  value: "http://192.168.1.2:8099/ava-dashboard"
```

Ava's browser will load the dashboard through the proxy, getting the full real frontend but with a trimmed entity subscription. This is especially useful for:
- Old Android 5-7 devices with limited CPU and RAM
- Kiosks displaying a single dashboard with few entities
- Instances with 1000+ entities where dashboard load time exceeds 10 seconds

---

## FAQ

### Web page shows blank?

1. Check if URL is correct
2. Check network connection
3. Try switching render mode (hardware/software)
4. Try switching to GeckoView engine
5. Check if HA is accessible

### Web page loads slowly?

1. Check network speed
2. Simplify dashboard content
3. Reduce animation effects
4. Try GeckoView engine on old devices
5. If you have a large HA instance (thousands of entities), see [Large Instance Dashboard Acceleration](#large-instance-dashboard-acceleration) above

### Touch not responding?

1. Check if Touch Enabled is on
2. Check overlay permission
3. Try restarting service
4. Check if other overlays are blocking

### Keyboard doesn't work in browser?

1. This may be a WebView issue on certain devices
2. Try switching to GeckoView engine
3. Check if the system WebView is up to date

### Browser doesn't follow dark mode?

1. Enable **Follow Dark Mode** in Settings -> Browser
2. Check if Ava dark mode is enabled
3. Check if HA dashboard supports dark mode

### GeckoView engine install failed?

1. Check device architecture (arm64-v8a or armeabi-v7a required)
2. Check network connection
3. Ensure overlay permission is granted for the engine pack
4. Try again later if download fails

---

*Back to [Home](Home)*
