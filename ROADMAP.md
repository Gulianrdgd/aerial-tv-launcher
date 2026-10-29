# Roadmap

Ideas for future versions, roughly in order of priority. Nothing here is promised.

Every feature on this list is a setting. Each one can be switched on or off, and the defaults keep the home screen as it is today. Where a feature has choices, they're listed with it.

## Everyday use

- [ ] **Day and night aerials.** Tag each clip as day or night, the same way clips are tagged by scenery, and play day clips during the day and night clips after sunset. Choices: follow the sun, day only, night only, any time.
- [ ] **System screensaver.** Provide an Android screensaver (DreamService) so the TV's own idle timer shows the aerials over any app, not only on the home screen. Choices: what it shows (clock, weather, clip name, Now Playing).
- [ ] **Inputs in the dock.** Pin an input such as "HDMI 2 · PlayStation" to the dock and switch to it with one press. Choices: which inputs, and where they sit in the dock.
- [ ] **Favourite or skip a clip.** Hold OK during the screensaver to like a clip or never see it again. Choices: play favourites only, or favourites more often; a list in Settings to undo skips.
- [ ] **Search and Assistant button.** A button in the top bar that opens voice search, for remotes without a dedicated button.

## Setup

- [ ] **Easier notification access.** Settings explains why Now Playing and notifications need access, shows the adb command and a QR code to the instructions.
- [ ] **Keep the remote's buttons working.** An optional accessibility service that handles Home and Input on TVs where they misbehave once the stock launcher is disabled. Choices: which buttons it handles.

## The aerials

- [ ] **4K and HDR clips.** Play Apple's 4K HDR versions on TVs that support them. Choices: 1080p, 4K, 4K HDR, or automatic.
- [ ] **"Where is this?"** A button during the screensaver shows where the clip was filmed on a small map, with a sentence about the place. Choices: on press only, or shown with every clip.
- [ ] **Your own photos and videos.** Play from a USB stick or a network folder as well as, or instead of, the aerials. Choices: source folder, mix with aerials or replace them, time per photo.
- [ ] **Burn-in protection.** Move the screensaver's clock and title slightly every few minutes. Choices: off, on, or automatic on OLED panels.

## The home screen

- [ ] **Weather details.** Select the weather tile to see the forecast for the coming hours and days. Choices: hourly, daily, or both.
- [ ] **Night mode.** Dim the panels and lower the video brightness in the evening. Choices: start and end times or follow sunset, how much to dim.
- [ ] **Recently used.** Order apps by how often they're opened, or add a row with the last few apps. Choices: manual order, most used, or a separate row.
- [ ] **Sleep timer and screen off.** Quick settings to turn the TV off after 30, 60 or 90 minutes, and to turn the screen off while audio keeps playing. Choices: which timer lengths appear.
- [ ] **Status icons.** Small icons in the top bar for Wi-Fi strength, Bluetooth devices and low storage. Choices: each icon separately.

## Settings and upkeep

- [ ] **Backup and restore.** Save the dock, order, hidden apps and settings to a file to move them to another TV or keep them through a reset.
- [ ] **Update notice.** For sideloaded builds, show a notice in Settings when a new release is out on GitHub. F-Droid builds leave it out.
- [ ] **Larger text.** A setting for bigger text and stronger contrast, for people who sit far from the TV. Choices: text size steps, contrast on or off.

## More ambitious

- [ ] **Home Assistant card.** Show the doorbell camera when someone rings, or a few light and scene buttons. Choices: server address, which entities, when the card appears.
- [ ] **Kids profile.** A separate dock with only chosen apps, locked with a PIN. Choices: allowed apps, PIN, whether the screensaver and settings are reachable.

## Quality and distribution

- [ ] **Speed on slow TVs.** A baseline profile, plus startup and scrolling measurements on a 1 GB device such as a Chromecast with Google TV.
- [ ] **Automatic builds.** GitHub Actions builds and tests every push and attaches the APK to a release when a tag is pushed.
- [ ] **F-Droid listing.** Open the merge request on fdroiddata and retake the Inputs and home screenshots on a real TV.
- [ ] **Translations.** Start with Dutch.
