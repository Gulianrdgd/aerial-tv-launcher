package nl.julian.aerial.ui.menus

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import nl.julian.aerial.data.AerialSource
import nl.julian.aerial.data.ConfigStore
import nl.julian.aerial.data.LauncherConfig
import nl.julian.aerial.data.NowPlayingRepository
import nl.julian.aerial.data.Scenery
import nl.julian.aerial.data.WatchNextRepository
import nl.julian.aerial.theme.AerialType
import nl.julian.aerial.theme.LineIcons
import nl.julian.aerial.theme.LocalLauncherColors
import nl.julian.aerial.theme.glass
import nl.julian.aerial.ui.widgets.startSafely

private val IDLE_STEPS = listOf(1, 2, 5, 10, 0)

/** Launcher settings: a glass side panel on the right, Google TV style. */
@Composable
fun SettingsPanel(config: LauncherConfig, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalLauncherColors.current
    val scope = rememberCoroutineScope()
    val store = remember { ConfigStore.get(context) }
    val first = remember { FocusRequester() }
    fun update(change: (LauncherConfig) -> LauncherConfig) = scope.launch { store.update(change) }

    var watchNextAccess by remember { mutableStateOf(WatchNextRepository.hasAccess(context)) }
    val askWatchNext = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        watchNextAccess = it
    }
    val nowPlayingAccess = remember { NowPlayingRepository.hasAccess(context) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp), contentAlignment = Alignment.CenterEnd) {
            Column(
                Modifier
                    .width(380.dp)
                    .fillMaxHeight()
                    .glass(RoundedCornerShape(36.dp), colors.panel.copy(alpha = 0.92f))
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("SETTINGS", style = AerialType.eyebrow, color = colors.contentMuted, modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 6.dp))

                MenuRow(
                    LineIcons.Mountains, "Scenery", value = config.scenery.label, focusRequester = first,
                    onClick = {
                        update {
                            val all = Scenery.entries
                            it.copy(scenery = all[(all.indexOf(it.scenery) + 1) % all.size])
                        }
                    },
                )
                MenuRow(
                    LineIcons.Film, "Aerial collection", value = config.aerialSource.label,
                    onClick = {
                        update {
                            val all = AerialSource.entries
                            it.copy(aerialSource = all[(all.indexOf(it.aerialSource) + 1) % all.size])
                        }
                    },
                )
                MenuRow(
                    LineIcons.Timer, "Screensaver",
                    value = if (config.idleMinutes == 0) "Off" else "After ${config.idleMinutes} min without input",
                    onClick = {
                        update {
                            val i = IDLE_STEPS.indexOf(it.idleMinutes)
                            it.copy(idleMinutes = IDLE_STEPS[(i + 1) % IDLE_STEPS.size])
                        }
                    },
                )
                MenuRow(
                    LineIcons.Eye, "Hidden apps",
                    value = if (config.hidden.isEmpty()) "None" else "${config.hidden.size} hidden · select to show all",
                    onClick = { update { it.copy(hidden = emptySet()) } },
                )
                MenuRow(
                    LineIcons.Play, "Continue Watching",
                    value = if (watchNextAccess) "On" else "Off · select to allow",
                    onClick = { if (!watchNextAccess) askWatchNext.launch(WatchNextRepository.PERMISSION) },
                )
                MenuRow(
                    LineIcons.Music, "Now Playing",
                    value = if (nowPlayingAccess) "On" else "Off · grant with adb (see README)",
                    onClick = {},
                )
                MenuRow(
                    LineIcons.Home, "Default home app",
                    value = "Switch launchers",
                    onClick = { context.startSafely(Intent(Settings.ACTION_HOME_SETTINGS)) },
                )
                MenuRow(
                    LineIcons.Gear, "Android TV settings",
                    onClick = { context.startSafely(Intent(Settings.ACTION_SETTINGS)) },
                )
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}
