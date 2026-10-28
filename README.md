# Aerial

An Android TV / Google TV launcher. It has a Monet-style home screen of frosted glass panels over Apple TV-style aerial video, and the UI colours follow whatever is playing.

- **Aerials:** 265 streamed 1080p clips from Apple, Fire TV and the community. They're cached on disk (1 GB), and the next clip is fetched in the background.
- **Dynamic colour:** the playing frame is sampled every few seconds and the whole UI re-tints to match.
- **Widgets:** clock, network and Bluetooth status, weather (Open-Meteo, located from the TV's timezone) and Now Playing with transport controls.
- **Continue Watching:** the system Watch Next list that streaming apps publish to.
- **Dock and app grid:** long-press OK on any app to open, move, pin, hide, view info or uninstall.
- **Screensaver:** after a few minutes without input the UI fades away and the aerial plays full screen.

## Build and install

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then press Home on the TV and choose **Aerial**, or open the settings panel (gear) → *Default home app*.

## One-time permissions

**Continue Watching** asks for access the first time you select it: an on-screen dialog, no adb needed.

**Now Playing** reads media sessions through a notification listener. Android TV has no settings screen for this, so grant it once over adb:

```sh
adb shell cmd notification allow_listener nl.julian.aerial/nl.julian.aerial.media.MediaListenerService
```

## Development

- Kotlin, Jetpack Compose with `androidx.tv:tv-material`, Media3 ExoPlayer, DataStore, Coil, Material Color Utilities.
- `./gradlew :app:testDebugUnitTest` runs the unit tests.
- Debug builds include a fake media session for testing Now Playing without a music app:
  `adb shell am broadcast -n nl.julian.aerial/.debug.FakeMediaReceiver -a play` (or `pause`, `stop`).

## Licence

GPL-3.0. The aerial manifest and the video-wallpaper approach are adapted from
[couchy-launcher](https://github.com/conreo/couchy-launcher). See [NOTICE](NOTICE).
