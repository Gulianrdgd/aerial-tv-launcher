# Roadmap

Ideas for future versions, roughly in order of priority. Nothing here is promised.

Every feature on this list is a setting. Each one can be switched on or off, and the defaults keep the home screen as it is today. Where a feature has choices, they're listed with it.

Two rules hold for all of it:

- **Free, with nothing locked.** No paid tier, no licence check and no dependency on the Play Store, so features never disappear after a sideload or an update.
- **Simple first.** The main settings stay short. Everything else goes under "More options", with a few ready-made looks to start from.

## From users of other launchers

- [x] **Notice when Aerial isn't Home any more.** Firmware updates and reboots often hand Home back to the stock launcher. Aerial detects it and shows how to fix it, with the adb command for TVs that need it.
- [x] **Explain an empty Continue Watching row.** An empty row says why. Hold OK on a show to leave its app out; Settings brings them back.
- [x] **Start with an app or input.** Open a chosen app or HDMI input when the TV turns on. Choices: home, an app, an input.
- [x] **Time limits.** Part of kids mode: apps open only during the allowed hours.
- [x] **Jellyfin row.** Sign in to your Jellyfin server (More options > System) for a row with Continue watching and Next up. A card opens the Jellyfin app.
- [x] **Plex row.** Sign in with a code at plex.tv/link (More options > System) for a row with On Deck. A card opens the Plex app.
- [x] **Background links.** Add the address of a video, a live stream (HLS, such as a webcam) or a photo, played with your own photos and videos.
- [ ] **Unsplash backgrounds.** Photos from Unsplash (needs an API key).
- [x] **Tile size.** Normal (6 per row), medium (5) or large (4).
- [x] **Hidden from recent apps.** Keep Aerial out of the recent apps screen. Choices: on or off.

## Everyday use

- [x] **Day and night aerials.** Tag each clip as day or night, the same way clips are tagged by scenery, and play day clips during the day and night clips after sunset. Choices: follow the sun, day only, night only, any time.
- [x] **System screensaver.** Provide an Android screensaver (DreamService) so the TV's own idle timer shows the aerials over any app, not only on the home screen. Choices: what it shows (clock, weather, clip name, Now Playing).
- [x] **Inputs in the dock.** Pin an input such as "HDMI 2 · PlayStation" to the dock and switch to it with one press. Choices: which inputs, and where they sit in the dock.
- [x] **Favourite or skip a clip.** Hold OK during the screensaver to like a clip or never see it again. Choices: play favourites only, or favourites more often; a list in Settings to undo skips.
- [x] **Search and Assistant button.** A button in the top bar that opens voice search, for remotes without a dedicated button.

## Setup

- [x] **Easier notification access.** Settings explains why Now Playing and notifications need access, shows the adb command and a QR code to the instructions.
- [x] **Keep the remote's buttons working.** An optional accessibility service that handles Home and Input on TVs where they misbehave once the stock launcher is disabled. Choices: which buttons it handles.

## The aerials

- [x] **4K and HDR clips.** Play Apple's 4K HDR versions on TVs that support them. Choices: 1080p, 4K, 4K HDR, or automatic.
- [x] **"Where is this?"** The screensaver names the place on screen, using Apple's own captions, which change as the clip moves (114 clips). Apple publishes no coordinates, so there's no map. Choices: on or off.
- [x] **Your own photos and videos.** Play from a USB stick or SD card as well as, or instead of, the aerials. Choices: mix with aerials or replace them, time per photo.
- [x] **Web folders.** A link ending in / plays the videos and photos in that folder's file listing (NAS web shares, nginx, Apache, python http.server).
- [ ] **SMB shares.** Windows and NAS file shares; the SMB libraries add several MB, so this waits until it's asked for.
- [x] **Burn-in protection.** Move the screensaver's clock and title slightly every few minutes. Choices: on or off (Android can't tell whether a panel is OLED).

## The home screen

- [x] **Weather details.** Select the weather tile to see the forecast for the coming hours and days. Choices: hourly, daily, or both.
- [x] **Night mode.** Dim the panels and the video in the evening. Choices: after sunset or 22:00 to 07:00, how much to dim (20, 40 or 60%).
- [x] **Recently used.** Order All apps by how often they're opened, or add a Recent row. Choices: your order or most used first; Recent row on or off.
- [x] **Sleep timer and picture off.** Quick settings to put the TV to standby after 30, 60 or 90 minutes (through the Remote buttons accessibility service, the only way an app can), and to black out the picture while sound keeps playing.
- [x] **Status icons.** Small icons next to the date for the network (Wi-Fi, Ethernet or offline), Bluetooth, and a warning when storage is almost full. Wi-Fi shows its signal in bars (Android 10 and newer). Choices: each icon separately.

## Settings and upkeep

- [x] **Backup and restore.** Save all settings to aerial-settings.json in Downloads, and restore from it (or from a picked file on TVs with a file picker). Android 9 and older can't save there yet.
- [x] **Update notice.** For sideloaded builds, show a notice in Settings when a new release is out on GitHub. F-Droid builds leave it out.
- [x] **Larger text.** Text size (100, 110 or 120%) and high contrast, which puts all text at full brightness.

## Looks

- [x] **Frosted glass.** Blur the small video snapshot already taken for the colours and draw it behind the panels, so they look like real frosted glass. Choices: tinted (today), frosted, how much blur.
- [x] **Opening animation.** When an app opens, its tile grows to fill the screen first. Choices: on or off, speed.
- [x] **Tile depth on focus.** Focused tiles tilt slightly toward the direction of movement and catch a soft highlight. Choices: off, subtle, strong.
- [x] **Glow in the app's colour.** A tile's focus glow takes its colour from the app's banner. Choices: app colour, aerial colour, white.
- [x] **Panels slide in.** On wake, the panels come in one after another. Choices: rise and fade (today), slide in from the side, appear at once.
- [x] **Clock faces.** Besides the colour pills: a large thin clock, an analog face and a word clock. Choices: face per place (top bar, screensaver).
- [x] **Seasonal touches.** Snow in December, a frame on New Year's Eve. Choices: each one separately.
- [x] **Focus sounds.** Quiet sounds when moving focus or opening an app. Choices: off, soft, volume.

## Customization

- [x] **Theme settings.** Colours from the aerial, one colour (six to pick from) or black and white; panel opacity.
- [x] **More theme settings.** Corners (square, round, extra round) and text weight (lighter, normal, bolder); both travel with a shared look.
- [x] **Layout editor.** Show, hide and reorder the home screen sections (Settings > Home layout).
- [x] **Top bar order.** Rearrange the clock, weather, middle tile and buttons (Settings > Home layout); each can still be shown or hidden in the Top bar settings.
- [x] **App names and banners.** Rename an app, or show its icon and name instead of its banner (hold OK on the app).
- [x] **Icon packs.** Use an installed icon pack (ADW/Nova format) on tinted and icon tiles.
- [x] **Folders.** Group apps into a folder tile, such as "Streaming" or "Kids". Choices: name, colour, which apps.
- [x] **Quick settings of your own.** Show, hide and reorder the quick settings tiles (Settings > Home layout).
- [x] **Remote shortcuts.** Hold Home, or press the red, green, yellow or blue button, to open an app, the input switcher or the screensaver (through the Remote buttons accessibility service).
- [x] **Time-of-day layouts.** Put a chosen section first in the morning (06:00 to 12:00) and in the evening (18:00 to midnight), in Settings > Home layout.
- [x] **Share a theme.** Save the look (colours, panels, animations, clock faces, text) as aerial-theme.json in Downloads, and apply someone else's. A theme is too long to type in from a QR code, so it travels as a file.

## More ambitious

- [x] **Home Assistant.** Sign in with a long-lived token (More options > System). Lights, switches, scenes and scripts as buttons in a Home row; a doorbell sensor shows its camera for 30 seconds when it rings.
- [x] **Kids profile.** Kids mode (More options): only the chosen apps, at chosen hours (any time, 07:00 to 19:00 or 07:00 to 20:00); the top bar buttons and the remote's Input button need the PIN.
- [x] **Daily time limit.** Kids mode: 30 to 120 minutes of apps a day, counted from opening an app until the home screen is back. Inputs already need the PIN in kids mode.

## Quality and distribution

- [x] **Baseline profile.** Startup and the home screen's D-pad paths are compiled ahead of time (generated with `./gradlew :app:generateReleaseBaselineProfile`).
- [ ] **Measure on a slow TV.** Startup and scrolling on a 1 GB device such as a Chromecast with Google TV.
- [x] **Automatic builds.** GitHub Actions builds and tests every push, and on a version tag signs the APK and attaches it to the release (once the keystore secrets are set).
- [ ] **F-Droid listing.** Open the merge request on fdroiddata and retake the Inputs and home screenshots on a real TV.
- [x] **Translations.** Dutch, from a JSON file per language that contributors can add without touching code (see CONTRIBUTING.md).
