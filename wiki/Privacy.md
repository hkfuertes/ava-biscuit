# Privacy

Your privacy matters. This page explains exactly what Ava collects, what it cannot see, how the ESPHome protocol protects your data, and how to opt out entirely if you want to.

---

## The Short Version

- Ava uses **Microsoft Clarity** for anonymous install counting and crash diagnostics
- **Ava cannot see your screen content**, Android's overlay security model makes this technically impossible
- Voice audio and camera data go **only to your Home Assistant** on your local network
- ESPHome protocol supports **Noise Framework encryption** (ChaCha20-Poly1305) between Ava and HA
- mDNS discovery is **local network only**, no cloud, no phone-home
- No account, no login, no personal data collected
- If you are uncomfortable with any telemetry, **block external network access**, Ava still works perfectly

---

## What Ava Collects

### Microsoft Clarity (Telemetry)

Ava integrates Microsoft Clarity to get a rough idea of how many devices are actively running the app. This helps the developer prioritize features and fix crashes.

#### Why Microsoft Clarity was chosen, transparency over obscurity

This was a deliberate design decision from the very beginning. Many developers use obscure, niche analytics services that fly under the radar of most security tools, services with unknown privacy policies, unknown data practices, and unknown ownership. These services are specifically chosen by bad actors to bypass the network filtering that most routers and DNS-based ad blockers already provide.

Ava chose the opposite approach. Microsoft Clarity is:

- **Well-known to every security tool**, every Pi-hole blocklist, every ad blocker, every firewall app, every enterprise network filter knows `m.clarity.ms`. You don't need a special blocklist to stop it.
- **Easy to block**, a single DNS entry disables it completely. No fallback domains, no IP rotation, no obfuscation.
- **Published privacy policy**, Microsoft's privacy practices are public, audited, and legally enforceable under multiple jurisdictions

If Ava used a hidden analytics service, most users would never know it exists, never know what data it collects, and never be able to block it. By choosing a well-known, easily-blockable service, Ava ensures that you are always in control. The fact that your router or DNS filter can block it with a single rule is a feature, not a limitation, it means the telemetry is transparent and opt-out by design.

**What Clarity sees:**
- Anonymous session count (how many devices are running)
- App crash reports and stack traces
- Basic device info (Android version, app version)

**What Clarity does NOT see:**
- Your screen content (see "Android Overlay Security" below for why this is impossible)
- Your voice audio
- Your Home Assistant data or automations
- Your personal information, name, or location
- Your network credentials or WiFi password

**When Clarity activates:**
- Only after the Voice Satellite service starts (not on app launch)
- Only if the device can reach `m.clarity.ms` (if your network blocks it, Clarity silently does nothing)
- Clarity cache is cleared on every app launch, no persistent tracking data accumulates

**Read Microsoft Clarity's own privacy policy yourself:**
- [Microsoft Clarity Privacy](https://clarity.microsoft.com/privacy)
- [Microsoft Privacy Statement](https://privacy.microsoft.com/en-us/privacystatement)
- [Clarity Data Disclosure Guide](https://learn.microsoft.com/en-us/clarity/setup-and-installation/privacy-disclosure)

You do not need to take our word for it. Read Microsoft's own documents and make your own judgment.

### Voice Audio

- Wake word detection runs **entirely on-device** (microWakeWord TFLite or vsWakeWord ONNX)
- Voice recordings are sent **only to your Home Assistant** via ESPHome protocol on your local network
- Ava does not send voice audio to Microsoft, Google, or any third-party server
- If you block external internet access, voice still works, it only needs LAN access to HA

### Camera

- Camera frames are sent **only to your Home Assistant**
- Face detection and gender detection run **on-device**
- No camera data leaves your local network

---

## Why Ava Cannot See Your Screen

This is a fundamental Android security design, not a setting you need to enable. You can verify it yourself.

### Android Overlay Security Model

Ava's floating windows (clock, weather, notifications, subtitles, Quick Entity panel) and the browser overlay use Android's `SYSTEM_ALERT_WINDOW` permission. This lets Ava draw UI elements **on top of** other apps, including the Home Assistant dashboard rendered by the browser.

This overlay design was chosen not just for visual reasons, but as a deliberate security boundary. The browser renders your HA dashboard (with all your entities, sensors, cameras, and automations) inside a WebView/GeckoView sandbox. Ava's floating windows sit on top of this sandbox. Even if an attacker somehow compromised Ava's process remotely, they still could not read what is rendered underneath, because Android's overlay security model prevents it at the OS level.

But Android's security model has a strict boundary:

- **Overlays can draw ON TOP of other apps, but cannot READ what is underneath them.**
- An overlay window does not receive the pixel content of the app below it.
- There is no Android API that allows an overlay to capture or record the screen beneath it.
- Screen capture requires a completely separate permission (`MediaProjection`), which Android enforces with a mandatory user-visible confirmation dialog every single time.

This means:

- Ava's browser overlay renders the HA dashboard inside a WebView/GeckoView sandbox, Ava's code cannot access the rendered pixels. The browser is the most critical case: your HA dashboard with all your entities, sensors, cameras, and automations is rendered inside the WebView/GeckoView process, and Ava's main process cannot read any of it
- Ava's floating clock sits on top of the browser, but Ava has no idea what your dashboard looks like underneath
- Ava's notification overlay covers part of your screen, but Ava cannot see the content underneath
- Even if Ava were remotely compromised by an attacker, the attacker still cannot see your HA dashboard, because the overlay-to-underlay read barrier is enforced by Android at the OS level, not by Ava

This is a deep architectural advantage over HACS-based panel apps. Many HACS panel apps use Android's Accessibility Service to read screen content, or run a custom HTTP server on the device that exposes screen data via REST API. If such an app is compromised, the attacker gets full screen content. Ava's overlay design means there is nothing to steal, even in a worst-case compromise.

**Read Android's own security documentation yourself:**
- [Android Security: Secure sensitive activities](https://developer.android.com/security/fraud-prevention/activities), explains how Android 12+ blocks overlays from reading screen content
- [Android Overlay Permission Reference](https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW), the official permission description

### What Would Be Needed to Capture Your Screen

To actually capture screen content on Android, an app needs all of these:

1. `MediaProjection` permission, triggers a system dialog every time, user must manually approve each session
2. `FOREGROUND_SERVICE_MEDIA_PROJECTION`, a special foreground service type that shows a persistent "Screen recording in progress" notification
3. The user sees a visible system notification during the entire recording

Ava does **not** request `MediaProjection` permission. It does not have the `FOREGROUND_SERVICE_MEDIA_PROJECTION` service type. It cannot capture your screen.

**How to verify this yourself:**
- Go to Android Settings -> Apps -> Ava -> Permissions
- You will see: Microphone, Overlay, Camera, Bluetooth, Location
- You will NOT see: Screen Record, MediaProjection, Cast Screen
- If a permission is not in the list, the app cannot use it. Android enforces this at the OS level.

### Browser Content

Ava's browser uses either Android System WebView or GeckoView. Both are sandboxed rendering engines:

- The browser loads your HA URL and renders it inside the WebView/GeckoView sandbox
- Ava's code can control the URL and inject userscripts (Tampermonkey), but cannot read the rendered pixel output
- The rendered content belongs to the WebView process, not to Ava's main process
- This is the same security boundary that Chrome, Firefox, and every Android browser uses

---

## Ava Pro vs Other Android Panel Solutions

There are several Android panel apps and HACS-installed solutions for Home Assistant. They may look similar on the surface, but the underlying architecture is fundamentally different. The key distinction is whether the solution runs custom code inside your Home Assistant, or communicates through an official, reviewed integration.

| Dimension | Ava Pro | HACS / Custom Component Panel Apps |
|-----------|---------|------------------------------------|
| Communication protocol | Official ESPHome native API (same as ESP32/ESP8266 devices) | Custom HTTP/REST or WebSocket, invented by the developer, no peer review |
| Protocol maintained by | ESPHome team (Nabu Casa, HA core developers) | Individual developer, often one person |
| Encryption | Noise Framework (ChaCha20-Poly1305, Curve25519, audited by ESPHome team) | Usually none, or self-rolled "encryption" with no review |
| Discovery mechanism | mDNS / NSD (local only, standard RFC 6762) | Often custom UDP broadcast or hardcoded IPs |
| Security audit | ESPHome protocol is used by millions of devices, reviewed by HA core team | None, nobody actually reviews the code or crypto |
| Update mechanism | In-app updater from GitHub releases (verifiable) | HACS update (installs arbitrary code from GitHub, no signature verification, runs inside HA) |
| Code execution | None, Ava is a voice satellite, it does not run arbitrary HA automations | HACS components can execute arbitrary Python in HA, a malicious HACS repo can compromise your entire HA instance |
| Screen access | Cannot read screen content (Android overlay security, see above) | Varies, some use Accessibility Service to read screen content |
| Data sent externally | Microsoft Clarity (anonymous, blockable, see above) | Unknown, many HACS components phone home to analytics services you've never heard of |
| HA data access | None, Ava cannot read HA database, entities, secrets, or automations | Full access, the HACS component runs inside HA and can read everything |
| Attack surface | Random port in range 6054-7052 (ESPHome API, optionally encrypted, no web server). Port is randomized per device to prevent predictable targeting | Often exposes fixed-port HTTP server with REST API on the device, predictable and easy to scan |

### The HACS Security Problem

### How HACS panel apps bypass ESPHome and expose your data

Most Android panel solutions installed via HACS do not use the ESPHome protocol at all. Instead, they create a custom integration that runs inside Home Assistant, and the Android app communicates with it via a custom HTTP/REST or WebSocket API. This is a fundamentally different and more dangerous architecture:

**The typical HACS panel app flow:**
1. You install a custom component from HACS, it runs as Python code inside HA
2. The component opens a custom HTTP endpoint on your HA instance
3. The Android app connects to this custom endpoint
4. The component can read any HA data, call any HA service, and make any external network request
5. The Android app sends arbitrary requests to the component, which executes them inside HA

**What this means in practice:**
- The custom component has **full HA access**, it can read all entities, all history, all secrets, all tokens
- The custom component can **phone home to any server**, the developer can add an analytics endpoint, a telemetry service, or a data collection API at any time, via a simple update
- The Android app and the HACS component are **tightly coupled**, the app sends data to the component, the component can forward it anywhere
- There is **no protocol boundary**, the app and the component share whatever data the developer wants, with no encryption, no audit, and no oversight
- The developer can **change the data flow at any time**, a HACS update can silently add new data collection without you knowing

**Ava Pro's flow (official ESPHome integration):**
1. You install Ava APK on your Android device, it is a standalone app, not a HACS component
2. Ava announces itself via mDNS on your local network (standard ESPHome discovery)
3. Home Assistant's **built-in ESPHome integration** (maintained by Nabu Casa, part of HA core) discovers Ava
4. Ava communicates with HA exclusively through the ESPHome native API on a random port in range 6054-7052
5. The ESPHome integration handles all data routing, Ava never touches HA's database, automations, or secrets

**The critical difference:**
- Ava uses HA's **official, built-in ESPHome integration**, the same integration used by millions of ESP32 devices, reviewed by HA core developers, with optional Noise encryption
- HACS panel apps use a **custom integration written by one person**, no review, no audit, no encryption, full HA access
- Ava is a **voice satellite**, it sends audio and receives responses, nothing more. It cannot read your HA data, cannot modify your automations, cannot access your secrets
- HACS panel apps are **full HA integrations**, they can do anything HA can do, including reading all your data and sending it externally

This is not a theoretical risk. The architecture of "Android app + HACS custom component" means the developer has a direct pipeline from your Home Assistant to their server, and they can activate it at any time with a HACS update that you might not even notice.
- Ava does not run any code inside Home Assistant
- Ava communicates with HA only through the ESPHome integration (which is a core HA component maintained by Nabu Casa)
- The ESPHome integration is reviewed by HA core developers and used by millions of devices
- Ava cannot read or modify your HA automations, secrets, or database, it is a voice satellite that sends audio and receives responses

### ESPHome protocol constraints vs HACS unlimited access

A deeper issue with HACS-based panel apps is that the custom component has **unlimited access** to the entire Home Assistant platform. The developer can:
- Read and modify any entity, any sensor, any automation
- Access HA's secret storage (API keys, passwords, tokens for other integrations)
- Call any HA service, including executing shell commands if the Shell Command integration is enabled
- Subscribe to all state changes across your entire smart home in real time
- Make arbitrary outbound network connections to any server, at any time
- Write files to HA's filesystem, persisting data even after the component is "uninstalled"

There is no sandbox, no permission boundary, no protocol constraint. The HACS component is just Python code running inside HA with the same privileges as HA itself.

**Ava is constrained by the ESPHome protocol.** Everything Ava does is defined by the ESPHome native API specification. The protocol only allows specific, predefined message types:
- Voice assistant audio streaming (send microphone audio, receive TTS audio)
- Sensor state reporting (temperature, battery, etc.)
- Switch and media player entity control (mute, volume, play/pause)
- Text input (send a text command to the voice assistant)
- Device information (name, version, MAC address)

Ava **cannot** do any of the following, because the ESPHome protocol does not have message types for them:
- Read arbitrary HA entities or history
- Access HA secrets or tokens
- Call arbitrary HA services
- Modify HA automations or configuration
- Subscribe to state changes of other devices
- Execute shell commands
- Write files to HA's filesystem

This is a fundamental architectural difference. HACS gives the developer a blank check to do anything HA can do. ESPHome gives Ava a strict, limited API that only supports voice satellite operations. Even if Ava's developer wanted to collect your data, the ESPHome protocol does not provide a way to do it. The protocol itself is the security boundary.

This is also why developing on the ESPHome protocol is more constrained than writing a HACS component. Every feature must fit within the protocol's message types. You cannot just "add a quick API endpoint" to read some data, because the ESPHome integration on the HA side only processes the defined message types. This constraint is by design, it is what makes ESPHome devices safe to add to your network.

### Why HACS "open source" does not mean "safe"

Many users assume that because a HACS component's code is on GitHub, it must be safe. This is a dangerous misconception:

- **Code on GitHub means the code is visible, not that anyone has read it.** Most HACS components have zero security reviews.
- **HACS does not review code.** HACS explicitly states it is not a store and does not perform any code review or security scanning.
- **A repository owner can push a malicious update at any time.** HACS will install it automatically, and the new code runs inside your HA with full access.
- **The repository can be sold or transferred.** A new owner can push a data-collection update, and you would never know unless you read every diff.
- **A custom REST API invented by one developer has not been audited by anyone.** There is no protocol-level security review.

Ava Pro's approach is different: instead of inventing a new protocol and running custom code inside HA, Ava uses the **official ESPHome native API**, the same protocol used by millions of ESP32 devices, maintained by the ESPHome team, reviewed by Home Assistant core developers, and optionally encrypted with the Noise Framework. Ava does not add any custom protocol, custom encryption, or custom API surface on top of this. Ava does not run any code inside Home Assistant.

**Read about HACS security yourself:**
- [HACS Documentation](https://hacs.xyz/), note that HACS explicitly states it is not a store and does not review code
- [Home Assistant Security Best Practices](https://www.home-assistant.io/docs/configuration/security/), official HA security guide
- [ESPHome Security Analysis](https://smarthome-aber-sicher.de/en/blog/2025/10/05/esphome-after-the-security-vulnerability-irresponsible-or-still-acceptable/), independent analysis of ESPHome security

---

## ESPHome Protocol Security

Ava communicates with Home Assistant using the ESPHome native API. This is not a custom protocol invented by Ava, it is the same protocol used by all ESPHome devices (ESP32, ESP8266, etc.) and maintained by the ESPHome team.

### Noise Framework Encryption

The ESPHome API supports optional encryption using the **Noise Protocol Framework**:

- Protocol identifier: `Noise_NNpsk0_25519_ChaChaPoly_SHA256`
- Cipher: **ChaCha20-Poly1305 AEAD** (authenticated encryption)
- Key exchange: **Curve25519** with pre-shared key (PSK)
- Handshake: completes in ~10ms (vs ~1 second for TLS)
- MAC: provides authentication for the entire encrypted payload

If you enable encryption in your ESPHome integration, all communication between Ava and Home Assistant is encrypted end-to-end. Even if someone intercepts your WiFi traffic, they cannot read your voice audio, camera frames, or control commands.

**Read the ESPHome protocol specification yourself:**
- [ESPHome Protocol Details](https://developers.esphome.io/architecture/api/protocol_details/), full technical specification of Noise encryption and frame format
- [ESPHome Native API Component](https://esphome.io/components/api/), configuration guide including encryption setup
- [ESPHome API Encryption PR #2254](https://github.com/esphome/esphome/pull/2254), the original implementation with design rationale

### mDNS Discovery (Local Only)

Ava uses mDNS (Multicast DNS) / NSD (Network Service Discovery) to announce itself on your local network so Home Assistant can find it automatically:

- Service type: `_esphomelib._tcp`
- Broadcast scope: **local network only** (multicast 224.0.0.251, TTL=255)
- mDNS packets do **not** cross routers, they stay within your LAN
- No cloud service, no phone-home, no external server involved
- This is the same discovery mechanism used by AirPlay, Chromecast, and all ESPHome devices

**Read about mDNS yourself:**
- [RFC 6762: Multicast DNS](https://datatracker.ietf.org/doc/html/rfc6762), the official standard
- [Android NSD Documentation](https://developer.android.com/ndk/guides/concepts/nsd), how Android implements it

### No Web Server

Ava does **not** run a web server. Unlike some ESPHome firmware that includes a web server component (which has been the source of security vulnerabilities in the past), Ava's ESPHome implementation is API-only:

- Port is randomized in range 6054-7052 (based on ESPHome default 6053, offset by random 1-999) is the ESPHome native API port (TCP, protobuf, optionally encrypted)
- There is no HTTP server, no web UI, no REST API exposed
- No attack surface from web-based vulnerabilities

**Read about ESPHome web server security concerns:**
- [ESPHome Security Analysis](https://smarthome-aber-sicher.de/en/blog/2025/10/05/esphome-after-the-security-vulnerability-irresponsible-or-still-acceptable/), independent security analysis recommending disabling web server

---

## Hacker / Intrusion Scenario Analysis

### Scenario 1: Someone on my WiFi tries to connect to Ava

**Without encryption enabled:**
- An attacker on the same WiFi could connect to Ava's random port in range 6054-7052
- They could send ESPHome API commands (but Ava's API surface is limited, it's a voice satellite, not a full ESPHome device with GPIO control)
- They could potentially trigger a wake event or send text commands

**With encryption enabled (recommended):**
- The Noise handshake requires the pre-shared key
- Without the correct key, the handshake fails and the connection is rejected
- The attacker cannot read or inject any traffic
- ChaCha20-Poly1305 AEAD means tampered packets are detected and dropped

**How to enable encryption:**
- In Home Assistant: ESPHome integration -> Configure -> set encryption key
- Generate a key with: `openssl rand -base64 32`
- Ava automatically uses the key configured in HA's ESPHome integration

### Scenario 2: Someone intercepts my WiFi traffic

**Without encryption:**
- Voice audio and camera frames are transmitted in cleartext
- An attacker with a WiFi sniffer could capture and replay the traffic

**With encryption:**
- All traffic is encrypted with ChaCha20-Poly1305
- The attacker sees only encrypted bytes, they cannot decode voice, camera, or commands
- The AEAD MAC prevents packet injection or modification

### Scenario 3: Someone tries to access Ava from the internet

**Ava does not expose anything to the internet:**
- The ESPHome API listens on a random port (6054-7052), bound to the local network interface
- mDNS discovery is local-only (multicast, does not cross routers)
- Ava does not open any inbound ports to the internet
- Ava does not use UPnP or NAT-PMP to open router ports

**If your router has port forwarding enabled (you did this manually):**
- Disable port forwarding for Ava's port in range 6054-7052, it should never be exposed to the internet
- This is the same advice for all ESPHome devices: [never expose ESPHome to the internet](https://smarthome-aber-sicher.de/en/blog/2025/10/05/esphome-after-the-security-vulnerability-irresponsible-or-still-acceptable/)

### Scenario 4: Malicious app installed on the same device

**Another app on your phone cannot:**
- Read Ava's voice audio (Android sandbox isolates app audio)
- Read Ava's camera frames (camera access requires separate permission, granted per-app)
- Read Ava's network traffic (Android network isolation since Android 4.0)
- Read what Ava renders on screen (overlay security model, see above)
- Access Ava's internal storage (Android sandbox, unless you granted file access)

**Another app could potentially:**
- See that Ava is running (via Android's Activity Manager, if they have usage access permission)
- See Ava's mDNS broadcast (if they are on the same device and have network access)
- Neither of these reveals any sensitive data

### Scenario 5: Ava itself is malicious (supply chain attack)

**If you don't trust Ava itself:**
- Ava Pro is closed source, but the lite flavor (3.1) source code is public: [https://github.com/knoop7/Ava](https://github.com/knoop7/Ava)
- You can use a network monitor (like Wireshark on your router) to verify exactly what traffic Ava sends
- You can use Android's built-in network traffic monitor in Settings -> Data Usage
- You can run Ava on a dedicated device with no personal data
- You can block all external network access and Ava still works as a voice satellite

---

## How to Disable Telemetry

If you prefer zero external communication, you have several options. **All of them keep Ava fully functional**, the core voice satellite feature only needs local network access.

### Option 1: Block External Network Access (Recommended)

Since Ava's core functionality (voice satellite, ESPHome protocol) only needs local network access to Home Assistant, you can safely block all external internet access:

**On your router:**
- Create a firewall rule blocking the Ava device from accessing the internet
- Or create a DNS rule blocking `m.clarity.ms` specifically
- The device still reaches Home Assistant on your LAN

**On the device (Android firewall):**
- Use [NetGuard](https://github.com/M66B/NetGuard) (open source, no root) to block Ava's internet access
- Or use AFWall+ (requires root) for per-app firewall rules
- Keep WiFi/LAN access enabled so Ava can reach Home Assistant

**On your DNS server (Pi-hole, AdGuard Home):**
- Add `m.clarity.ms` to your blocklist
- Clarity's initialization check will fail silently
- No telemetry data is sent

**Result:** Clarity never initializes. Voice satellite, browser, screensaver, floating windows, notifications, everything works normally. Only the in-app updater and Mod Store downloads (which need GitHub access) will stop working.

### Option 2: Don't Start the Voice Satellite Service

Clarity only initializes after the Voice Satellite service starts. If you never start the service, Clarity never activates. (But then Ava doesn't do much, the service is the core feature.)

### Option 3: Verify With Your Own Tools

You don't need to trust this wiki. Verify it yourself:

- **Wireshark** on your router or a mirrored port, capture all traffic from the Ava device's IP
- **Pi-hole** query log, see every DNS request the device makes
- **Android Network Logging**, Settings -> Developer Options -> Networking -> Network logger
- **NetGuard** log, shows every connection attempt per app

You should see:
- DNS query for your Home Assistant hostname (local)
- DNS query for `m.clarity.ms` (telemetry, blockable)
- TCP connection to Home Assistant on Ava's random port (ESPHome API)
- DNS query to `github.com` only when checking for updates or downloading mods

If you see anything unexpected, report it on [GitHub Issues](https://github.com/knoop7/Ava/issues).

---

## Data Flow Diagram

```
Your Voice          Your Home Assistant (your server, your data)
   |                          ^
   v                          |
[Ava Device] ----ESPHome----> |  (optionally Noise-encrypted)
   |   random port              |
   |                          |
   |  Wake word: on-device    |
   |  Voice audio: to HA only |
   |  Camera: to HA only      |
   |  Screen: NOT accessible  |
   |
   +---> m.clarity.ms (anonymous install count, crash logs)
         ^                    |
         |                    |
         +-- blocked if you disable external access
              (router firewall / NetGuard / Pi-hole)
```

---

## Summary

| Concern | Status | Verification |
|---------|--------|-------------|
| Voice audio | Sent only to Home Assistant | Wireshark capture |
| Camera data | Sent only to Home Assistant | Wireshark capture |
| Screen content | Not accessible by Ava | Android permission list |
| Personal data | Not collected | Verifiable via network monitoring |
| Account required | No | No login screen exists |
| Telemetry | Anonymous install count via Clarity | Blockable, verifiable |
| Telemetry opt-out | Block external access | NetGuard / Pi-hole / router |
| Wake word processing | 100% on-device | No network during idle |
| Face/gender detection | 100% on-device | No network during detection |
| ESPHome protocol | Optionally Noise-encrypted | Enable encryption in HA |
| mDNS discovery | Local network only | Does not cross routers |
| Web server | None | No HTTP port exposed |
| Internet exposure | None | No port forwarding needed |

---

## References

**Read these yourself. Verify everything.**

- [Microsoft Clarity Privacy Policy](https://clarity.microsoft.com/privacy)
- [Microsoft Privacy Statement](https://privacy.microsoft.com/en-us/privacystatement)
- [ESPHome Protocol Details (Noise Encryption)](https://developers.esphome.io/architecture/api/protocol_details/)
- [ESPHome Native API Component](https://esphome.io/components/api/)
- [ESPHome API Encryption Implementation](https://github.com/esphome/esphome/pull/2254)
- [ESPHome Security Best Practices](https://smarthome-aber-sicher.de/en/blog/2025/10/05/esphome-after-the-security-vulnerability-irresponsible-or-still-acceptable/)
- [Android Overlay Security (SYSTEM_ALERT_WINDOW)](https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW)
- [Android Secure Sensitive Activities](https://developer.android.com/security/fraud-prevention/activities)
- [RFC 6762: Multicast DNS](https://datatracker.ietf.org/doc/html/rfc6762)
- [NetGuard (Open Source Android Firewall)](https://github.com/M66B/NetGuard)
- [Ava Source Code](https://github.com/knoop7/Ava)

---

*Back to [Home](Home)*
