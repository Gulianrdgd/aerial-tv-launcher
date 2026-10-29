package nl.julian.aerial.ui.menus

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
import androidx.tv.material3.Text
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

/** Launcher settings, in the right-hand side panel. */
@Composable
fun SettingsPanel(config: LauncherConfig, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ConfigStore.get(context) }
    val first = remember { FocusRequester() }
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
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            backupNote = if (Backup.restore(context, uri)) "Settings restored" else "That file isn't an Aerial backup"
        }
    }

    SidePanel("Settings", onDismiss, initialFocus = first) {
        Section("Aerials")
        MenuRow(
            Symbols.Landscape, "Scenery", value = config.scenery.label, focusRequester = first,
            onClick = { update { it.copy(scenery = it.scenery.next()) } },
        )
        MenuRow(
            Symbols.Moon, "Day and night", value = config.aerialTime.label,
            onClick = { update { it.copy(aerialTime = it.aerialTime.next()) } },
        )
        MenuRow(
            Symbols.Disc, "Your photos and videos",
            value = when {
                config.ownMedia == OwnMedia.Off -> "Off · from a USB stick or SD card"
                !mediaAccess -> "${config.ownMedia.label} · select to allow access"
                else -> config.ownMedia.label
            },
            onClick = {
                if (config.ownMedia != OwnMedia.Off && !mediaAccess) askMedia.launch(LocalMedia.permissions)
                else {
                    update { it.copy(ownMedia = it.ownMedia.next()) }
                    if (!mediaAccess) askMedia.launch(LocalMedia.permissions)
                }
            },
        )
        if (config.ownMedia != OwnMedia.Off) {
            MenuRow(
                Symbols.Timer, "Each photo", value = "${config.photoSeconds} seconds",
                onClick = {
                    update {
                        val i = PHOTO_STEPS.indexOf(it.photoSeconds)
                        it.copy(photoSeconds = PHOTO_STEPS[(i + 1) % PHOTO_STEPS.size])
                    }
                },
            )
        }
        MenuRow(
            Symbols.Location, "Where is this?",
            value = if (config.showPlaces) "Screensaver names the place on screen" else "Off · shows the scenery",
            onClick = { update { it.copy(showPlaces = !it.showPlaces) } },
        )
        MenuRow(
            Symbols.Tune, "Burn-in protection",
            value = if (config.shiftText) "Screensaver text moves a little every few minutes" else "Off · for OLED TVs",
            onClick = { update { it.copy(shiftText = !it.shiftText) } },
        )
        MenuRow(
            Symbols.MediaPlayer, "Video quality", value = config.videoQuality.label,
            onClick = { update { it.copy(videoQuality = it.videoQuality.next()) } },
        )
        MenuRow(
            Symbols.Screensaver, "Screensaver shows", value = config.ambientInfo.label,
            onClick = { update { it.copy(ambientInfo = it.ambientInfo.next()) } },
        )
        MenuRow(
            Symbols.Favorite, "Favourites",
            value = "${config.favouriteMode.label} · ${config.favourites.size} saved. Hold OK on the screensaver to add one",
            onClick = { update { it.copy(favouriteMode = it.favouriteMode.next()) } },
        )
        if (config.skipped.isNotEmpty()) {
            MenuRow(
                Symbols.Block, "Skipped clips", value = "${config.skipped.size} skipped. Select to bring them all back",
                onClick = { update { it.copy(skipped = emptySet()) } },
            )
        }
        MenuRow(
            Symbols.Timer, "Screensaver",
            value = if (config.idleMinutes == 0) "Off" else "After ${config.idleMinutes} min without input",
            onClick = {
                update {
                    val i = IDLE_STEPS.indexOf(it.idleMinutes)
                    it.copy(idleMinutes = IDLE_STEPS[(i + 1) % IDLE_STEPS.size])
                }
            },
        )

        Section("Clock & weather")
        MenuRow(
            Symbols.Clock, "Clock face", value = config.clockFace.label,
            onClick = { update { it.copy(clockFace = it.clockFace.next()) } },
        )
        MenuRow(
            Symbols.Screensaver, "Screensaver clock", value = config.ambientClockFace.label,
            onClick = { update { it.copy(ambientClockFace = it.ambientClockFace.next()) } },
        )
        MenuRow(
            Symbols.Clock, "Clock", value = config.clockFormat.label,
            onClick = { update { it.copy(clockFormat = it.clockFormat.next()) } },
        )
        MenuRow(
            Symbols.Thermostat, "Temperature", value = config.tempUnit.label,
            onClick = { update { it.copy(tempUnit = it.tempUnit.next()) } },
        )
        MenuRow(
            Symbols.Location, "Weather location",
            value = config.weatherCity.ifBlank { "Automatic (from the TV's timezone)" },
            onClick = { cityDialog = true },
        )
        MenuRow(
            Symbols.Calendar, "Date, time & timezone", value = "Android TV settings",
            onClick = { context.startSafely(Intent(Settings.ACTION_DATE_SETTINGS)) },
        )

        Section("Top bar")
        MenuRow(
            Symbols.Cloud, "Weather", value = if (config.barShowsWeather) "Shown, with the rain forecast" else "Hidden",
            onClick = { update { it.copy(barShowsWeather = !it.barShowsWeather) } },
        )
        MenuRow(
            Symbols.Calendar, "Forecast", value = config.weatherDetails.label + if (config.weatherDetails != WeatherDetails.Off) " · select the weather tile" else "",
            onClick = { update { it.copy(weatherDetails = it.weatherDetails.next()) } },
        )
        MenuRow(
            Symbols.Screensaver, "Aerial name", value = if (config.barShowsAerial) "Shown" else "Hidden",
            onClick = { update { it.copy(barShowsAerial = !it.barShowsAerial) } },
        )
        MenuRow(
            Symbols.Music, "Now playing", value = if (config.barShowsMusic) "Shown while music plays" else "Hidden",
            onClick = { update { it.copy(barShowsMusic = !it.barShowsMusic) } },
        )
        MenuRow(
            Symbols.Search, "Search button", value = if (config.barShowsSearch) "Shown, opens the Assistant" else "Hidden",
            onClick = { update { it.copy(barShowsSearch = !it.barShowsSearch) } },
        )
        MenuRow(
            Symbols.Wifi, "Network icon", value = if (config.statusNetwork) "Next to the date" else "Hidden",
            onClick = { update { it.copy(statusNetwork = !it.statusNetwork) } },
        )
        MenuRow(
            Symbols.Bluetooth, "Bluetooth icon", value = if (config.statusBluetooth) "Next to the date while on" else "Hidden",
            onClick = { update { it.copy(statusBluetooth = !it.statusBluetooth) } },
        )
        MenuRow(
            Symbols.Storage, "Storage warning", value = if (config.statusStorage) "Shown when storage is almost full" else "Hidden",
            onClick = { update { it.copy(statusStorage = !it.statusStorage) } },
        )
        MenuRow(
            Symbols.Tune, "Buttons", value = if (config.compactButtons) "Icons only, stacked" else "Icons with labels",
            onClick = { update { it.copy(compactButtons = !it.compactButtons) } },
        )

        Section("Home screen")
        MenuRow(
            Symbols.Snow, "Snow in December", value = if (config.snow) "On" else "Off",
            onClick = { update { it.copy(snow = !it.snow) } },
        )
        MenuRow(
            Symbols.Sun, "New Year's frame", value = if (config.newYearFrame) "On, 31 Dec and 1 Jan" else "Off",
            onClick = { update { it.copy(newYearFrame = !it.newYearFrame) } },
        )
        MenuRow(
            Symbols.Tune, "Panels appear", value = config.reveal.label,
            onClick = { update { it.copy(reveal = it.reveal.next()) } },
        )
        MenuRow(
            Symbols.Tune, "Tile depth", value = config.tileDepth.label,
            onClick = { update { it.copy(tileDepth = it.tileDepth.next()) } },
        )
        MenuRow(
            Symbols.Tune, "Focus ring", value = config.focusColor.label,
            onClick = { update { it.copy(focusColor = it.focusColor.next()) } },
        )
        MenuRow(
            Symbols.Tune, "Opening animation", value = config.launchAnimation.label,
            onClick = { update { it.copy(launchAnimation = it.launchAnimation.next()) } },
        )
        MenuRow(
            Symbols.Tune, "Panels", value = config.glass.label,
            onClick = { update { it.copy(glass = it.glass.next()) } },
        )
        MenuRow(
            Symbols.Tune, "Text size", value = "${config.textSize}%",
            onClick = {
                update {
                    val i = TEXT_STEPS.indexOf(it.textSize)
                    it.copy(textSize = TEXT_STEPS[(i + 1) % TEXT_STEPS.size])
                }
            },
        )
        MenuRow(
            Symbols.Tune, "Contrast", value = if (config.highContrast) "High · all text at full brightness" else "Normal",
            onClick = { update { it.copy(highContrast = !it.highContrast) } },
        )
        MenuRow(
            Symbols.Tune, "Order of All apps", value = config.appOrder.label,
            onClick = { update { it.copy(appOrder = it.appOrder.next()) } },
        )
        MenuRow(
            Symbols.Clock, "Recent row", value = if (config.recentRow) "Shown above All apps" else "Hidden",
            onClick = { update { it.copy(recentRow = !it.recentRow) } },
        )
        MenuRow(
            Symbols.Tune, "Tile style",
            value = if (config.tintedTiles) "Tinted to the aerial" else "App banners",
            onClick = { update { it.copy(tintedTiles = !it.tintedTiles) } },
        )
        MenuRow(
            Symbols.Show, "Hidden apps",
            value = if (config.hidden.isEmpty()) "None" else "${config.hidden.size} hidden · select to show all",
            onClick = { update { it.copy(hidden = emptySet()) } },
        )
        MenuRow(
            Symbols.Play, "Continue Watching",
            value = if (watchNextAccess) "On" else "Off · select to allow",
            onClick = { if (!watchNextAccess) askWatchNext.launch(WatchNextRepository.PERMISSION) },
        )
        MenuRow(
            Symbols.Music, "Now Playing & notifications",
            value = if (nowPlayingAccess) "On" else "Off · select to see how to turn it on",
            onClick = { if (!nowPlayingAccess) accessDialog = true },
        )

        Section("Night mode")
        MenuRow(
            Symbols.Moon, "Dim the screen", value = config.nightMode.label,
            onClick = { update { it.copy(nightMode = it.nightMode.next()) } },
        )
        if (config.nightMode != NightMode.Off) {
            MenuRow(
                Symbols.Tune, "How much", value = "${config.nightDim}% darker",
                onClick = {
                    update {
                        val i = DIM_STEPS.indexOf(it.nightDim)
                        it.copy(nightDim = DIM_STEPS[(i + 1) % DIM_STEPS.size])
                    }
                },
            )
        }

        Section("System")
        val newVersion by produceState<String?>(null, config.updateCheck) {
            value = if (config.updateCheck) UpdateCheck.newer(context) else null
        }
        newVersion?.let { version ->
            MenuRow(
                Symbols.Info, "Aerial $version is out", value = "Download it from ${UpdateCheck.PAGE.removePrefix("https://")}",
                onClick = { context.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateCheck.PAGE))) },
            )
        }
        MenuRow(
            Symbols.Info, "Check for updates", value = if (config.updateCheck) "On · asks GitHub when Settings opens" else "Off",
            onClick = { update { it.copy(updateCheck = !it.updateCheck) } },
        )
        MenuRow(
            Symbols.Storage, "Back up settings", value = backupNote ?: "Saves ${Backup.FILE} in Downloads",
            onClick = {
                scope.launch { backupNote = Backup.save(context)?.let { "Saved to $it" } ?: "Couldn't save the backup" }
            },
        )
        MenuRow(
            Symbols.Storage, "Restore settings", value = "From ${Backup.FILE} in Downloads",
            onClick = {
                scope.launch {
                    if (Backup.restoreSaved(context)) backupNote = "Settings restored"
                    // Not ours (another TV, or a reset): let the user pick it, where the TV has a file picker.
                    else if (runCatching { pickBackup.launch(arrayOf("application/json", "*/*")) }.isFailure) {
                        backupNote = "No backup found in Downloads"
                    }
                }
            },
        )
        val remoteOn = remember { RemoteButtonsService.isEnabled(context) }
        MenuRow(
            Symbols.Tune, "Remote buttons",
            value = when {
                config.remoteButtons == RemoteButtons.Off -> "Off · for TVs where Input or Home stop working"
                remoteOn -> "${config.remoteButtons.label} open Aerial"
                else -> "${config.remoteButtons.label} · turn on Aerial in Accessibility settings"
            },
            onClick = {
                update { it.copy(remoteButtons = it.remoteButtons.next()) }
                if (!remoteOn && config.remoteButtons == RemoteButtons.Off) {
                    context.startSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            },
        )
        MenuRow(
            Symbols.Home, "Default home app",
            value = "Switch launchers",
            onClick = { context.startSafely(Intent(Settings.ACTION_HOME_SETTINGS)) },
        )
        MenuRow(
            Symbols.Settings, "Android TV settings",
            onClick = { context.startSafely(Intent(Settings.ACTION_SETTINGS)) },
        )
    }

    if (accessDialog) AccessDialog(onDismiss = { accessDialog = false })
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

@Composable
private fun Section(title: String) {
    Text(
        title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
    )
}

/** The next value of an enum, wrapping around (for settings rows that cycle on select). */
private inline fun <reified T : Enum<T>> T.next(): T = enumValues<T>().let { it[(ordinal + 1) % it.size] }
