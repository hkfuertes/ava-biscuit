# Wake Word Engines

Ava has two on-device wake word engines. Both run entirely locally — no audio leaves the device for wake detection. Compatible with Android 5-16.

## vsWakeWord: Browser Engine vs Ava Pro's Native C++ Port

The original vsWakeWord by [jxlarrea](https://github.com/jxlarrea/voice-satellite-card-integration) is a browser-based engine — it runs in JavaScript inside a web page, using WebGPU compute shaders for inference. It is designed for the Voice Satellite browser extension that runs on top of Home Assistant's web UI.

Ava Pro did not wrap or bridge that JavaScript engine. We rewrote the entire vsWakeWord concept from scratch as a native C++ library for Android. Every component that the original runs in JavaScript — audio feature extraction, model inference, phoneme decoding, fuzzy matching, energy gating, confirmation logic — was independently reimplemented in C++.

| Dimension | Original (Browser) | Ava Pro (Native C++) |
|-----------|-------------------|----------------------|
| Runtime | JavaScript in browser | Native C++ library, compiled per ABI |
| Compute | WebGPU compute shaders | CPU (custom-stripped ONNX Runtime) |
| Platform | Any device with a modern browser + WebGPU | Any Android 5+ device, no browser needed |
| GPU dependency | Requires WebGPU support (many old devices don't have it) | No GPU required — runs on pure CPU, works on Android 5+ |
| Audio pipeline | Web Audio API → JS processing | Android AudioRecord → native C++ ring buffer |
| Feature extraction | JavaScript FFT + Mel filterbank | Native C++ implementation, incremental (only computes new frames) |
| Energy gate | JavaScript | Native C++, with sleep buffer replay |
| Phoneme matching | JavaScript | Native C++ |
| False wake defense | Same multi-layer concept | Same concept, reimplemented in C++ with additional optimizations |
| Latency | JS → WebGPU dispatch overhead | Direct native calls, zero JNI in hot path |
| Old device support | Needs WebGPU — most Android 5-9 devices excluded | Works on everything, including 1GB RAM tablets |
| Background operation | Browser tab must stay open | Runs as a foreground service, screen off, no browser needed |

**Why a native C++ rewrite matters:**

- **No browser dependency.** The original requires a browser tab with WebGPU. Many wall-mounted tablets and kiosk devices don't have WebGPU, or their browsers are too old. Ava Pro's C++ engine runs directly on the device with no browser at all.
- **No JavaScript overhead.** JavaScript engines add GC pauses, JIT warmup, and interpretation overhead. Ava Pro's C++ pipeline has none of that — every audio chunk is processed with predictable, constant-time native code.
- **Works as a background service.** A browser tab must stay visible and active. Ava Pro's engine runs inside a foreground service — the screen can be off, the browser closed, the device in kiosk mode, and wake word detection still works.
- **Android 5+ compatibility.** WebGPU requires Android 12+ in most browsers. Ava Pro's C++ engine works on Android 5, including devices with 1GB RAM and no GPU acceleration.
- **Custom-stripped ONNX Runtime.** Instead of the full 17MB ONNX library, Ava Pro ships a 3.4MB (arm64) / 2.0MB (armeabi) reduced build containing only the operators the wake word models need. Model loading is faster, memory footprint is smaller, and the binary loads quicker on low-end devices.

**Same model format, completely different engine.** Both use `.ort` model files and `.json` manifests with the same phoneme target structure. But the runtime that processes those models is entirely different — one is JavaScript in a browser, the other is a ground-up C++ library.

## Differences from Original brownard/Ava — Wake Word Engine Only

Ava Pro is based on [brownard/Ava](https://github.com/brownard/Ava). This section covers only the wake word engine differences.

| Dimension | brownard/Ava (original) | Ava Pro (knoop7/Ava) |
|-----------|--------------------------|----------------------|
| Engine count | 1 (microWakeWord) | 2 (microWakeWord + vsWakeWord) |
| microWakeWord engine | TFLite binary classification, sliding window threshold | Same engine, same 9 built-in models |
| vsWakeWord engine | Not available | ONNX model + fully native C++ inference pipeline (FFT, CTC decode, edit distance, energy gate — all in C++) |
| Built-in models | 9 micro (.tflite) | 9 micro (.tflite) + 3 vs (.ort) |
| Model format (micro) | .tflite + .json | .tflite + .json (identical, V2/V3 compatible) |
| Model format (vs) | N/A | .ort (ONNX) + .json manifest with phoneme targets |
| Custom model loading | DocumentTreeWakeWordProvider (SAF folder picker in Settings) | In-app import (Wake Word Library) + APK assets injection — see [Custom Wake Words](Custom-Wake-Words) |
| False wake defense | Threshold only (single layer) | Threshold (micro) or multi-layer gates (vs) |
| Inference runtime | TensorFlow Lite | TensorFlow Lite + ONNX Runtime (custom stripped build) + native C++ pipeline |
| CPU / memory footprint | Minimal | Minimal (micro) or moderate (vs, sleeps during silence) |

### Why Ava Pro runs faster than the original

The microWakeWord engine itself is identical between both apps — same models, same inference path. The speed difference comes from what happens around the engine, not inside it.

**1. Stop word detection is no longer always-on.** The original runs the stop-word model continuously alongside the wake-word model — two models inference every audio chunk, 24/7, even when nothing is happening. Ava Pro (since 0.5.2) only activates the stop-word model when it is actually useful: when a timer alarm is ringing, or when a voice session is in progress (Listening / Processing / Responding). During idle standby, the stop model is completely skipped. This cuts continuous CPU load in half during the 99% of the time the device is just waiting.

**2. vsWakeWord skips inference during silence.** When using the vsWakeWord engine, the energy gate completely bypasses ONNX inference when the room is quiet. The ONNX model only wakes up when the gate detects voice-like audio. On a quiet device sitting in a hallway, this means the ONNX engine effectively sleeps most of the time, while still catching the wake word the moment someone speaks.

**3. Buffered audio replay on gate open.** When the energy gate transitions from closed to open (someone starts speaking), Ava Pro replays the last ~1 second of buffered audio through the ONNX model in a single batch. This means the beginning of the wake word — which happened while the gate was still deciding — is not lost. The user experiences instant wake detection without waiting for the model to warm up from silence.

**4. Incremental feature extraction in native C++.** vsWakeWord extracts log-Mel features from audio in C++. Instead of recomputing the full feature window every chunk, the engine shifts the existing feature buffer and only computes the new frames. On a 1300ms window with 80ms chunks, this means computing ~6 new frames instead of ~128 every cycle — a 20x reduction in FFT work, all in native code.

**5. Adaptive gain normalization.** vsWakeWord applies a smooth adaptive gain to normalize voice volume before inference. This means the model sees consistently-leveled audio regardless of distance or microphone sensitivity. Consistent input means the CTC confidence scores are more stable, which means the confirmation gates reach their threshold in fewer attempts — faster triggering with fewer false rejects.

**6. Custom-stripped ONNX Runtime.** The ONNX Runtime shipped with Ava Pro is a custom reduced build — only the CPU execution provider, no GPU/NNAPI delegates, no training APIs. The native library is ~3.4MB (arm64) / ~2.0MB (armeabi) instead of the official 17MB. Model loading and session creation are faster, and the smaller binary loads into memory quicker on low-end devices. The tradeoff is no hardware acceleration, but for wake-word-sized models the CPU path is already fast enough and avoids the latency and compatibility issues of GPU/NNAPI on fragmented Android devices.

**Net effect:** On a typical device in idle standby, Ava Pro's wake-word CPU usage is lower than the original because stop-word inference is skipped. When someone speaks, vsWakeWord's energy gate + buffered replay + incremental features make detection feel instant despite the heavier model. The microWakeWord engine path matches the original's speed; the vsWakeWord path trades higher peak CPU for smarter gating and faster perceived response.

## Engine Comparison

| Dimension | microWakeWord | vsWakeWord |
|-----------|---------------|------------|
| Architecture | TFLite binary classification | ONNX model + fully native C++ inference engine |
| Model size | 50-80KB, uint8 quantized | ~500KB |
| Inference | 10ms frame, stride 3 | 80ms cycle, 1300ms window |
| Frontend | microfeatures | On-device audio feature extraction with smart energy gating |
| Decision | 5-frame sliding window mean > threshold | Multi-layer false wake defense (energy gate → phoneme matching → confirmation → cooldown) |
| Output | Scalar 0-1 probability | Phoneme sequence with traceability |
| Wake-word swap | **Full retraining required** | **Manifest JSON hot-swap** |
| CPU / memory | Minimal | Moderate (sleeps during silence) |
| False wake defense | Threshold only (zero-sum trade-off) | Multi-layer independent gates |
| Interpretability | None | Phoneme-level debugging |
| Best for | Low-end Android 5+ persistent background | Noise-sensitive, explainability-required deployments |

## microWakeWord (default)

microWakeWord is the default engine. It uses TensorFlow Lite with tiny quantized models. Each wake word is a separate `.tflite` model file paired with a `.json` config. The detector runs a sliding window average over the last 5 frames and triggers when the average probability exceeds the cutoff.

**Built-in micro models (9):**

| Model ID | Wake Word | Author |
|----------|-----------|--------|
| `hey_jarvis` | Hey Jarvis | Kevin Ahrendt |
| `alexa` | Alexa | Kevin Ahrendt |
| `hey_home_assistant` | Hey Home Assistant | Michael Hansen |
| `hey_mycroft` | Hey Mycroft | Kevin Ahrendt |
| `hey_luna` | Hey Luna | adamlonsdale |
| `hey_peppa_pig` | Hey Peppa Pig | Michael Hansen |
| `okay_computer` | Okay Computer | Michael Hansen |
| `okay_nabu` | OK Nabu | Kevin Ahrendt |
| `choo_choo_homie` | Choo Choo Homie | Michael Hansen |

**Stop word:** `stop` (Stop) by Kevin Ahrendt.

## vsWakeWord

vsWakeWord uses ONNX models for phoneme recognition, but the entire inference pipeline — from audio feature extraction to phoneme decoding to false wake confirmation — is implemented in pure C++. This is not a thin JNI wrapper around a Java library. Every hot-path computation (FFT, Mel filterbank, CTC decoding, edit distance, energy gate, counter logic) runs natively, with zero Java/Kotlin overhead in the audio processing loop.

Instead of a binary "is this the wake word?" classifier, it decodes audio into a phoneme sequence and matches against target phonemes. This makes it more robust to noise and accents. The native C++ implementation ensures maximum CPU cache utilization and minimal latency on low-end devices.

**Built-in vs models (3):**

| Model ID | Wake Word | Type |
|----------|-----------|------|
| `hey_jarvis` | Hey Jarvis | Wake word |
| `ok_nabu` | OK Nabu | Wake word |
| `ok_stop` | Ok Stop | Stop classifier |

### vsWakeWord Detection Pipeline

The engine uses a multi-layer gating architecture. Each layer independently filters out false wakes before the next layer runs:

**Layer 1 — Energy Gate (Sleep/Wake):** A lightweight energy gate runs before any model inference. When the room is quiet, the engine is completely asleep — no computation, no decoding. Once audio energy exceeds the wake threshold, the engine transitions to active mode. After a few seconds of continuous silence, it returns to sleep.

**Layer 2 — Buffered Audio Replay:** While the energy gate was sleeping, audio was being buffered. When the gate opens (someone starts speaking), the buffered audio is replayed through the engine in a single batch. This means the beginning of the wake word — which happened while the gate was still deciding — is not lost. The user experiences instant wake detection without waiting for the model to warm up from silence.

**Layer 3 — Phoneme Matching:** The engine decodes audio into a phoneme sequence and matches against target phoneme sequences for each registered wake word. Multiple pronunciations per wake word are supported, with fuzzy matching to tolerate minor phoneme variations.

**Layer 4 — Counter / Borderline Confirmation:** Two confirmation strategies prevent single-window false triggers:
- **Counter mode**: Requires consecutive matches (typically 2) before triggering. A soft hold mechanism tolerates brief dips between syllables when speaking quickly. High-confidence detections can bypass the counter entirely.
- **Borderline mode**: Detections near the threshold require a second confirming window within a short time frame. High-confidence detections trigger immediately.

**Layer 5 — Per-Keyword Cooldown:** After a keyword triggers, it enters a cooldown period (default 2000ms, configurable per keyword in the manifest). This prevents the same wake word from firing repeatedly during a single utterance.

**Layer 6 — Adaptive Gain Normalization:** A smooth adaptive gain normalizes voice volume before inference. This ensures the engine sees consistently-leveled audio regardless of distance or microphone sensitivity, making detection more stable and faster to trigger.

### vsWakeWord Manifest

Each vs model is a pair: `id.json` (manifest) + `id.ort` (model file). The manifest defines the wake word's phoneme targets, detection thresholds, and runtime behavior. Multiple pronunciations per wake word are supported.

Key manifest fields:
- `wake_word_targets` — phoneme ID sequences to match (multiple pronunciations allowed)
- `wake_word_target_phonemes` — human-readable phoneme sequences for debugging
- `runtime.required_hits` — how many consecutive matches needed to trigger (2 = double confirm)
- `runtime.high_confidence_bypass` — skip the hit counter if confidence is very high
- `runtime.cooldown_ms` — cooldown before the same keyword can trigger again
- `stop_classifier` — `true` for stop-word models (different gating logic)

## Stop Words

Stop words interrupt the current conversation or stop Ava's response (e.g., timer alarm).

| Stop Word | Engine | Model ID | Description |
|-----------|--------|----------|-------------|
| Stop | microWakeWord | `stop` | Default stop word for micro engine |
| Ok Stop | vsWakeWord | `ok_stop` | Stop word for vs engine (`stop_classifier: true`) |

Stop-word detection only runs when actually needed (since 0.5.2): when a timer alarm is actively ringing, or when a voice session is in progress (Listening, Processing, Responding). During idle standby with no alarm, the stop model is skipped — cutting CPU load and heat.

## Engine Switching

Each engine stores wake words independently (`microWakeWords` / `vsWakeWords`). Switching engines auto-restores the last selection — no more lost models or silent failures. Cross-engine ID mapping: micro's `okay_nabu` auto-maps to VS's `ok_nabu`. HA-configured wake words also resolve correctly across engines. Service auto-restarts on engine switch, keeping the detector in sync with settings.

## How to Change Wake Word

1. Open Ava app
2. Go to **Settings** -> **Voice Config**
3. Find **Wake Word Engine** and choose microWakeWord or vsWakeWord
4. Find **Wake Word 1** option
5. Select your preferred wake word from the list
6. Optionally configure **Wake Word 2** for dual wake word mode
7. New wake word takes effect after service restart (auto)

## Wake Word Sensitivity

Adjust sensitivity to control how easily the wake word triggers:
- Higher sensitivity = easier to trigger, but more false positives
- Lower sensitivity = fewer false positives, but may miss quiet speech

## Wake Sound

Each wake word can have its own wake sound:
- **Wake Word 1 Sound**: Played when Wake Word 1 is detected
- **Wake Word 2 Sound**: Played when Wake Word 2 is detected
- **Default Sound**: Used if no custom sound is set
- **None**: Silent recording start

## Wake Visual Feedback

Ava provides clear visual feedback during wake and conversation:

**Wake Instant:**
- Colorful ripple expanding from screen center
- Android 13+: RuntimeShader with distorted halo + star particles
- Android 7: Soft circular diffusion
- Compatibility paths for other versions

**Conversation (when Floating Subtitle is disabled):**
- Full-screen edge glow that changes with state:
  - **Listening**: Edge light breathes with microphone volume
  - **Processing**: Slow breathing animation
  - **Speaking**: Pulsates with TTS energy

**Dual Wake-Word Color Coding:**
- Wake Word 1 = green (default), Wake Word 2 = blue (default)
- Ripple and edge light match the triggered wake word
- Custom colors available in Settings → Extensions → Interface → Voice feedback colors
- 7 rainbow presets (red through purple) also available

**Technical Notes:**
- Edge glow uses pre-rendered Gaussian blur bitmaps for performance
- Ripple animation driven by system uptime (prevents Kiosk devices with "animation duration = 0" from killing the effect)
- Android 7.0/7.1 optimized to clean circular diffusion without Shader dependency

---

*Back to [Voice Control](Voice-Control)*
