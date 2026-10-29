# Roadmap

Ideas for future versions, roughly in order of priority. Nothing here is promised.

Every feature on this list is a setting. Each one can be switched on or off, and the defaults keep the home screen as it is today. Where a feature has choices, they're listed with it.

Two rules hold for all of it:

- **Free, with nothing locked.** No paid tier, no licence check and no dependency on the Play Store, so features never disappear after a sideload or an update.
- **Simple first.** The main settings stay short. Everything else goes under "More options", with a few ready-made looks to start from.

## From users of other launchers

- [ ] **Notice when Aerial isn't Home any more.** Firmware updates and reboots often hand Home back to the stock launcher. Aerial detects it and shows how to fix it, with the adb command for TVs that need it.
- [ ] **Explain an empty Continue Watching row.** Say that apps have to support the row, and list which installed apps do. Choices: which apps may appear in the row.
- [ ] **Start with an app or input.** Open a chosen app or HDMI input when the TV turns on. Choices: home, an app, an input.
- [ ] **Time limits.** Limit inputs and apps to certain hours, as part of the kids profile. Choices: per app or input, hours, PIN to override.
- [ ] **Plex and Jellyfin row.** Show recent and unwatched items from your own media server. Choices: server, libraries, row position.
- [ ] **Background add-ons.** Other sources for the background, such as Unsplash, a folder of photos or a webcam stream. Choices: source, how often it changes.
- [ ] **Tile size.** Fewer, larger tiles per row (text size is done, under Larger text).
- [ ] **Hidden from recent apps.** Keep Aerial out of the recent apps screen. Choices: on or off.

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
- [ ] **Network folders.** The same from a network share (SMB) or a NAS. Choices: server, folder.
- [x] **Burn-in protection.** Move the screensaver's clock and title slightly every few minutes. Choices: on or off (Android can't tell whether a panel is OLED).

## The home screen

- [x] **Weather details.** Select the weather tile to see the forecast for the coming hours and days. Choices: hourly, daily, or both.
- [x] **Night mode.** Dim the panels and the video in the evening. Choices: after sunset or 22:00 to 07:00, how much to dim (20, 40 or 60%).
- [x] **Recently used.** Order All apps by how often they're opened, or add a Recent row. Choices: your order or most used first; Recent row on or off.
- [x] **Sleep timer and picture off.** Quick settings to put the TV to standby after 30, 60 or 90 minutes (through the Remote buttons accessibility service, the only way an app can), and to black out the picture while sound keeps playing.
- [x] **Status icons.** Small icons next to the date for the network (Wi-Fi, Ethernet or offline), Bluetooth, and a warning when storage is almost full. Choices: each icon separately. Wi-Fi signal strength is still to do.

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
- [ ] **Focus sounds.** Quiet sounds when moving focus or opening an app. Choices: off, soft, volume.

## Customization

- [ ] **Theme settings.** Colour source (from the aerial, one fixed colour, black and white), panel opacity, corner roundness, tile size and font weight.
- [ ] **Layout editor.** Reorder or hide the home screen sections, and choose which tiles go in the top bar and in what order.
- [ ] **App names and banners.** Rename an app, pick another banner or icon, or use an icon pack.
- [ ] **Folders.** Group apps into a folder tile, such as "Streaming" or "Kids". Choices: name, colour, which apps.
- [ ] **Quick settings of your own.** Choose which shortcuts appear in the quick settings row and in what order.
- [ ] **Remote shortcuts.** Choose what a long press on Home or another button does: open an app, switch inputs, start the screensaver.
- [ ] **Time-of-day layouts.** Different layouts by time, such as a large weather tile in the morning and Continue Watching first in the evening. Choices: times and the layout for each.
- [ ] **Share a theme.** Export the look as a file or QR code and import someone else's.

## More ambitious

- [ ] **Home Assistant card.** Show the doorbell camera when someone rings, or a few light and scene buttons. Choices: server address, which entities, when the card appears.
- [ ] **Kids profile.** A separate dock with only chosen apps, locked with a PIN. Choices: allowed apps, PIN, whether the screensaver and settings are reachable.

## Quality and distribution

- [ ] **Speed on slow TVs.** A baseline profile, plus startup and scrolling measurements on a 1 GB device such as a Chromecast with Google TV.
- [ ] **Automatic builds.** GitHub Actions builds and tests every push and attaches the APK to a release when a tag is pushed.
- [ ] **F-Droid listing.** Open the merge request on fdroiddata and retake the Inputs and home screenshots on a real TV.
- [ ] **Translations.** Start with Dutch.
