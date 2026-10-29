package nl.julian.aerial.ui.menus

import nl.julian.aerial.data.KidsHours
import nl.julian.aerial.data.FocusSounds
import nl.julian.aerial.data.LaunchAnimation
import nl.julian.aerial.data.FocusColor
import nl.julian.aerial.data.TileDepth
import nl.julian.aerial.theme.Reveal
import nl.julian.aerial.data.Glass
import nl.julian.aerial.data.AppOrder
import nl.julian.aerial.data.TempUnit
import nl.julian.aerial.data.ClockFace
import nl.julian.aerial.data.ClockFormat
import nl.julian.aerial.data.FavouriteMode
import nl.julian.aerial.data.AmbientInfo
import nl.julian.aerial.data.VideoQuality
import nl.julian.aerial.data.AerialTime
import nl.julian.aerial.data.Scenery
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.key
import nl.julian.aerial.data.kidsUsedToday
import nl.julian.aerial.data.PlexRepository
import nl.julian.aerial.data.HomeAssistant
import nl.julian.aerial.data.HaConfig
import androidx.compose.runtime.mutableIntStateOf
import nl.julian.aerial.data.JellyfinRepository
import nl.julian.aerial.data.IconPacks
import nl.julian.aerial.data.withLook
import nl.julian.aerial.data.look
import nl.julian.aerial.data.encode
import nl.julian.aerial.data.decodeLook
import nl.julian.aerial.data.THEME_COLORS
import nl.julian.aerial.data.ThemeSource
import android.net.Uri
import androidx.compose.runtime.produceState
import nl.julian.aerial.data.UpdateCheck
import nl.julian.aerial.data.Backup
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.ui.Text
import kotlinx.coroutines.launch
import nl.julian.aerial.data.ConfigStore
import nl.julian.aerial.data.LauncherConfig
import nl.julian.aerial.data.NowPlayingRepository
import nl.julian.aerial.data.NightMode
import nl.julian.aerial.data.OwnMedia
import nl.julian.aerial.data.RemoteButtons
import nl.julian.aerial.data.WatchNextRepository
import nl.julian.aerial.data.WeatherDetails
import nl.julian.aerial.remote.RemoteButtonsService
import nl.julian.aerial.wallpaper.LocalMedia
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.ui.widgets.startSafely

private val IDLE_STEPS = listOf(1, 2, 5, 10, 0)
private val PHOTO_STEPS = listOf(10, 15, 30, 60)
private val DIM_STEPS = listOf(20, 40, 60)
private val TEXT_STEPS = listOf(100, 110, 120)
private val OPACITY_STEPS = listOf(62, 80, 45)

/** Launcher settings: the two-pane [SettingsScreen], plus the panels and dialogs its rows open. */
@Composable
fun SettingsPanel(config: LauncherConfig, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ConfigStore.get(context) }
    var layout by remember { mutableStateOf(false) }
    var shortcutsOpen by remember { mutableStateOf(false) }
    var startupOpen by remember { mutableStateOf(false) }
    var kidsAppsOpen by remember { mutableStateOf(false) }
    var haOpen by remember { mutableStateOf(false) }
    /** Plex sign-in: the code to enter at plex.tv/link while Aerial waits, or why it failed. */
    var plexCode by remember { mutableStateOf<String?>(null) }
    /** Home Assistant sign-in: 0 = closed, 1 server, 2 token. */
    var haStep by remember { mutableIntStateOf(0) }
    var haServer by remember { mutableStateOf("") }
    var haNote by remember { mutableStateOf<String?>(null) }
    /** Choosing a PIN; [pinThenOn] also turns kids mode on afterwards. */
    var pinDialog by remember { mutableStateOf(false) }
    var pinThenOn by remember { mutableStateOf(false) }
    var linkDialog by remember { mutableStateOf(false) }
    /** Jellyfin sign-in, one field at a time: 0 = closed, 1 server, 2 username, 3 password. */
    var jfStep by remember { mutableIntStateOf(0) }
    var jfServer by remember { mutableStateOf("") }
    var jfUser by remember { mutableStateOf("") }
    var jfNote by remember { mutableStateOf<String?>(null) }
    fun update(change: (LauncherConfig) -> LauncherConfig) = scope.launch { store.update(change) }

    var watchNextAccess by remember { mutableStateOf(WatchNextRepository.hasAccess(context)) }
    val askWatchNext = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        watchNextAccess = it
    }
    var mediaAccess by remember { mutableStateOf(LocalMedia.hasAccess(context)) }
    val askMedia = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        mediaAccess = it.values.all { granted -> granted }
    }
    val nowPlayingAccess = remember { NowPlayingRepository.hasAccess(context) }

    var cityDialog by remember { mutableStateOf(false) }
    var accessDialog by remember { mutableStateOf(false) }
    var backupNote by remember { mutableStateOf<String?>(null) }
    var lookNote by remember { mutableStateOf<String?>(null) }
    fun useLook(text: String?) {
        val look = text?.let(::decodeLook)
        if (look != null) update { it.withLook(look) }
        lookNote = if (look != null) "Look applied" else "That file isn't an Aerial look"
    }
    val pickLook = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { useLook(Backup.read(context, uri)) }
    }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            backupNote = if (Backup.restore(context, uri)) "Settings restored" else "That file isn't an Aerial backup"
        }
    }

    // Small builders, so every setting below is one line of what it is.
    fun toggle(icon: Int, title: String, checked: Boolean, description: String? = null, flip: (LauncherConfig) -> LauncherConfig) =
        SettingItem.Toggle(icon, title, checked, description) { update(flip) }
    fun <T> choice(
        icon: Int, title: String, current: T, options: List<T>, label: (T) -> String,
        description: String? = null, set: (LauncherConfig, T) -> LauncherConfig,
    ) = SettingItem.Choice(icon, title, options.map(label), options.indexOf(current).coerceAtLeast(0), description) { i ->
        update { set(it, options[i]) }
    }
    fun action(icon: Int, title: String, value: String? = null, onClick: () -> Unit) = SettingItem.Action(icon, title, value, onClick)

    val newVersion by produceState<String?>(null, config.updateCheck) {
        value = if (config.updateCheck) UpdateCheck.newer(context) else null
    }
    val remoteOn = remember { RemoteButtonsService.isEnabled(context) }
    val packs = remember { IconPacks.installed(context) }

    val categories = listOf(
        SettingCategory(Symbols.Settings, "System", listOfNotNull(
            newVersion?.let { version ->
                action(Symbols.Info, "Aerial $version is out", "Download it from ${UpdateCheck.PAGE.removePrefix("https://")}") {
                    context.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateCheck.PAGE)))
                }
            },
            action(Symbols.Play, "When the TV turns on", startupLabel(config.startup, emptyList())) { startupOpen = true },
            action(Symbols.Home, "Default home app", "Switch launchers") { context.startSafely(Intent(Settings.ACTION_HOME_SETTINGS)) },
            action(Symbols.Settings, "Android TV settings") { context.startSafely(Intent(Settings.ACTION_SETTINGS)) },
            action(Symbols.Home, "Run setup again", "The questions from the first start") { update { it.copy(setupDone = false) }; onDismiss() },
            toggle(Symbols.Info, "Check for updates", config.updateCheck, "Asks GitHub when Settings opens") { it.copy(updateCheck = !it.updateCheck) },
            action(Symbols.Storage, "Back up settings", backupNote ?: "Saves ${Backup.FILE} in Downloads") {
                scope.launch { backupNote = Backup.save(context)?.let { "Saved to $it" } ?: "Couldn't save the backup" }
            },
            action(Symbols.Storage, "Restore settings", "From ${Backup.FILE} in Downloads") {
                scope.launch {
                    if (Backup.restoreSaved(context)) backupNote = "Settings restored"
                    // Not ours (another TV, or a reset): let the user pick it, where the TV has a file picker.
                    else if (runCatching { pickBackup.launch(arrayOf("application/json", "*/*")) }.isFailure) {
                        backupNote = "No backup found in Downloads"
                    }
                }
            },
            toggle(Symbols.Hide, "Hide from recent apps", config.hideFromRecents) { it.copy(hideFromRecents = !it.hideFromRecents) },
            toggle(Symbols.Home, "Warn when Aerial isn't Home", config.homeWarning) { it.copy(homeWarning = !it.homeWarning) },
        )),
        SettingCategory(Symbols.Landscape, "Aerials", listOfNotNull(
            choice(Symbols.Landscape, "Scenery", config.scenery, Scenery.entries, { it.label }) { c, v -> c.copy(scenery = v) },
            choice(Symbols.Moon, "Day and night", config.aerialTime, AerialTime.entries, { it.label }) { c, v -> c.copy(aerialTime = v) },
            choice(Symbols.MediaPlayer, "Video quality", config.videoQuality, VideoQuality.entries, { it.label }) { c, v -> c.copy(videoQuality = v) },
            choice(
                Symbols.Timer, "Screensaver", config.idleMinutes, IDLE_STEPS,
                { if (it == 0) "Off" else "After $it min without input" },
            ) { c, v -> c.copy(idleMinutes = v) },
            choice(Symbols.Screensaver, "Screensaver shows", config.ambientInfo, AmbientInfo.entries, { it.label }) { c, v -> c.copy(ambientInfo = v) },
            toggle(Symbols.Location, "Where is this?", config.showPlaces, "The screensaver names the place on screen") { it.copy(showPlaces = !it.showPlaces) },
            toggle(Symbols.Shield, "Burn-in protection", config.shiftText, "Screensaver text moves a little every few minutes") { it.copy(shiftText = !it.shiftText) },
            choice(
                Symbols.Favorite, "Favourites", config.favouriteMode, FavouriteMode.entries, { it.label },
                "${config.favourites.size} saved. Hold OK on the screensaver to add one",
            ) { c, v -> c.copy(favouriteMode = v) },
            if (config.skipped.isEmpty()) null else action(Symbols.Block, "Skipped clips", "${config.skipped.size} skipped. Select to bring them all back") {
                update { it.copy(skipped = emptySet()) }
            },
            choice(
                Symbols.Disc, "Your photos and videos", config.ownMedia, OwnMedia.entries, { it.label },
                if (config.ownMedia != OwnMedia.Off && !mediaAccess) "Select to allow access" else "From a USB stick or SD card",
            ) { c, v ->
                if (v != OwnMedia.Off && !mediaAccess) askMedia.launch(LocalMedia.permissions)
                c.copy(ownMedia = v)
            },
            if (config.ownMedia == OwnMedia.Off) null else choice(
                Symbols.Timer, "Each photo", config.photoSeconds, PHOTO_STEPS, { "$it seconds" },
            ) { c, v -> c.copy(photoSeconds = v) },
            action(
                Symbols.Disc, "Video and photo links",
                (if (config.links.isEmpty()) "None" else "${config.links.size} links") + " · a video, live stream, photo, or a web folder ending in /",
            ) { linkDialog = true },
        )),
        SettingCategory(Symbols.Clock, "Clock & weather", listOf(
            choice(Symbols.Clock, "Clock", config.clockFormat, ClockFormat.entries, { it.label }) { c, v -> c.copy(clockFormat = v) },
            choice(Symbols.Clock, "Clock face", config.clockFace, ClockFace.entries, { it.label }) { c, v -> c.copy(clockFace = v) },
            choice(Symbols.Screensaver, "Screensaver clock", config.ambientClockFace, ClockFace.entries, { it.label }) { c, v -> c.copy(ambientClockFace = v) },
            choice(Symbols.Thermostat, "Temperature", config.tempUnit, TempUnit.entries, { it.label }) { c, v -> c.copy(tempUnit = v) },
            action(Symbols.Location, "Weather location", config.weatherCity.ifBlank { "Automatic (from the TV's timezone)" }) { cityDialog = true },
            action(Symbols.Calendar, "Date, time & timezone", "Android TV settings") { context.startSafely(Intent(Settings.ACTION_DATE_SETTINGS)) },
        )),
        SettingCategory(Symbols.Cloud, "Top bar", listOf(
            toggle(Symbols.Cloud, "Weather", config.barShowsWeather, "With the rain forecast") { it.copy(barShowsWeather = !it.barShowsWeather) },
            choice(
                Symbols.Calendar, "Forecast", config.weatherDetails, WeatherDetails.entries, { it.label },
                "Select the weather tile to see it",
            ) { c, v -> c.copy(weatherDetails = v) },
            toggle(Symbols.Music, "Now playing", config.barShowsMusic, "While music plays") { it.copy(barShowsMusic = !it.barShowsMusic) },
            toggle(Symbols.Screensaver, "Aerial name", config.barShowsAerial) { it.copy(barShowsAerial = !it.barShowsAerial) },
            toggle(Symbols.Search, "Search button", config.barShowsSearch, "Opens the Assistant") { it.copy(barShowsSearch = !it.barShowsSearch) },
            choice(Symbols.SmartButton, "Buttons", config.compactButtons, listOf(false, true), { if (it) "Icons only, stacked" else "Icons with labels" }) { c, v ->
                c.copy(compactButtons = v)
            },
            toggle(Symbols.Wifi, "Network icon", config.statusNetwork, "Next to the date") { it.copy(statusNetwork = !it.statusNetwork) },
            toggle(Symbols.Bluetooth, "Bluetooth icon", config.statusBluetooth, "Next to the date while on") { it.copy(statusBluetooth = !it.statusBluetooth) },
            toggle(Symbols.Storage, "Storage warning", config.statusStorage, "Shown when storage is almost full") { it.copy(statusStorage = !it.statusStorage) },
        )),
        SettingCategory(Symbols.Layout, "Home screen", listOf(
            action(Symbols.Layout, "Home layout", "Sections and quick settings: show, hide, reorder") { layout = true },
            choice(
                Symbols.Grid, "Tile size", config.tilesPerRow, listOf(6, 5, 4),
                { when (it) { 4 -> "Large, 4 per row"; 5 -> "Medium, 5 per row"; else -> "Normal, 6 per row" } },
            ) { c, v -> c.copy(tilesPerRow = v) },
            choice(Symbols.Style, "Tile style", config.tintedTiles, listOf(false, true), { if (it) "Tinted to the aerial" else "App banners" }) { c, v ->
                c.copy(tintedTiles = v)
            },
            choice(Symbols.Sort, "Order of All apps", config.appOrder, AppOrder.entries, { it.label }) { c, v -> c.copy(appOrder = v) },
            action(Symbols.Show, "Hidden apps", if (config.hidden.isEmpty()) "None" else "${config.hidden.size} hidden · select to show all") {
                update { it.copy(hidden = emptySet()) }
            },
            action(
                Symbols.Play, "Continue Watching",
                when {
                    !watchNextAccess -> "Off · select to allow"
                    config.watchNextHidden.isNotEmpty() -> "On · ${config.watchNextHidden.size} apps left out, select to bring them back"
                    else -> "On · hold OK on a show to leave its app out"
                },
            ) {
                if (!watchNextAccess) askWatchNext.launch(WatchNextRepository.PERMISSION)
                else update { it.copy(watchNextHidden = emptySet()) }
            },
            action(Symbols.Music, "Now Playing & notifications", if (nowPlayingAccess) "On" else "Off · select to see how to turn it on") {
                if (!nowPlayingAccess) accessDialog = true
            },
        )),
        SettingCategory(Symbols.Sparkle, "Looks", listOfNotNull(
            choice(Symbols.Palette, "Colours", config.themeSource, ThemeSource.entries, { it.label }) { c, v -> c.copy(themeSource = v) },
            if (config.themeSource != ThemeSource.Fixed) null else choice(
                Symbols.Palette, "Colour", config.themeColor, THEME_COLORS.map { it.first }, { c -> THEME_COLORS.first { it.first == c }.second },
            ) { c, v -> c.copy(themeColor = v) },
            choice(Symbols.Blur, "Panels", config.glass, Glass.entries, { it.label }) { c, v -> c.copy(glass = v) },
            choice(Symbols.Opacity, "Panel opacity", config.panelOpacity, OPACITY_STEPS.sorted(), { "$it%" }) { c, v -> c.copy(panelOpacity = v) },
            choice(Symbols.Corners, "Corners", config.roundness, listOf(40, 100, 140), { when (it) { 40 -> "Square"; 140 -> "Extra round"; else -> "Round" } }) { c, v ->
                c.copy(roundness = v)
            },
            choice(Symbols.TextWeight, "Text weight", config.fontWeight, listOf(-100, 0, 100), { when (it) { -100 -> "Lighter"; 100 -> "Bolder"; else -> "Normal" } }) { c, v ->
                c.copy(fontWeight = v)
            },
            choice(
                Symbols.IconPack, "Icon pack", config.iconPack, listOf<String?>(null) + packs.map { it.first },
                { p -> packs.firstOrNull { it.first == p }?.second ?: "App icons" },
                if (packs.isEmpty()) "None installed · for tinted and icon tiles" else "For tinted and icon tiles",
            ) { c, v -> c.copy(iconPack = v) },
            choice(Symbols.Motion, "Panels appear", config.reveal, Reveal.entries, { it.label }) { c, v -> c.copy(reveal = v) },
            choice(Symbols.Depth, "Tile depth", config.tileDepth, TileDepth.entries, { it.label }) { c, v -> c.copy(tileDepth = v) },
            choice(Symbols.FocusRing, "Focus ring", config.focusColor, FocusColor.entries, { it.label }) { c, v -> c.copy(focusColor = v) },
            choice(Symbols.Animation, "Opening animation", config.launchAnimation, LaunchAnimation.entries, { it.label }) { c, v -> c.copy(launchAnimation = v) },
            choice(Symbols.Speaker, "Focus sounds", config.focusSounds, FocusSounds.entries, { it.label }) { c, v -> c.copy(focusSounds = v) },
            toggle(Symbols.Snow, "Snow in December", config.snow) { it.copy(snow = !it.snow) },
            toggle(Symbols.Sun, "New Year's frame", config.newYearFrame, "31 Dec and 1 Jan") { it.copy(newYearFrame = !it.newYearFrame) },
            action(Symbols.Storage, "Share this look", lookNote ?: "Saves ${Backup.THEME_FILE} in Downloads") {
                scope.launch {
                    lookNote = Backup.write(context, Backup.THEME_FILE, config.look().encode())?.let { "Saved to $it" } ?: "Couldn't save the look"
                }
            },
            action(Symbols.Storage, "Use a shared look", "From ${Backup.THEME_FILE} in Downloads") {
                scope.launch {
                    val saved = Backup.read(context, Backup.THEME_FILE)
                    if (saved != null) useLook(saved)
                    else if (runCatching { pickLook.launch(arrayOf("application/json", "*/*")) }.isFailure) lookNote = "No look found in Downloads"
                }
            },
        )),
        SettingCategory(Symbols.TextSize, "Reading", listOf(
            choice(Symbols.TextSize, "Text size", config.textSize, TEXT_STEPS, { "$it%" }) { c, v -> c.copy(textSize = v) },
            toggle(Symbols.Contrast, "High contrast", config.highContrast, "All text at full brightness") { it.copy(highContrast = !it.highContrast) },
        )),
        SettingCategory(Symbols.Moon, "Night mode", listOfNotNull(
            choice(Symbols.Moon, "Dim the screen", config.nightMode, NightMode.entries, { it.label }) { c, v -> c.copy(nightMode = v) },
            if (config.nightMode == NightMode.Off) null else choice(
                Symbols.Brightness, "How much", config.nightDim, DIM_STEPS, { "$it% darker" },
            ) { c, v -> c.copy(nightDim = v) },
        )),
        SettingCategory(Symbols.Kids, "Kids mode", listOf(
            SettingItem.Toggle(Symbols.Kids, "Kids mode", config.kidsMode, "Only chosen apps, at chosen hours, PIN for the rest") {
                if (config.kidsMode) update { it.copy(kidsMode = false) }
                else if (config.kidsPin.isEmpty()) { pinThenOn = true; pinDialog = true }
                else update { it.copy(kidsMode = true) }
            },
            action(Symbols.Show, "Kids' apps", "${config.kidsApps.size} allowed") { kidsAppsOpen = true },
            choice(Symbols.Timer, "Allowed hours", config.kidsHours, KidsHours.entries, { it.label }) { c, v -> c.copy(kidsHours = v) },
            choice(
                Symbols.Timer, "Time per day", config.kidsDailyMinutes, listOf(0, 30, 60, 90, 120),
                { if (it == 0) "No limit" else "$it minutes" },
                if (config.kidsDailyMinutes == 0) null else "${config.kidsUsedToday()} minutes used today",
            ) { c, v -> c.copy(kidsDailyMinutes = v) },
            action(Symbols.PinCode, "PIN", if (config.kidsPin.isEmpty()) "Not set" else "Set · select to change") { pinThenOn = false; pinDialog = true },
        )),
        SettingCategory(Symbols.Home, "Connected services", listOf(
            action(
                Symbols.Home, "Home Assistant",
                config.homeAssistant?.let { "Connected to ${it.server.substringAfter("://")} · buttons, doorbell" } ?: "Not connected · select to sign in",
            ) { if (config.homeAssistant != null) haOpen = true else { haNote = null; haStep = 1 } },
            action(
                Symbols.MediaPlayer, "Plex",
                plexCode ?: config.plex?.let { "Connected to ${it.server.substringAfter("://").substringBefore('/')} · select to sign out" }
                    ?: "Not connected · select to sign in with a code",
            ) {
                if (config.plex != null) update { it.copy(plex = null) }
                else scope.launch {
                    PlexRepository.newPin(context).onSuccess { pin ->
                        plexCode = "Go to plex.tv/link and enter ${pin.code}"
                        PlexRepository.awaitLogin(context, pin)
                            .onSuccess { login -> plexCode = null; update { it.copy(plex = login) } }
                            .onFailure { plexCode = "Couldn't sign in: ${it.message}" }
                    }.onFailure { plexCode = "Couldn't reach Plex: ${it.message}" }
                }
            },
            action(
                Symbols.MediaPlayer, "Jellyfin",
                config.jellyfin?.let { "Connected to ${it.server.substringAfter("://")} · select to sign out" }
                    ?: "Not connected · select to sign in and show a Jellyfin row",
            ) {
                if (config.jellyfin != null) update { it.copy(jellyfin = null) } else { jfNote = null; jfStep = 1 }
            },
        )),
        SettingCategory(Symbols.Remote, "Remote", listOf(
            choice(
                Symbols.Remote, "Remote buttons", config.remoteButtons, RemoteButtons.entries, { it.label },
                when {
                    config.remoteButtons == RemoteButtons.Off -> "For TVs where Input or Home stop working"
                    remoteOn -> "They open Aerial"
                    else -> "Turn on Aerial in Accessibility settings"
                },
            ) { c, v ->
                if (v != RemoteButtons.Off && !remoteOn) context.startSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                c.copy(remoteButtons = v)
            },
            action(Symbols.Shortcut, "Remote shortcuts", "Hold Home or a colour button to open an app") { shortcutsOpen = true },
        )),
    )

    if (layout) LayoutPanel(config, { change -> update(change) }, onDismiss = { layout = false })
    else if (haOpen) HomeAssistantPanel(config, { change -> update(change) }, onDismiss = { haOpen = false })
    else if (kidsAppsOpen) KidsAppsPanel(config, { change -> update(change) }, onDismiss = { kidsAppsOpen = false })
    else if (startupOpen) StartupPanel(config, { change -> update(change) }, onDismiss = { startupOpen = false })
    else if (shortcutsOpen) ShortcutsPanel(config, remoteOn, { change -> update(change) }, onDismiss = { shortcutsOpen = false })
    else SettingsScreen(categories, onDismiss)

    if (accessDialog) AccessDialog(onDismiss = { accessDialog = false })
    if (pinDialog) RenameDialog(
        "Choose a PIN", current = "", resetLabel = null, password = true, note = "4 digits, to leave kids mode",
        onSave = { pin ->
            if (pin.length >= 4 && pin.all(Char::isDigit)) {
                pinDialog = false
                update { it.copy(kidsPin = pin, kidsMode = it.kidsMode || pinThenOn) }
            }
        },
        onDismiss = { pinDialog = false },
    )
    when (haStep) {
        1 -> RenameDialog(
            "Home Assistant address", current = haServer.ifEmpty { "http://homeassistant.local:8123" }, resetLabel = null,
            note = haNote, onSave = { haServer = it.trim().trimEnd('/'); haStep = if (it.length > 7) 2 else 0 }, onDismiss = { haStep = 0 },
        )
        2 -> RenameDialog(
            "Long-lived access token", current = "", resetLabel = null, password = true,
            note = "Make one in Home Assistant under your profile, Security. Typing it with the Google TV app on your phone is easiest.",
            onSave = { token ->
                haStep = 0
                scope.launch {
                    val ha = HaConfig(haServer, token.trim())
                    HomeAssistant.states(ha)
                        .onSuccess { update { it.copy(homeAssistant = ha) }; haOpen = true }
                        .onFailure { haNote = "Couldn't connect: ${it.message}"; haStep = 1 }
                }
            },
            onDismiss = { haStep = 0 },
        )
    }
    when (jfStep) {
        1 -> RenameDialog(
            "Jellyfin server", current = jfServer.ifEmpty { "http://" }, resetLabel = null, note = jfNote ?: "For example http://192.168.1.10:8096",
            onSave = { jfServer = it; jfStep = if (it.length > 7) 2 else 0 }, onDismiss = { jfStep = 0 },
        )
        2 -> RenameDialog(
            "Username", current = jfUser, resetLabel = null,
            onSave = { jfUser = it; jfStep = 3 }, onDismiss = { jfStep = 0 },
        )
        3 -> RenameDialog(
            "Password", current = "", resetLabel = null, password = true,
            onSave = { pw ->
                jfStep = 0
                scope.launch {
                    JellyfinRepository.login(context, jfServer, jfUser, pw)
                        .onSuccess { login -> update { it.copy(jellyfin = login) } }
                        .onFailure { jfNote = "Couldn't sign in: ${it.message}"; jfStep = 1 }
                }
            },
            onDismiss = { jfStep = 0 },
        )
    }
    if (linkDialog) RenameDialog(
        title = "Add a link", current = "https://", resetLabel = if (config.links.isEmpty()) null else "Remove all links",
        onSave = { url ->
            linkDialog = false
            update {
                when {
                    url.isEmpty() -> it.copy(links = emptyList())
                    url.startsWith("http") && url.length > 10 -> it.copy(
                        links = (it.links + url).distinct(),
                        ownMedia = if (it.ownMedia == OwnMedia.Off) OwnMedia.Mix else it.ownMedia,
                    )
                    else -> it
                }
            }
        },
        onDismiss = { linkDialog = false },
    )
    if (cityDialog) {
        CityDialog(
            current = config.weatherCity,
            onSave = { city ->
                update { it.copy(weatherCity = city) }
                cityDialog = false
            },
            onDismiss = { cityDialog = false },
        )
    }
}

/** The next value of an enum, wrapping around (for settings rows that cycle on select). */
internal inline fun <reified T : Enum<T>> T.next(): T = enumValues<T>().let { it[(ordinal + 1) % it.size] }
