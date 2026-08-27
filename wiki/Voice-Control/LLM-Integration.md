# LLM Integration (Claw Assistant)

[Claw Assistant](https://github.com/ha-china/ha_claw) is the recommended LLM hub for Ava. It transforms Home Assistant's conversation pipeline with multi-turn tool calls, agent cascading, and workspace-based persona system — giving Ava true AI agency rather than single-shot intent matching.

---

## Why Claw Assistant?

HA's native conversation system is single-shot: one AI call, one result. When you say "set the living room to movie mode," the AI needs to dim lights, adjust brightness, turn on the TV, switch input, and close curtains — a single tool call can't handle that.

Claw Assistant fixes this by injecting a **multi-turn tool call loop** into HA's pipeline: the AI calls a tool, examines the result, decides whether to continue, calls another tool, and repeats until the task is complete.

---

## Ava Device Detection

Claw Assistant has **built-in Ava identity awareness**. It automatically detects Ava devices in the HA device registry — this is not a network/online check. It inspects the HA device and entity registries to identify Ava voice satellites.

### How Detection Works

When a conversation turn starts, Claw Assistant checks the device behind the `assist_satellite` entity:

1. **Manufacturer check** — device manufacturer is `ava` or `voice assistant`
2. **Model marker check** — device model contains `ava.voice_satellite` or `ava_android`
3. **Entity fingerprint check** — the device has ESPHome entities with suffixes like `voice_command`, `assistant_response`, or `mute_microphone`

If any of these match, the device is verified as an Ava voice satellite.

### What Happens After Detection

Once an Ava device is identified, Claw Assistant automatically:

- **Resolves device context** — extracts the device name, area, ESPHome host IP, and firmware version from the HA registry
- **Collects control entities** — finds all ESPHome entities registered to this device that match Ava's control suffixes: `screen_toggle`, `mute_microphone`, `lock_screen`, `restart_service`, `media_player`, `microphone_volume`, `play_wake_sound`, `stop_alarm`
- **Builds a voice system prompt** — injects self-identity, device facts, entity list, voice intent map, and ADB runbook into the LLM's system prompt for this conversation turn
- **Merges with existing prompt** — the Ava-specific prompt is appended to whatever workspace/persona prompt is already configured, without overwriting it

This means the AI knows **which device it's speaking from**, what entities it can control on that device, and how to execute commands via HA services or ADB.

### Voice Intent Map

The AI automatically understands these spoken commands and maps them to device actions:

| Spoken Phrase | Action |
|---------------|--------|
| "turn on screen" / "wake screen" | `switch.turn_on` → `screen_toggle` |
| "turn off screen" / "screen off" | `switch.turn_off` → `screen_toggle` |
| "lock screen" | `lock.lock` → `lock_screen` |
| "unlock" | `lock.unlock` → `lock_screen` |
| "mute" / "turn off microphone" | `switch.turn_on` → `mute_microphone` |
| "unmute" / "turn on microphone" | `switch.turn_off` → `mute_microphone` |
| "restart Ava service" | `button.press` → `restart_service` |
| "stop alarm" | `button.press` → `stop_alarm` |
| "stop" / "stop talking" | ADB broadcast `ACTION_STOP` |
| "wake device" | `switch.turn_on` → `screen_toggle` or ADB `ACTION_WAKE` |
| "microphone volume up/down" | `number.set_value` → `microphone_volume` |
| "play/pause/next/volume" | `media_player.*` → `media_player` on this device |

### ADB Runbook

For actions that can't be done via HA entities, the AI can use `ExecutePython` to run ADB commands directly against the Ava device. Claw Assistant injects a ready-to-use Python template with the device's LAN IP:

```python
import subprocess, shlex
HOST = "192.168.x.x"  # this Ava device's LAN host/IP
def adb(args, timeout=8):
    cmd = ["adb", "-s", f"{HOST}:5555"] + shlex.split(args)
    p = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    return {"rc": p.returncode, "out": p.stdout.strip(), "err": p.stderr.strip()}
subprocess.run(["adb", "connect", f"{HOST}:5555"], capture_output=True, text=True, timeout=5)
print(adb("shell am broadcast -a com.example.ava.ACTION_WAKE"))
```

**Ava control broadcasts available to the AI:**

| Broadcast Action | Description |
|-----------------|-------------|
| `ACTION_WAKE` | Wake screen |
| `ACTION_STOP` | Stop current voice session |
| `ACTION_MUTE_MIC` | Mute microphone |
| `ACTION_UNMUTE_MIC` | Unmute microphone |
| `ACTION_TOGGLE_MIC` | Toggle microphone mute |
| `ACTION_START_SERVICE` | Start Ava voice satellite service |
| `ACTION_STOP_SERVICE` | Stop Ava voice satellite service |

**Ava permission grant broadcasts:**

| Broadcast Action | Permission |
|-----------------|------------|
| `ACTION_GRANT_RECORD_AUDIO` | RECORD_AUDIO |
| `ACTION_GRANT_CAMERA` | CAMERA |
| `ACTION_GRANT_LOCATION` | Location permissions |
| `ACTION_GRANT_BLUETOOTH` | BT_SCAN / CONNECT / ADVERTISE + location |
| `ACTION_GRANT_NOTIFICATIONS` | POST_NOTIFICATIONS |
| `ACTION_GRANT_OVERLAY` | SYSTEM_ALERT_WINDOW via appops |
| `ACTION_GRANT_WRITE_SETTINGS` | WRITE_SETTINGS via appops |
| `ACTION_GRANT_SECURE_SETTINGS` | WRITE_SECURE_SETTINGS |
| `ACTION_GRANT_INSTALL_PACKAGES` | REQUEST_INSTALL_PACKAGES via appops |
| `ACTION_ACTIVATE_DEVICE_ADMIN` | dpm set-active-admin |

---

## Installation

### Prerequisites

- Home Assistant 2025.12+
- At least one AI Conversation Agent integration (OpenAI / Google AI / Claude / Ollama, etc.)
- Ava device already added to HA via ESPHome integration

### HACS Install (Recommended)

1. In HACS: Settings → Integrations → add repository `https://github.com/ha-china/ha_claw`
2. Click install on "Claw Assistant"
3. Restart Home Assistant
4. Settings → Devices & Services → Add Integration → search "Claw Assistant"

### Manual Install

1. Download the code and place the `claw_assistant` directory into `config/custom_components/`
2. Restart Home Assistant
3. Settings → Devices & Services → Add Integration → search "Claw Assistant"

---

## Configuration

### Agent Settings

When adding the integration, configure your AI agents:

| Setting | Description | Required |
|---------|-------------|----------|
| Primary Agent | Main LLM conversation agent (e.g., OpenAI, Claude) | Yes |
| Fallback Agent | Secondary agent if primary fails | Yes |
| Tertiary Fallback | Optional third-tier fallback | No |

The system includes **Adaptive Memory** — it automatically tracks each agent's success/failure rate and intelligently skips known-incompatible models.

### Conversation Settings

Accessible via Configure → Conversation Settings:

**Conversation Mode:**

| Mode | Description |
|------|-------------|
| `no_name` | Most concise, saves tokens |
| `add_name` | Recommended, balances accuracy and cost |
| `detailed` | Most complete, for complex scenarios |

**Display Options:**

- Streaming output (typewriter effect)
- Continuous conversation (seamless multi-turn, no re-trigger needed)
- Context status bar (view AI work progress)
- File upload
- Rich Markdown rendering

**Runtime Parameters:**

- Tool loop limit: 3–50 rounds (default 15)
- Pipeline timeout: 5–360 minutes (default 15)

### Workspace Editor

Define all AI behavior through 8 Markdown documents, editable directly in HA UI (Configure → Workspace Editor):

| Document | Purpose |
|----------|---------|
| `IDENTITY.md` | AI identity (name, role) |
| `SOUL.md` | Personality and response style |
| `USER.md` | User profile (name, timezone, preferences) |
| `MEMORY.md` | Persistent long-term memory |
| `TOOLS.md` | Tool usage preferences and constraints |
| `AGENTS.md` | Multi-Agent collaboration rules |
| `BOOTSTRAP.md` | First-install setup flow |
| `HEARTBEAT.md` | Scheduled tasks and reminders |

### Skill System

Skills are Markdown instruction files that the AI matches and loads based on user intent:

- Install via conversation: "Install a weather briefing skill for me"
- Manage via Configure → Skill Editor
- AI can propose new skills (requires human approval via ProposeSelfEdit)

### Slash Commands

| Command | Description |
|---------|-------------|
| `/new` | Start a new conversation |
| `/reset` | Clear history and state |
| `/stop` | Abort current task |
| `/history` | Manage conversation history |
| `/skill` | View/invoke skills |
| `/model` | List/switch agents |
| `/help` | Help |
| `/commands` | List all commands |

### Scheduled Tasks (Heartbeat)

The AI can create scheduled check tasks and proactively notify you:

- "Tell me the weather every morning at 8 AM"
- "Check the server status every hour"
- "Alert me when the stock price drops below XX"

---

## 50+ Built-in Tools

**Device & Control:**
- **ServiceCall** — Call any HA service
- **BatchControl** — Control multiple devices in one command
- **CameraCapture** — Camera snapshot / live frame analysis
- **MediaAnalyze** — Upload media for AI analysis
- **IntentCall** — Invoke third-party intent handlers

**Automation & Scripts:**
- **Automation** — Full CRUD for automations
- **Script** — Script management and execution
- **HelperManager** — Create/delete HA Helpers

**Dashboard:**
- **DashboardCard** — Dynamically create/edit Lovelace cards

**System Management:**
- **HAControl** — Shell commands, integration reload, logs, diagnostics
- **ConfigEntries** — Integration install/configure/delete
- **HACS** — Manage HACS store
- **ConfigFile** — Read/write HA config files (with staging + confirmation)
- **Registry** — Area/floor/label/category/entity registry management
- **ExecutePython** — Execute Python code (with sandbox isolation)

**Information & Search:**
- **WebSearch + UrlFetch** — Web search and content extraction
- **HistoryQuery** — Entity history queries
- **SmartDiscovery** — Intelligent entity discovery by person/area/state/type

**AI Self-Governance:**
- **InstallSkill / DeleteSkill** — Skill management
- **ProposeSelfEdit** — AI proposes self-modifications (human approval required)
- **MemoryGraph** — Long-term graph memory (SQLite + FTS5)
- **ConversationMemory** — Conversation-level memory
- **AgentHandoff** — Inter-AI consultation
- **ParallelToolCall** — Parallel multi-tool execution

---

## Architecture

```
User Input
  |
  +-- Slash command? --> Handle directly (/new /stop /skill ...)
  |
  +-- Simple intent? --> HA native engine
  |
  +-- Complex request --> Orchestrator
                           |
                           +-- Build System Prompt (Workspace + Skill + Memory + Entity context)
                           +-- Ava device detection → inject voice system prompt
                           |
                           +-- Agent cascading execution
                           |   +-- Primary Agent --> Success? Return
                           |   +-- Fail -> Fallback Agent --> Success? Return
                           |   +-- Fail -> Tertiary fallback --> Return
                           |
                           +-- Multi-turn tool loop (up to 50 rounds)
                           |   +-- AI calls a tool
                           |   +-- Execute tool, return result
                           |   +-- AI analyzes, decides next step
                           |   +-- Repeat until complete or limit reached
                           |
                           +-- Response processing
                               +-- Text cleanup & formatting
                               +-- Markdown rendering
                               +-- Streaming output
```

---

## Security

- 100% open source — every line auditable on [GitHub](https://github.com/ha-china/ha_claw)
- All user data stored in HA's `.storage/claw_assistant/` directory
- No third-party cloud dependency (except your AI model)
- ProposeSelfEdit requires human approval — AI cannot silently modify itself
- Uninstalling via Settings → Integrations automatically reverts all hooks

---

## With Ava

When Claw Assistant is set as the conversation engine in the HA Assist Pipeline:

1. User speaks to Ava → audio sent to HA → Claw Assistant processes
2. Claw Assistant detects the Ava device and injects device-specific context
3. The AI knows the device name, area, available entities, and ADB commands
4. The AI can control the Ava device itself (screen, mic, media) plus all HA entities
5. Response is sent back to Ava for TTS playback

**Priority rule:** The AI uses HA entity services first (screen_toggle, mute_microphone, etc.). If the entity is missing or the action requires Android native access (wake screen, read dumpsys, grant permissions), it falls back to ExecutePython + ADB.

---

*Back to [Voice Control](Voice-Control)*
