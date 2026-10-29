package nl.julian.aerial.ui.menus

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.data.LauncherConfig
import nl.julian.aerial.data.NowPlayingRepository
import nl.julian.aerial.data.WatchNextRepository
import nl.julian.aerial.remote.RemoteButtonsService
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.ui.Text
import nl.julian.aerial.ui.isDefaultHome
import nl.julian.aerial.ui.widgets.startSafely

/**
 * First-run setup, one short step at a time: Home app, aerials, clock and
 * weather, the home screen, and access. Everything here is also in
 * Settings; Settings > System > Run setup again opens it later.
 */
@Composable
fun SetupWizard(config: LauncherConfig, update: ((LauncherConfig) -> LauncherConfig) -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var cityDialog by remember { mutableStateOf(false) }
    var accessDialog by remember { mutableStateOf(false) }
    // Re-checked whenever we come back from the TV's own settings.
    var isHome by remember { mutableStateOf(isDefaultHome(context)) }
    var watchNext by remember { mutableStateOf(WatchNextRepository.hasAccess(context)) }
    var remoteOn by remember { mutableStateOf(RemoteButtonsService.isEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        isHome = isDefaultHome(context)
        watchNext = WatchNextRepository.hasAccess(context)
        remoteOn = RemoteButtonsService.isEnabled(context)
        onPauseOrDispose { }
    }
    val askWatchNext = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { watchNext = it }
    val last = 6

    Overlay(onDismiss = { if (step > 0) step-- }) {
        Column(
            Modifier
                .width(640.dp)
                .background(LocalSurfaces.current.default, MaterialTheme.shapes.extraLarge)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val (title, intro) = when (step) {
                0 -> "Welcome to Aerial" to "A few questions to set up your TV. Everything can be changed later in Settings."
                1 -> "Home button" to "Aerial works best as the app the Home button opens."
                2 -> "Aerials" to "What plays behind the home screen and as the screensaver."
                3 -> "Clock & weather" to "How the time and temperature are shown, and where the weather is for."
                4 -> "Home screen" to "How your apps look."
                5 -> "Access" to "Optional extras that need permission from the TV."
                else -> "All set" to "Enjoy the view. Hold OK on an app for more options, and find the rest in Settings."
            }
            Text("${step + 1} / ${last + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(intro, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.padding(4.dp))

            key(step) {
                val first = remember { FocusRequester() }
                // Steps with choices start on the first one; the others on Start or Finish.
                val buttonsOnly = step == 0 || step == last || (step == 1 && isHome)
                Column(
                    (if (buttonsOnly) Modifier else Modifier.focusRequester(first)).focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    when (step) {
                        1 -> MenuRow(
                            Symbols.Home, if (isHome) "Aerial is your Home app" else "Make Aerial the Home app",
                            value = if (isHome) "Nothing to do here" else "Opens the TV's settings. Choose Aerial there.",
                            onClick = { if (!isHome) context.startSafely(Intent(Settings.ACTION_HOME_SETTINGS)) },
                        )
                        2 -> {
                            MenuRow(Symbols.Landscape, "Scenery", value = config.scenery.label, onClick = { update { it.copy(scenery = it.scenery.next()) } })
                            MenuRow(Symbols.Moon, "Day and night", value = config.aerialTime.label, onClick = { update { it.copy(aerialTime = it.aerialTime.next()) } })
                            MenuRow(Symbols.MediaPlayer, "Video quality", value = config.videoQuality.label, onClick = { update { it.copy(videoQuality = it.videoQuality.next()) } })
                        }
                        3 -> {
                            MenuRow(Symbols.Clock, "Clock", value = config.clockFormat.label, onClick = { update { it.copy(clockFormat = it.clockFormat.next()) } })
                            MenuRow(Symbols.Thermostat, "Temperature", value = config.tempUnit.label, onClick = { update { it.copy(tempUnit = it.tempUnit.next()) } })
                            MenuRow(
                                Symbols.Location, "Weather location", value = config.weatherCity.ifBlank { "Automatic (from the TV's timezone)" },
                                onClick = { cityDialog = true },
                            )
                        }
                        4 -> {
                            MenuRow(
                                Symbols.Tune, "Tile size",
                                value = when (config.tilesPerRow) { 4 -> "Large, 4 per row"; 5 -> "Medium, 5 per row"; else -> "Normal, 6 per row" },
                                onClick = { update { it.copy(tilesPerRow = if (it.tilesPerRow <= 4) 6 else it.tilesPerRow - 1) } },
                            )
                            MenuRow(
                                Symbols.Tune, "Tile style", value = if (config.tintedTiles) "Tinted to the aerial" else "App banners",
                                onClick = { update { it.copy(tintedTiles = !it.tintedTiles) } },
                            )
                            MenuRow(Symbols.Tune, "Panels", value = config.glass.label, onClick = { update { it.copy(glass = it.glass.next()) } })
                        }
                        5 -> {
                            MenuRow(
                                Symbols.Play, "Continue Watching", value = if (watchNext) "On" else "Off · select to allow",
                                onClick = { if (!watchNext) askWatchNext.launch(WatchNextRepository.PERMISSION) },
                            )
                            MenuRow(
                                Symbols.Music, "Now Playing & notifications",
                                value = if (NowPlayingRepository.hasAccess(context)) "On" else "Off · select to see how to turn it on",
                                onClick = { if (!NowPlayingRepository.hasAccess(context)) accessDialog = true },
                            )
                            MenuRow(
                                Symbols.Input, "Remote buttons", value = if (remoteOn) "On" else "Off · for TVs where Input or Home stop working",
                                onClick = { if (!remoteOn) context.startSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            )
                        }
                    }
                    Spacer(Modifier.padding(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.End)) {
                        when (step) {
                            0 -> DialogButton("Skip", primary = false, onClick = onDone)
                            last -> {}
                            else -> DialogButton("Back", primary = false) { step-- }
                        }
                        DialogButton(
                            when (step) { 0 -> "Start"; last -> "Finish"; else -> "Next" }, primary = true,
                            modifier = if (buttonsOnly) Modifier.focusRequester(first) else Modifier,
                        ) { if (step == last) onDone() else step++ }
                    }
                }
                LaunchedEffect(Unit) {
                    // One frame, so the rows and buttons are laid out and can take focus.
                    withFrameNanos { }
                    runCatching { first.requestFocus() }
                }
            }
        }
    }
    if (cityDialog) CityDialog(
        current = config.weatherCity,
        onSave = { city -> update { it.copy(weatherCity = city) }; cityDialog = false },
        onDismiss = { cityDialog = false },
    )
    if (accessDialog) AccessDialog(onDismiss = { accessDialog = false })
}
