# Contributing

Bug reports, ideas and pull requests are welcome. See [ROADMAP.md](ROADMAP.md) for what's planned.

## Translations

Every language is one file in [`app/src/main/assets/translations/`](app/src/main/assets/translations), named after the language code: `nl.json` for Dutch, `de.json` for German, `fr.json` for French. Aerial picks the file that matches the TV's language; texts without a translation stay in English.

### Adding a language

1. Copy `nl.json` to `<your language code>.json`.
2. Replace each Dutch value with your translation. Leave the English keys on the left exactly as they are.
3. Open a pull request. You don't need Android Studio or any Kotlin.

```json
{
  "Settings": "Einstellungen",
  "{0} seconds": "{0} Sekunden",
  "Rename {0}": "{0} umbenennen"
}
```

Things to keep in mind:

- **Placeholders.** `{0}`, `{1}` stand for a number or a name. Keep every placeholder from the key in your translation; you can move them to wherever your language needs them. A test checks this, so a missing one shows up in the pull request.
- **Length.** Tiles and rows are narrow on a TV. Short words beat precise ones.
- **Leave out what you're unsure of.** A missing line falls back to English; a wrong one is worse.
- **Testing on a device.** On Android 13 and newer you can switch only Aerial to your language: `adb shell cmd locale set-app-locales nl.julian.aerial --locales de-DE`, then restart Aerial.

### When you add a text to the app

Write it in English in the code, as usual: Aerial's `Text` translates it as it's drawn. Then add it to `nl.json`, so translators find it there. Texts with a number or a name in them become keys with placeholders: `"$n seconds"` in the code is `"{0} seconds"` in the file.

## Code

Run `./gradlew :app:assembleDebug :app:testDebugUnitTest` before opening a pull request; GitHub Actions runs the same on every push. Keep changes focused, match the style of the code around them, and make anything new a setting that's off by default (see the rules at the top of the roadmap).
