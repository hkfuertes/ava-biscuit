# Mod Store

The Mod Store installs and manages extension modules for Ava. Mods add new features, device-specific hardware support, and custom integrations, all without modifying Ava itself.

---

## Overview

Mods are lightweight Java modules that run inside Ava's process. They can expose new entities to Home Assistant, add device-specific hardware controls, or extend Ava with entirely new capabilities like AI assistants and Zigbee gateways.

**What Mods Can Do:**
- Add support for special hardware devices (smart screens, smart speakers, LED controllers)
- Expose sensors, switches, buttons, and text entities to Home Assistant
- Add GPIO, system interface, and service call capabilities
- Extend device attributes and automation flows
- Add custom Ava interaction logic
- Integrate open-source components like AI assistants and Zigbee coordinators

**Two categories of mods:**
- **Device Support** — hardware-specific mods for particular devices or chip platforms
- **Features** — general-purpose mods that work on any compatible Android device

---

## Access the Mod Store

1. Go to **Settings** -> **Advanced** -> **Mod Store**
2. Browse available mods
3. Install, enable, disable, or manage mods

---

## Mod Store Interface

| Section | Description |
|---------|-------------|
| Installed | Mods currently installed on this device |
| Available | Mods available for installation from the repository |
| Refresh | Check for new mods and updates |
| Manage | Configure, update, or uninstall installed mods |

---

## Managing Mods

### Install a Mod
1. Go to **Settings** -> **Advanced** -> **Mod Store**
2. Find the mod in the **Available** list
3. Tap to download
4. The voice service will restart to apply changes

### Enable / Disable
1. Find the mod in the **Installed** list
2. Tap **Enable** or **Disable**
3. The voice service restarts to apply changes

### Update
1. Tap **Update** on an installed mod when a new version is available
2. The voice service restarts to apply changes

### Uninstall
1. Find the mod in the **Installed** list
2. Tap **Manage** -> **Uninstall**
3. Confirm deletion

---

## Mod Configuration

Some mods have configurable options. After installing a mod:
1. Find the mod in the **Installed** list
2. Tap **Manage**
3. Configure available options
4. If no options are available, "This module has no configurable options" is shown

Configuration options include toggles, text fields, number inputs, and dropdown selectors. Changes take effect after the voice service restarts.

---

## Permissions

Some mods require additional permissions:
- If a mod requires a permission that hasn't been granted, it cannot be enabled
- If permission is denied permanently, open app settings and allow the required permission for that module
- Some mods require ADB or root access for hardware-level operations

---

## Headless Control

Mods can be enabled, disabled, and reloaded via ADB broadcast without opening the Ava UI:

```bash
# Enable a mod
adb shell am broadcast -a com.example.ava.ACTION_SET_MOD_ENABLED --es mod_id echo_dot_led --ez mod_enabled true

# Reload all enabled mods
adb shell am broadcast -a com.example.ava.ACTION_RELOAD_MOD
```

See [Mod Development](Mod-Store-Development) for full broadcast reference.

---

## Topics

| Topic | Description |
|-------|-------------|
| [Mod Catalog](Mod-Store-Catalog) | All available mods with detailed descriptions, configurations, and use cases |
| [Mod Development](Mod-Store-Development) | Complete developer guide — manifest spec, entity types, manager API, voice pipeline, device hooks, build, publishing |

---

## FAQ

### Mod Store shows no mods?
1. Tap **Refresh** to check for available mods
2. Check network connection
3. Mods may not be available for your device

### Mod installation failed?
1. Check network access
2. Check repository URL
3. Check module configuration
4. Try again later

### Mod won't enable?
1. Check if the mod requires additional permissions
2. Grant the required permission
3. If permission was denied permanently, open app settings and allow it
4. Some mods require root or ADB provisioning

### Voice service restarts after mod changes?
This is expected. After enable, disable, or update, the voice service restarts automatically to apply changes. This ensures the mod is properly loaded or unloaded.

### Can I write my own mod?
Yes. See [Mod Development](Mod-Store-Development) for the complete developer guide. Source code, build scripts, and examples are at [https://github.com/knoop7/ava-mods](https://github.com/knoop7/ava-mods).

---

*Back to [Home](Home)*
