# Ava Biscuit

Headless Home Assistant voice appliance for the Amazon Echo Dot Biscuit running CM12.1 / Android 5.1.

This fork turns Ava into a minimal ESPHome-native Assist satellite for Biscuit hardware. It is meant to run as a background service with no launcher UI.

## What it does

- Exposes a Home Assistant Assist satellite over the ESPHome native API.
- Uses the Biscuit microphone and speaker for Assist audio.
- Supports local wake words from bundled Micro Wake Word assets.
- Exposes retained Home Assistant entities:
  - Assist satellite
  - media player
  - microphone mute switch
  - microphone volume number
  - wake sound switch
  - sound selection dropdowns (Configuration entities)
  - continuous conversation switch (Configuration entity)
  - action button independent-mode switch (Configuration entity)
  - action button pressed binary sensor
  - ambient light sensor
  - Start/Stop Assist button
  - Assist status diagnostic text sensor
- Maps the physical Biscuit action button to Start/Stop Assist, or to `action_button_pressed` when independent mode is enabled.
- Supports continuous conversation when Home Assistant asks to continue.
- Drives the Biscuit LED ring from real Assist states.
- Keeps playback on ExoPlayer/Media3; FLAC support depends on the ROM exposing a working FLAC MediaCodec.

## Install

Install the APK with ADB. This applies to both locally built APKs and APKs downloaded from GitHub Actions.

```sh
adb install -r path/to/Ava.apk
```

To build the debug APK locally first:

```sh
cd android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/Ava-*-debug.apk
```

## First start

A fresh install does not start by itself. Start the service once with ADB:

```sh
adb shell am startservice -n net.mfuertes.biscuit.ava/com.example.ava.services.VoiceSatelliteService
```

After the first manual start, Ava starts again on boot and after app updates.

Then add the discovered ESPHome device in Home Assistant.

## Wake-word file staging

Bundled wake words ship inside the APK. Ava also reads extra Micro Wake Word files from `/sdcard/wakeWords`; this does not require `adb root`:

```sh
adb shell mkdir -p /sdcard/wakeWords
adb push my_wake_word.json /sdcard/wakeWords/
adb push my_wake_word.tflite /sdcard/wakeWords/
adb shell ls -l /sdcard/wakeWords
adb shell am broadcast -a net.mfuertes.biscuit.ava.ACTION_STOP_SERVICE
adb shell am startservice -n net.mfuertes.biscuit.ava/com.example.ava.services.VoiceSatelliteService
```

Keep matching `.json` and `.tflite` files together in that flat directory. Restart Ava after pushing files so Home Assistant sees the new wake words, shown with `(external)` in the dropdown.

## Sound file staging

Bundled WAV sounds ship inside the APK. Ava exposes Home Assistant Configuration dropdowns for wake, stop, timer, and continuous-prompt sounds. The dropdowns include bundled WAVs plus extra flat `.wav` files from `/sdcard/sounds`:

```sh
adb shell mkdir -p /sdcard/sounds
adb push wake_word_triggered.wav /sdcard/sounds/
adb push stop_sound.wav /sdcard/sounds/
adb push timer_finished.wav /sdcard/sounds/
adb push continuous_prompt.wav /sdcard/sounds/
adb shell am broadcast -a net.mfuertes.biscuit.ava.ACTION_STOP_SERVICE
adb shell am startservice -n net.mfuertes.biscuit.ava/com.example.ava.services.VoiceSatelliteService
```

Restart Ava after adding files so Home Assistant sees the new options. External sounds are shown with `(external)`. Only flat `.wav` filenames are used; other formats and nested paths are ignored.

## Build notes

Known-good local environment:

```sh
export ANDROID_HOME=/home/hkfuertes/Android/Sdk
export ANDROID_SDK_ROOT=/home/hkfuertes/Android/Sdk
export JAVA_HOME=/home/hkfuertes/.local/share/mise/installs/java/17.0.2
cd android
./gradlew :app:testDebugUnitTest :app:compileDebugKotlin :app:assembleDebug
```

## GitHub Actions

The `Build APK` workflow is manual-only (`workflow_dispatch`). It builds a release APK signed with the Android debug certificate and uploads it as the `ava-release-debug-signed-apk` artifact.

## Lineage

This fork is derived from [knoop7/Ava](https://github.com/knoop7/Ava), which itself is based on the original [brownard/Ava](https://github.com/brownard/Ava).

Thanks to [View Assist Companion App](https://github.com/msp1974/ViewAssistCompanionApp) for useful reference implementations around Assist satellite behavior, custom files, and continuous conversation.

Powered by [ESPHome](https://esphome.io/).
