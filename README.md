# Adaptive Soundscape

Adaptive Soundscape is an offline Android app for listening to evolving environments while working, relaxing, or falling asleep. Choose Rainy Room, Night Train, or Forest Cabin, then adjust the layers or save your mix.

## Run and verify

Use JDK 17 or 21 and an Android SDK with API 35. The app supports Android 8 and later. Set `ANDROID_HOME` to the SDK directory or use Android Studio to open this checkout.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew installDebug
./gradlew connectedDebugAndroidTest
```

Install and connected tests require a phone or emulator. The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## How it works

- Each scene has three continuous layers with different loop lengths and two independently scheduled intermittent sounds. The WAV generator overlaps each loop's end and start so the wrap does not fade to silence.
- A Media3 playback service owns playback, timing, focus handling, and media controls. The mixer applies per-layer gain and master gain to both continuous and active intermittent sounds.
- Focus, Relax, and Sleep presets run for 25, 45, and 60 minutes respectively. Each timer fades playback out over the last 30 seconds.
- DataStore saves the last mix and up to 20 named favourites. Compose draws the three scene animations and updates them with the selected mix.
- Playback generates all sounds locally. See the [asset manifest](docs/AUDIO_ASSETS.md) and [release checklist](docs/RELEASE_CHECKLIST.md).

The launch sounds are procedural. Release-quality environmental recordings and real-device listening are separate milestones.
