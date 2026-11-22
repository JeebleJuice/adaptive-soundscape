# Android release checks

- [x] Run local JVM tests, Android lint, and debug and unsigned release builds.
- [ ] Run the connected Compose and media-controller tests against a booted emulator or device.
- [ ] Start Rainy Room in airplane mode from a fresh install. Listen for one hour and mark any audible loop seams, repeating events, or interruptions.
- [ ] Exercise all scenes on phone speakers, wired headphones, and Bluetooth. Check balance and sudden peaks at maximum mixer levels.
- [ ] Lock the screen during playback. Verify media notification metadata, pause/resume, Bluetooth controls, and continued playback while switching apps.
- [ ] Disconnect headphones and interrupt playback with a call, another media app, and transient audio focus.
- [ ] Confirm a timer expires while locked, fades all active layers, and pauses playback. Check cancel and restart during a fade.
- [ ] Recreate the process and confirm the last mix and named favourites load correctly.
- [ ] Measure CPU, memory, and battery during a one-hour locked session. Verify scene animations stop when the activity is stopped.
- [ ] Review large text, TalkBack, and touch targets on a compact screen.
- [ ] Review audio provenance, prepare store screenshots and a short preview, and produce a signed release using the owner's release key.

## Privacy

Adaptive Soundscape works offline. It has no account, ads, analytics, or Internet permission. Mix preferences and generated audio are stored on the device. Clearing app storage deletes them.
