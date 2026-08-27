# Music Playback

Ava can serve as a Home Assistant media player, playing music and voice announcements. It also supports Music Assistant integration via Sendspin for multi-room audio sync.

---

## Overview

Ava supports the following audio features:
- Media playback (music, podcasts, etc.)
- TTS voice announcements
- Wake prompt sounds
- Timer sounds
- Music Assistant multi-room sync (via [Sendspin](Sendspin))

---

## Media Player

### Entity Type

In Home Assistant, Ava registers as a media player entity:
- `media_player.your_device_name`

### Supported Operations

| Operation | Description |
|-----------|-------------|
| Play | Play media |
| Pause | Pause playback |
| Stop | Stop playback |
| Volume | 0-100% |
| Mute | Enable/disable mute |

---

## Playing Music

### Via Home Assistant Service

```yaml
service: media_player.play_media
target:
  entity_id: media_player.your_device_name
data:
  media_content_id: "http://example.com/music.mp3"
  media_content_type: "music"
```

### Supported Formats

| Format | Description |
|--------|-------------|
| MP3 | Most common |
| AAC | Apple format |
| OGG | Open source format |
| WAV | Lossless format |
| FLAC | Lossless compressed |

### Supported Sources

- HTTP/HTTPS URLs
- Local file paths
- Streaming URLs

---

## Vinyl Record Cover

When playing music, Ava can display beautiful vinyl record style album covers.

### How to Enable

1. Go to **Settings** -> **Interaction**
2. Find **Media Player**
3. Turn on **Home Assistant Media Controls** or **Music Assistant Media Controls**

### HA Media Player Entity

Set the Home Assistant media player entity ID for album art:

1. Go to **Settings** -> **Interaction** -> **Media Player**
2. Find **Home Assistant Media Player**
3. Enter entity ID, e.g. `media_player.xxx`
4. Album art only works when album display is enabled

### Set Cover

```yaml
service: esphome.your_device_name_media_cover
data:
  url: "http://example.com/cover.jpg"
```

---

## TTS Voice Announcements

### Via Home Assistant

```yaml
service: tts.speak
target:
  entity_id: media_player.your_device_name
data:
  message: "Hello, welcome home"
```

### TTS Engines

Ava uses the TTS engine configured in Home Assistant. Recommended:
- Piper (local, fast)
- Google TTS (online, good quality)
- Azure TTS (online, good quality)

---

## Volume Control

### Set Volume

```yaml
service: media_player.volume_set
target:
  entity_id: media_player.your_device_name
data:
  volume_level: 0.8  # 0.0 - 1.0
```

### Mute

```yaml
service: media_player.volume_mute
target:
  entity_id: media_player.your_device_name
data:
  is_volume_muted: true
```

---

## Conversation Subtitles

Display text content of voice conversations as a floating window.

### How to Enable

1. Go to **Settings** -> **Interaction**
2. Turn on **Floating Subtitle** switch

---

## Music Assistant Integration

Ava supports [Music Assistant](https://music-assistant.io/) for multi-room audio sync via the Sendspin protocol. See [Sendspin](Sendspin) for detailed setup.

### Settings

| Setting | Description |
|---------|-------------|
| Music Assistant Media Controls | Show music controls during Music Assistant playback |
| Sendspin Volume Control | Device Volume or Software Volume |
| Sendspin Optimization Mode | Recommended for low-memory devices |
| Sendspin Sync Offset | Fine-tune playback timing in milliseconds |
| Sendspin Audio Format | Audio codec and quality |
| Sendspin Device Name | Name shown on Music Assistant server |
| Sync device volume | Keep all related media volumes aligned |

---

## Settings Summary

| Setting | Location | Description | Default |
|---------|----------|-------------|---------|
| HA Media Player | Interaction -> Media Player | Entity ID for album art | - |
| HA Media Controls | Interaction -> Media Player | Show music controls | Off |
| Music Assistant Media Controls | Interaction -> Media Player | Show MA music controls | Off |
| Floating Subtitle | Interaction | Show conversation text | Off |

---

## Home Assistant Services

### Play Media

```yaml
service: media_player.play_media
target:
  entity_id: media_player.your_device_name
data:
  media_content_id: "http://example.com/music.mp3"
  media_content_type: "music"
```

### Pause / Resume / Stop

```yaml
service: media_player.media_pause
target:
  entity_id: media_player.your_device_name
```

```yaml
service: media_player.media_play
target:
  entity_id: media_player.your_device_name
```

```yaml
service: media_player.media_stop
target:
  entity_id: media_player.your_device_name
```

### Set Volume

```yaml
service: media_player.volume_set
target:
  entity_id: media_player.your_device_name
data:
  volume_level: 0.8
```

---

## FAQ

### Music not playing?

1. Check if URL is accessible
2. Check if audio format is supported
3. Check if volume is muted
4. Check Home Assistant connection

### Cover not showing?

1. Check if Media Controls is enabled
2. Check if HA Media Player entity ID is set correctly
3. Check network connection
4. Try manually setting cover URL

### Volume too low?

1. Increase volume in Ava settings
2. Increase device system volume
3. Check if mute is enabled

### Music out of sync with other rooms?

See [Sendspin](Sendspin) for multi-room sync configuration. Adjust Sendspin Sync Offset in Settings -> Extensions -> Media Player.

---

*Back to [Home](Home)*
