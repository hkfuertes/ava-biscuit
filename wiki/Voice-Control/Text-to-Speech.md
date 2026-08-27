# Text to Speech (Voice Replies)

Ava supports two TTS playback modes for voice assistant replies: **Standard TTS** (URL-based) and **Streaming TTS** (PCM-based). The mode is selected in Settings → Voice → Voice Replies.

---

## Standard TTS

Standard TTS waits until Home Assistant generates the full voice reply, then downloads and plays it from a URL.

**Flow:**

1. HA sends `TTS_START` with the reply text (displayed as subtitle)
2. HA sends `TTS_END` with the TTS audio URL
3. Ava downloads the audio file and plays it via ExoPlayer
4. On playback completion, Ava sends `announce_finished` to HA

**Characteristics:**

- Better compatibility — works with any HA TTS provider
- Supports floating subtitle overlay (word-by-word or full text)
- Supports wake sound and stop sound
- Higher latency — first audio is heard only after the full reply is generated and downloaded
- HTTP streaming playback with configurable connect/read timeout (30s)

**Early TTS URL:** HA may include the TTS URL in `RUN_START` before the conversation even begins. Ava caches this URL and uses it when `TTS_END` arrives, reducing latency.

---

## Streaming TTS

Streaming TTS starts playing audio while the reply is still being generated on the HA side. HA sends PCM audio chunks in real time.

**Flow:**

1. HA sends `TTS_START` — Ava enters Responding state
2. HA sends `TTS_STREAM_START` — Ava opens an `AudioTrack` (16 kHz, 16-bit, mono PCM)
3. HA sends PCM audio chunks via `VoiceAssistantAudio` messages — Ava writes them to `AudioTrack` in real time
4. HA sends `TTS_STREAM_END` — Ava drains remaining audio, then completes

**Characteristics:**

- Faster first response — audio starts playing as soon as the first PCM chunk arrives
- No floating subtitle overlay (subtitles are suppressed in streaming mode because text arrives in fragments)
- Requires HA server with streaming TTS output support (HA `SPEAKER` feature flag)
- PCM audio is played via raw `AudioTrack`, not ExoPlayer
- Volume scaling is applied per-sample on PCM data before writing to the track
- Buffered chunk handling: if PCM chunks arrive before `TTS_STREAM_START`, they are buffered and flushed when the stream opens (max 256 frames)

**Feature flag negotiation:** When streaming TTS is enabled in settings, Ava sets the `SPEAKER` feature flag in the `VoiceAssistantConfigurationRequest`. HA uses this to decide whether to send PCM streams.

---

## Mode Comparison

| Feature | Standard TTS | Streaming TTS |
|---------|-------------|---------------|
| First audio latency | Higher (wait for full reply) | Lower (play while generating) |
| Floating subtitles | Yes | No |
| Wake sound | Yes | Yes |
| Stop sound | Yes | Yes |
| Server requirement | Any HA TTS provider | HA with streaming output support |
| Playback engine | ExoPlayer (HTTP) | AudioTrack (raw PCM) |
| Audio format | Any (URL-based) | 16 kHz 16-bit mono PCM |
| Whisper response | Yes | Yes |

---

## Announcement Playback

Ava supports HA's `VoiceAssistantAnnounceRequest` for proactive announcements (e.g., timer finished, ask_question). This is separate from the conversation TTS flow.

**Flow:**

1. HA sends `AnnounceRequest` with `media_id` (and optional `preannounce_media_id`)
2. Ava ducks media playback, plays the announcement audio
3. On completion, Ava sends `announce_finished` to HA
4. If `start_conversation=true`, Ava starts a new voice pipeline to listen for the user's response (10s timeout)

**Preannounce:** If a `preannounce_media_id` is provided, Ava plays it first (e.g., "Attention:"), then plays the main media. A 3-second load timeout skips the preannounce if it fails to start.

---

## Whisper Response

Ava can temporarily lower the playback volume for wake sounds and TTS replies, useful for bedside or quiet environments.

**Settings:**

| Setting | Description | Default |
|---------|-------------|---------|
| Voice playback volume | Enable whisper mode | Off |
| Adaptive | Lower volume only when speech/ambient is quiet | On |
| Quiet volume | Volume level for whisper mode (15%–100%) | 30% |

**Adaptive mode:**

- **Wake sound:** Measures ambient mic level before wake detection. If below threshold (0.04 RMS), lowers wake sound volume.
- **TTS reply:** Measures user's speech peak during the session. If below threshold, lowers TTS playback volume.
- After TTS finishes, the original system volume is restored.

**Fixed mode:** Always uses the configured quiet volume for wake sound and TTS, regardless of ambient/speech level.

> Above 30% quiet volume, the toast warns that whisper mode won't effectively avoid disturbing others.

---

## Pipeline Error Messages (i18n)

Ava displays localized toast messages for voice pipeline errors. Error messages are determined by the error code from HA and displayed in the user's locale.

**Supported languages:** English (default), Chinese (zh), German (de), Russian (ru), Portuguese (pt), Vietnamese (vi).

**Locale detection:** Ava uses `LocaleUtils` to detect the system locale at runtime. If the locale is not one of the supported languages, English is used as fallback.

### Error Codes and Messages

| Error Code Pattern | String Key | English Message |
|--------------------|------------|-----------------|
| `stt-no-text` | `pipeline_error_no_speech` | Speech detected, no text |
| `timeout` / `timed-out` | `pipeline_error_no_response` | Response timed out, possibly network delay or busy service |
| `stt-*` / `intent-*` / `tts*` | `pipeline_error_config` | Voice processing paused, config may need a look |
| `cloud-auth` | `pipeline_error_cloud_auth` | Cloud auth issue, re-login |
| `wake` / `duplicate` | `pipeline_error_wake` | Wake signal anomaly, retry |
| (other) | `pipeline_error_unknown` | Temporary issue, will retry shortly |
| (HA disconnected) | `pipeline_error_ha_disconnected` | Server offline, check network |
| (HA start failed) | `pipeline_error_ha_start_failed` | Pipeline init issue, check config |
| (TTS playback) | `pipeline_error_tts_playback` | Voice reply playback failed: TTS address timed out, check the address |

### Localized Examples

| Language | `pipeline_error_no_speech` | `pipeline_error_tts_playback` |
|----------|---------------------------|-------------------------------|
| English | Speech detected, no text | Voice reply playback failed: TTS address timed out, check the address |
| Chinese | 检测到语音，但无文本 | 语音回复播放失败：TTS 地址超时请检查地址 |
| German | Sprache erkannt, kein Text | Sprachantwort-Wiedergabe fehlgeschlagen: TTS-Adresse timed out, Adresse prüfen |
| Russian | Речь есть, текста нет | Ошибка воспроизведения голосового ответа: тайм-аут адреса TTS, проверьте адрес |
| Portuguese | Voz detectada, sem texto | Falha na reprodução da resposta por voz: endereço TTS expirou, verifique o endereço |
| Vietnamese | Có giọng nói, không có văn bản | Phát phản hồi giọng nói thất bại: địa chỉ TTS hết thời gian, kiểm tra địa chỉ |

---

## TTS Playback Volume and Ducking

- TTS playback uses `USAGE_MEDIA` with `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`
- When TTS starts, Ava requests audio focus, which may duck other media players
- On TTS completion, audio focus is released, allowing other media to resume
- The TTS player volume follows the system media volume
- Whisper mode can temporarily override the volume during TTS playback

---

## Settings Reference

All TTS-related settings are in Settings → Voice → Voice Replies.

| Setting | Description | Default |
|---------|-------------|---------|
| TTS mode | Standard or Streaming | Standard |
| Floating subtitle overlay | Show subtitles in floating window during conversations | Off |
| Voice playback volume (Whisper) | Lower volume for quiet speech | Off |
| Adaptive whisper | Only whisper when speech is quiet | On |
| Quiet volume | Volume for whisper mode | 30% |

---

## Recommended TTS Engine

### AI Hub TTS (Kokoro)

[AI Hub TTS](https://github.com/truemanshum/ai-hub-tts) is a Home Assistant add-on based on the Kokoro model. It is the recommended TTS engine for Ava.

**Features:**

- Fully offline — no internet required after model download
- 26 high-quality voices
- CPU inference, generation < 1 second
- Wyoming protocol with auto-discovery — HA picks it up automatically
- Memory usage < 500 MB

**Installation:**

1. In Home Assistant: Settings → Add-ons → Add-on Store
2. Click ⋮ → Repositories → Add `https://github.com/truemanshum/ai-hub-tts`
3. Refresh and install AI Hub TTS
4. First start auto-downloads the model (~500 MB)
5. Settings → Voice Assistants → select AI Hub TTS as the TTS engine

**Configuration:**

```yaml
voice: af_heart      # Voice selection
sample_rate: 24000   # Sample rate
debug: false          # Debug mode
```

**With Ava:** Works in both Standard and Streaming TTS modes. For Streaming TTS, ensure the HA voice assistant pipeline has streaming output enabled.

---

*Back to [Voice Control](Voice-Control)*
