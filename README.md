<p align="center"><img src="art/logo.svg" width="140" alt="Aerial logo: a sunrise over Earth seen from orbit"></p>

# Aerial: an Android TV launcher with Apple TV aerials

[![Latest release](https://img.shields.io/github/v/release/Gulianrdgd/aerial-tv-launcher?label=release)](https://github.com/Gulianrdgd/aerial-tv-launcher/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Gulianrdgd/aerial-tv-launcher/total)](https://github.com/Gulianrdgd/aerial-tv-launcher/releases)
[![License: GPL-3.0](https://img.shields.io/github/license/Gulianrdgd/aerial-tv-launcher)](LICENSE)
![Android TV](https://img.shields.io/badge/Android%20TV-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)

Aerial is a custom launcher (home screen replacement) for Android TV and Google TV. Slow aerial video plays behind everything, the way it does on an Apple TV, and the panels on top follow Material You: their colours are picked from whatever clip is on screen. It has no ads and no sponsored rows.

Aerial is free forever. I use it on my own TV, and I'm not going to pay myself for it.

[Download the latest APK](https://github.com/Gulianrdgd/aerial-tv-launcher/releases/latest) and sideload it with adb (see [Install](#install)).

![Aerial Android TV launcher home screen with an aerial video background](docs/screenshots/home.png)

## What's on the home screen

At the top, a large, bold clock sits straight on the aerial, with the date and the weather's two-hour rain forecast under it, and the buttons for inputs, notifications and settings float in a small pill on the right. The original top bar, a panel of tiles, is still there as Classic in Settings › Top bar. In Settings you can hide the weather, add the name of the aerial that's playing or a Now Playing card with album art and play controls, and shrink the three buttons to a narrow column of icons.

Below the bar there's a gap where you just see the aerial. Then come your six dock apps with their own banners, a Continue Watching row filled by your streaming apps, the rest of your apps, and a row of quick settings.

Hold OK on an app to open, move, pin, hide or uninstall it, or to open its app info. Pressing Home while you're already home jumps back to the top.

After two minutes without input the panels fade out and the aerial plays full screen, much like Chromecast's ambient mode: the clip's name and scenery, the time and the weather sit in the bottom left, and whatever is playing shows in the top right. Any button brings the home screen back.

![Aerial screensaver with clip name, time and weather](docs/screenshots/screensaver.png)

## Inputs

The inputs button in the top bar lists the TV's HDMI ports first, in port order, then the other inputs. Each one shows the name you gave it on the TV, with an icon for the kind of device that's plugged in.

![Input switcher listing HDMI ports](docs/screenshots/inputs.png)

## Aerials

There are 265 clips at 1080p, from Apple's TV screensaver, from Fire TV and from community collections. Each one is tagged as city, nature, underwater or space, so you can pick the scenery in Settings or in the quick settings row.

Clips stream when they're shown and nothing is downloaded ahead. The last one or two clips stay in a 300 MB cache, so the screen comes up straight away after a restart and still has something to play when you're offline.

## Settings

![Aerial settings: categories on the left, the Looks settings on the right](docs/screenshots/settings.png)

Settings open from the gear in the top bar. The categories are on the left and their settings on the right, with the path at the top, so you always know where you are. A choice lists all its options at once, and on/off settings have a switch.

![Aerial's setup on first start, at the Aerials step](docs/screenshots/setup.png)

On first start a short setup asks about the Home button, the aerials, the clock and weather, and access. You can run it again from Settings › System. Everything Aerial added since 0.2 is off until you turn it on; [ROADMAP.md](ROADMAP.md) lists what's there and what's planned.

Weather comes from Open-Meteo, which needs no account. Aerial guesses your city from the TV's timezone, and you can type a different one.

## Install

Build a release APK and install it over adb:

```sh
./gradlew :app:assembleRelease -PlocalSign
adb install -r app/build/outputs/apk/release/app-release.apk
```

`-PlocalSign` signs the build with your local debug key. Without it the release build is unsigned, which is what F-Droid expects.

To make it the home screen, open Settings and choose Default home app. On some TVs, Philips among them, the stock launcher has a higher priority and keeps taking over the Home button. In that case, switch the stock launcher off for your user:

```sh
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

`adb shell pm enable com.google.android.tvlauncher` brings it back.

## Permissions

Continue Watching asks for access the first time you select it, through the usual Android dialog.

Now Playing and notifications need notification access. Android TV has no screen to grant that, so do it once over adb:

```sh
adb shell cmd notification allow_listener nl.julian.aerial/nl.julian.aerial.media.LauncherListenerService
```

## Development

The app is written in Kotlin with Jetpack Compose and `androidx.tv:tv-material`. Video plays through Media3 ExoPlayer, settings are stored with DataStore, and the colour schemes come from Material Color Utilities.

Run the unit tests with `./gradlew :app:testDebugUnitTest`.

GitHub Actions builds and tests every push. When a `v*` tag is pushed it also signs a release APK and attaches it to the release, but only if the repository has these secrets: `KEYSTORE_BASE64` (the keystore, base64-encoded), `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. Use the same key as earlier releases, or the new APK won't install over the old one.

Debug builds have test hooks for the emulator. They all go through one receiver:

```sh
adb shell am broadcast -n nl.julian.aerial/.debug.FakeMediaReceiver -a <action>
```

`play`, `pause` and `stop` control a fake track for Now Playing. `input-on` and `input-off` add or remove a fake TV input. Post a test notification with `adb shell cmd notification post -t 'Title' tag 'Text'`.

## Translations

Aerial is in English, Dutch, French, German, Italian, Portuguese and Spanish. Each language is one JSON file of English texts and their translations, so adding yours needs no code: see [CONTRIBUTING.md](CONTRIBUTING.md).

## Licence

GPL-3.0. The aerial list and the approach to video playback come from [couchy-launcher](https://github.com/conreo/couchy-launcher). See [NOTICE](NOTICE) for that and for the bundled font and icons.
