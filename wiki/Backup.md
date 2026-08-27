# Backup & Restore

Export and import Ava settings, configurations, and mods for device migration or disaster recovery.

---

## Overview

Ava's backup system allows you to export your complete configuration to a JSON file and restore it on the same or another device. This includes all settings, mod configurations, and optionally system permissions.

**What Gets Backed Up:**
- All Ava settings (voice, browser, screensaver, notifications, etc.)
- Mod configurations and enabled states
- Optional: Remote browser URL
- Optional: System permissions (requires root/Shizuku)

**Format:** JSON file with `.ava-backup` format, version 1

---

## Access Backup & Restore

1. Go to **Settings** → **Backup & Restore**
2. Choose **Export** or **Import** tab
3. Select what to include in the backup
4. Export saves to Downloads/Share, Import restores from a file

---

## Export

### Export Options

| Option | Default | Description |
|--------|---------|-------------|
| Settings | ✓ | All Ava app settings (always included) |
| Mods | ✓ | Mod configurations and enabled states |
| Remote Browser URL | ✗ | Browser remote URL configuration |
| System Permissions | ✗ | Root/Shizuku permissions (requires root) |

### Export Process

1. Tap **Export Backup**
2. Select what to include (Mods, Remote URL, System)
3. Choose share destination (Downloads, cloud storage, etc.)
4. File is named: `ava-YYYYMMDD-HHMMSS-backup.json`

### Export File Structure

```json
{
  "format": "ava-backup",
  "version": 1,
  "timestamp": "2024-01-15T14:30:00Z",
  "includes": {
    "settings": true,
    "mods": true,
    "remoteBrowserUrl": false,
    "systemPermissions": false
  },
  "data": {
    "settings": { ... },
    "mods": { ... },
    "remoteBrowserUrl": "https://...",
    "systemPermissions": { ... }
  }
}
```

---

## Import

### Import Process

1. Tap **Import Backup**
2. Select backup file from storage
3. Ava analyzes the file and shows what's included
4. Confirm import options
5. Import applies settings and restarts if needed

### Import Behavior

- **Settings:** All imported settings overwrite current ones
- **Mods:** Mod configurations are applied, mods are enabled/disabled as needed
- **Remote URL:** Browser remote URL is updated
- **System Permissions:** Requires root/Shizuku, may need ADB commands

### Import Results

- **Success:** Toast notification "Import successful"
- **Failed:** Toast notification "Import failed"
- **Satellite Restart:** Voice satellite restarts automatically if needed

---

## Use Cases

### Device Migration
Moving Ava from one Android device to another:
1. Export backup from old device
2. Transfer backup file to new device
3. Install Ava on new device
4. Import backup
5. Verify all settings and mods work

### Disaster Recovery
Restore Ava after factory reset or app corruption:
1. Have a recent backup file
2. Reinstall Ava
3. Import backup
4. Test all functionality

### Configuration Sync
Keep multiple Ava devices with identical settings:
1. Configure one device perfectly
2. Export backup
3. Import on other devices

---

## Backup File Management

### File Naming
- Format: `ava-YYYYMMDD-HHMMSS-backup.json`
- Example: `ava-20240115-143000-backup.json`
- Timestamp in UTC

### File Location
- Export: Shared via Android share system
- Store in cloud storage for safety
- Keep multiple versions for rollback

### File Security
- Contains all your Ava configuration
- May contain sensitive data (URLs, permissions)
- Store securely, share only with trusted devices

---

## Troubleshooting

### Import Fails
1. Verify file is a valid Ava backup
2. Check file isn't corrupted
3. Ensure Ava version compatibility
4. Try exporting fresh backup from source

### Mods Not Working After Import
1. Check if mods are installed in Mod Store
2. Reinstall missing mods
3. Re-enable mods in settings
4. Restart voice satellite

### System Permissions Not Applied
1. Ensure device has root or Shizuku
2. Run ADB commands if needed
3. Check permission grant status
4. Re-import with system permissions

### Voice Satellite Issues
1. Voice satellite restarts automatically
2. If still issues, manual restart:
   - Settings → Voice Satellite → Restart
   - Or: `adb shell am broadcast -a com.example.ava.ACTION_RESTART_VOICE_SATELLITE`

---

## Advanced

### Manual Backup Inspection
Backup files are JSON, can be viewed in any text editor:
- Check `includes` section for what's backed up
- Verify `data` section contents
- Manual editing not recommended

### Partial Imports
Import only what you need:
- Uncheck unwanted options during import
- Settings always imported (cannot be deselected)
- Use selective imports for testing

### Version Compatibility
- Format version 1 (current)
- Future versions will maintain backward compatibility
- Old backups work on newer Ava versions

---

## Best Practices

1. **Regular Backups:** Export monthly or after major changes
2. **Multiple Copies:** Keep backups in different locations
3. **Test Restores:** Verify backup works before relying on it
4. **Version Control:** Keep several backup versions
5. **Security:** Store backups securely, don't share publicly

---

*Back to [Home](Home)*
