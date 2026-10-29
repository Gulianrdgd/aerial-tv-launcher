package nl.julian.aerial.ui.menus

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
import nl.julian.aerial.data.RemoteButtons
import nl.julian.aerial.data.WatchNextRepository
import nl.julian.aerial.remote.RemoteButtonsService
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.ui.widgets.startSafely

private val IDLE_STEPS = listOf(1, 2, 5, 10, 0)

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
    val nowPlayingAccess = remember { NowPlayingRepository.hasAccess(context) }

    var cityDialog by remember { mutableStateOf(false) }
    var accessDialog by remember { mutableStateOf(false) }

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
            Symbols.Tune, "Buttons", value = if (config.compactButtons) "Icons only, stacked" else "Icons with labels",
            onClick = { update { it.copy(compactButtons = !it.compactButtons) } },
        )

        Section("Home screen")
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

        Section("System")
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
