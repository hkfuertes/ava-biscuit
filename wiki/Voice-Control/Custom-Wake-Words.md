# Custom Wake Words

Ava supports two ways to add custom wake words: **in-app import** (recommended, no tools needed) and **APK injection** (advanced, for power users).

Both methods support microWakeWord (`.tflite` + `.json`) and vsWakeWord (`.ort` + `.json`) model formats.

Compatible with Android 5-16.

---

## Method 1: In-App Import (Recommended)

Since 0.5.5, Ava includes a built-in Wake Word Library that lets you import custom wake word models directly in the app — no APK editing, no desktop tools required.

### Supported File Types

| Engine | Config File | Model File |
|--------|------------|------------|
| microWakeWord | `.json` | `.tflite` |
| vsWakeWord | `.json` (manifest) | `.ort` (ONNX) |

The app automatically detects which engine a `.json` file belongs to based on its content.

### Import Methods

#### Option A: Import a ZIP archive (batch)

If you have multiple wake words bundled in a `.zip` file, you can import them all at once:

1. Go to **Settings** → **Voice Config** → **Wake Word Library**
2. Tap **Import wake word files**
3. Select a `.zip` file containing your wake word files
4. The app extracts and installs all valid models automatically
5. Imported wake words appear in the library list

The ZIP file should contain `.json` + `.tflite` pairs (microWakeWord) or `.json` + `.ort` pairs (vsWakeWord). Files are grouped by their ID (filename without extension).

#### Option B: Import a single JSON config

1. Go to **Settings** → **Voice Config** → **Wake Word Library**
2. Tap **Import wake word files**
3. Select a `.json` file
4. If the JSON is a complete package (includes model data), import finishes immediately
5. If the model file is missing, the app prompts to select the matching model file (`.tflite` for microWakeWord, `.ort` for vsWakeWord)
6. After both files are imported, the wake word appears as "Ready" in the library

#### Option C: Add a model file to an existing entry

If a library entry shows "Missing model file", you can add the model file separately:

1. Tap the entry in the Wake Word Library
2. Tap **Add model file**
3. Select the matching `.tflite` or `.ort` file

### Managing Imported Wake Words

| Action | How |
|--------|-----|
| Use an imported wake word | Tap it in the library → tap **Set as wake word** |
| Delete an imported wake word | Tap it → tap **Remove** → confirm |
| Engine auto-switch | Selecting a vsWakeWord model automatically switches the engine to vsWakeWord |

> **Note:** Stop-word models (models with `stop_classifier: true`) cannot be imported through the Wake Word Library. Stop words are built-in only.

---

## Method 2: APK Injection (Advanced)

Ava Pro also loads microWakeWord models from `assets/wakeWords/` and vsWakeWord models from `assets/vswakeword/` inside the APK. You can inject custom models without building from source — just edit the APK directly.

**Tools needed:**
- Android file manager with APK editing: **MT Manager** (Chinese), **APK Editor Pro**, or **Nexus APK Editor**
- Or on desktop: `apktool` + `zipalign` + `apksigner`

### Steps (MT Manager method)

1. Download the Ava Pro lite APK from [GitHub releases](https://github.com/knoop7/Ava/releases)
2. Open MT Manager, long-press the APK, select "View" (or "Extract")
3. Navigate to `assets/wakeWords/` (for microWakeWord) or `assets/vswakeword/` (for vsWakeWord)
4. Copy your custom model files into the directory:
   - microWakeWord: `my_word.tflite` + `my_word.json`
   - vsWakeWord: `my_word.ort` + `my_word.json`
5. Save and repack the APK (MT Manager handles re-signing automatically)
6. Uninstall the old Ava, install the modified APK
7. Open Settings -> Voice Config -> Wake Word, your custom model appears in the list

### Steps (desktop apktool method)

```bash
# Decompile
apktool d Ava-0.5.4.apk -o ava_decoded

# Add your model
cp my_word.tflite my_word.json ava_decoded/assets/wakeWords/

# For vsWakeWord
cp my_word.ort my_word.json ava_decoded/assets/vswakeword/

# Repack
apktool b ava_decoded -o Ava-custom.apk

# Sign
zipalign -v 4 Ava-custom.apk Ava-custom-aligned.apk
apksigner sign --ks ava-key.jks --ks-pass pass:1234 --out Ava-custom-signed.apk Ava-custom-aligned.apk
```

---

## microWakeWord: Download Pretrained or Train Your Own

Ava uses **microWakeWords V3** format. The JSON config is identical to the V3 models from the community.

### Option 1: Download Pretrained Models (Easiest)

The [microWakeWords](https://github.com/TaterTotterson/microWakeWords) repo by TaterTotterson maintains a large library of pretrained V3 models. The format is directly compatible with Ava.

1. Browse the [microWakeWordsV3 directory](https://github.com/TaterTotterson/microWakeWords/tree/main/microWakeWordsV3)
2. Find a wake word you like (e.g., `aleesa`, `angel`, `annika`, `arale`, `artamis`, etc.)
3. Download both files: `name.json` and `name.tflite`
4. Import via Wake Word Library (Method 1 above) or place in `assets/wakeWords/` (Method 2)

The V3 JSON format is identical to Ava's built-in models:

```json
{
  "type": "micro",
  "wake_word": "ah_lehks_sah",
  "author": "Tater Totterson",
  "website": "https://github.com/TaterTotterson/microWakeWord-Trainer-AppleSilicon",
  "model": "ah_lehks_sah.tflite",
  "trained_languages": ["en"],
  "version": 2,
  "micro": {
    "probability_cutoff": 0.1,
    "sliding_window_size": 3,
    "feature_step_size": 10,
    "tensor_arena_size": 30000,
    "minimum_esphome_version": "2024.7.0"
  }
}
```

### Option 2: Train Your Own with macOS Trainer (Apple Silicon)

If you have a Mac with Apple Silicon (M1/M2/M3/M4), you can train a custom wake word with a local web UI:

1. **Install the trainer:**
   - Download the signed macOS app from [WakeWord Trainer releases](https://github.com/TaterTotterson/microWakeWord-Trainer-AppleSilicon/releases/latest)
   - Or clone and run from source:
     ```bash
     git clone https://github.com/TaterTotterson/microWakeWord-Trainer-AppleSilicon.git
     cd microWakeWord-Trainer-AppleSilicon
     ./run.sh
     ```
   - Open `http://127.0.0.1:8789` in your browser

2. **Train the wake word:**
   - Enter your wake phrase in the Trainer tab
   - Choose language (en, or other Piper-supported languages)
   - Optionally test pronunciation with Test TTS
   - Click **Start training**
   - The trainer uses Piper TTS to generate samples automatically
   - Personal samples are optional but improve accuracy

3. **Optionally capture real samples from devices:**
   - Flash a device with Tater firmware (from the Firmware tab)
   - Enable `Capture Wake Audio` on the device
   - Set `Trainer App URL` to `http://<trainer-ip>:8789`
   - Review captured clips in the Captured Audio tab
   - Mark good clips as "This is good", bad ones as "False wake"

4. **Get the output files:**
   - Successful training produces:
     - `trained_wake_words/<wake_word>.tflite`
     - `trained_wake_words/<wake_word>.json`

5. **Install into Ava:**
   - Import via Wake Word Library (Method 1 above)
   - Or copy both files to `assets/wakeWords/` and rebuild the APK (Method 2)

### Option 3: Train with Direct Script

```bash
git clone https://github.com/TaterTotterson/microWakeWord-Trainer-AppleSilicon.git
cd microWakeWord-Trainer-AppleSilicon
./train_microwakeword_macos.sh "hey_my_custom_word"
```

If `personal_samples/*.wav` or `negative_samples/*.wav` exist in the folder, they are included automatically.

### JSON Config Parameters

| Parameter | Description | Typical Value |
|-----------|-------------|---------------|
| `probability_cutoff` | Detection threshold (lower = easier to trigger, more false positives) | 0.1 - 0.97 |
| `sliding_window_size` | Frames to average before triggering | 3 - 9 |
| `feature_step_size` | Feature extraction step in ms | 10 |
| `tensor_arena_size` | TFLite arena size in bytes (must match model) | 21000 - 30000 |
| `minimum_esphome_version` | Minimum ESPHome version | 2024.7.0 |

> **Note:** microWakeWord requires a trained model for each wake word. There is no hot-swap — you need a `.tflite` file. But the V3 community library has 100+ pretrained models you can download directly.

---

## vsWakeWord: Manifest Hot-Swap

vsWakeWord supports manifest JSON hot-swap. To create a custom wake word:

1. **Train a model** using the vsWakeWord training pipeline (PyTorch → ONNX export)
2. **Create a manifest JSON** with your phoneme targets:
   ```json
   {
     "name": "my_custom_word",
     "format": "vs-wake-word-ctc-v1",
     "recommended_threshold": 0.6,
     "input": { "name": "input", "shape": [1, 128, 40], "dtype": "float32", "feature": "log_mel" },
     "output": { "name": "log_probs", "shape": [1, 49, 52], "dtype": "float32" },
     "feature_config": {
       "sample_rate": 16000, "window_ms": 1300, "frame_ms": 25, "hop_ms": 10,
       "n_fft": 512, "n_mels": 40, "f_min": 80.0, "f_max": 7600.0, "log_floor": 1e-06
     },
     "ctc": {
       "vocab_size": 52, "blank_id": 1, "pad_id": 0, "word_sep_id": 2,
       "wake_word_targets": [[your_phoneme_ids]],
       "wake_word_target_phonemes": [["your", "phonemes"]],
       "max_edit_distance": 1
     },
     "runtime": {
       "required_hits": 2, "hit_mode": "consecutive",
       "cooldown_ms": 2000, "high_confidence_bypass": 6.8
     },
     "stop_classifier": false
   }
   ```
3. **Import via Wake Word Library** (Method 1 above) or place both files in `assets/vswakeword/` (Method 2)

The 52-phoneme inventory uses IPA-style symbols. See an existing manifest (e.g., `hey_jarvis.json`) for the full inventory list.

> **Advantage:** vsWakeWord's manifest-based approach lets you swap wake word targets without retraining the base model in some cases — just update the `wake_word_targets` phoneme IDs. However, for best accuracy, a model trained on your specific wake word is recommended.

---

> **Compatibility:** Both apps share the same microWakeWord model format (V2/V3 `.tflite` + `.json`). Models from [TaterTotterson/microWakeWords](https://github.com/TaterTotterson/microWakeWords) V3 directory work directly.

---

*Back to [Voice Control](Voice-Control)*
