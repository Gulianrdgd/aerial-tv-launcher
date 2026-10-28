# Aerial

Aerial is a home screen for Android TV and Google TV. Slow aerial video plays behind everything, the way it does on an Apple TV, and the panels on top follow Material You: their colours are picked from whatever clip is on screen.

![Home screen](docs/screenshots/home.png)

## What's on the home screen

The bar at the top holds the clock, the aerial that's playing (select it to skip to the next one), the weather and three buttons for inputs, notifications and settings. When music is playing, the aerial tile turns into a Now Playing card with album art and play controls, and the clock and weather shrink to make room for it.

Below the bar there's a gap where you just see the aerial. Then come your six dock apps with their own banners, a Continue Watching row filled by your streaming apps, a row of quick settings, and the rest of your apps.

Hold OK on an app to open, move, pin, hide or uninstall it, or to open its app info. Pressing Home while you're already home jumps back to the top.

After two minutes without input the panels fade out and the aerial plays full screen, much like Chromecast's ambient mode: the clip's name, the time and the weather sit in the bottom left, and whatever is playing shows in the top right. Any button brings the home screen back.

![Screensaver](docs/screenshots/screensaver.png)

## Aerials

There are 265 clips at 1080p, from Apple's TV screensaver, from Fire TV and from community collections. Each one is tagged as city, nature, underwater or space, so you can pick the scenery in Settings or in the quick settings row.

Clips stream when they're shown and nothing is downloaded ahead. The last one or two clips stay in a 300 MB cache, so the screen comes up straight away after a restart and still has something to play when you're offline.

## Settings

![Settings](docs/screenshots/settings.png)

The settings panel opens from the gear in the top bar. It covers scenery and collection, how soon the screensaver starts, 12 or 24 hour time, Celsius or Fahrenheit, the weather location, tile style (app banners or tiles tinted to the aerial), and hidden apps. It also links to the Android TV settings for date, time and timezone.

Weather comes from Open-Meteo, which needs no account. By default the city is taken from the TV's timezone; you can type a different one.

## Install

Build a release APK and install it over adb:

```sh
./gradlew :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

To make it the home screen, open Settings and choose Default home app. On some TVs, Philips among them, the stock launcher has a higher priority and keeps taking over the Home button. In that case, switch the stock launcher off for your user:

```sh
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

`adb shell pm enable com.google.android.tvlauncher` brings it back.

## Permissions

Continue Watching asks for access the first time you select it, with a normal on-screen dialog.

Now Playing and notifications need notification access. Android TV has no screen to grant that, so do it once over adb:

```sh
adb shell cmd notification allow_listener nl.julian.aerial/nl.julian.aerial.media.LauncherListenerService
```

## Development

The app is written in Kotlin with Jetpack Compose and `androidx.tv:tv-material`. Video plays through Media3 ExoPlayer, settings are stored with DataStore, and the colour schemes come from Material Color Utilities.

Run the unit tests with `./gradlew :app:testDebugUnitTest`.

Debug builds have test hooks for the emulator. They all go through one receiver:

```sh
adb shell am broadcast -n nl.julian.aerial/.debug.FakeMediaReceiver -a <action>
```

`play`, `pause` and `stop` control a fake track for Now Playing. `input-on` and `input-off` add or remove a fake TV input. Test notifications can be posted with `adb shell cmd notification post -t 'Title' tag 'Text'`.

## Licence

GPL-3.0. The aerial list and the approach to video playback come from [couchy-launcher](https://github.com/conreo/couchy-launcher). See [NOTICE](NOTICE) for that and for the bundled font and icons.
