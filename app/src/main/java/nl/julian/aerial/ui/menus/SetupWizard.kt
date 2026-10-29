package nl.julian.aerial.ui.menus

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.data.LauncherConfig
import nl.julian.aerial.data.NowPlayingRepository
import nl.julian.aerial.data.WatchNextRepository
import nl.julian.aerial.remote.RemoteButtonsService
import nl.julian.aerial.theme.EyebrowStyle
import nl.julian.aerial.theme.FluidEasing
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.theme.panel
import nl.julian.aerial.theme.topHighlight
import nl.julian.aerial.ui.FocusSurface
import nl.julian.aerial.ui.Text
import nl.julian.aerial.ui.isDefaultHome
import nl.julian.aerial.ui.tr
import nl.julian.aerial.ui.widgets.startSafely

/** Outer radius of the card; the inner core is inset by [SHELL] and keeps its curve concentric. */
private val CARD_RADIUS = 36.dp
private val SHELL = 8.dp

/**
 * First-run setup, one short step at a time: Home app, aerials, clock and
 * weather, the home screen, and access. Everything here is also in
 * Settings; Settings > System > Run setup again opens it later.
 *
 * Built like the home screen's panels: a see-through shell with a hairline
 * rim holding a solid inner card. Steps slide sideways in the direction you
 * move, on the same easing as the rest of Aerial.
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

    // The card rises into place once, when the wizard opens.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(800, easing = FluidEasing)) }

    Overlay(onDismiss = { if (step > 0) step-- }) {
        Box(
            Modifier
                .width(680.dp)
                .graphicsLayer {
                    alpha = entrance.value
                    translationY = (1f - entrance.value) * 48.dp.toPx()
                }
                .panel(RoundedCornerShape(CARD_RADIUS))
                .padding(SHELL),
        ) {
            val core = RoundedCornerShape(CARD_RADIUS - SHELL)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(LocalSurfaces.current.default, core)
                    .topHighlight(core)
                    .padding(horizontal = 32.dp, vertical = 28.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepEyebrow(step + 1, last + 1)
                    Spacer(Modifier.weight(1f))
                    ProgressDots(step, last + 1)
                }
                Spacer(Modifier.height(20.dp))

                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInHorizontally(tween(550, easing = FluidEasing)) { w -> (if (forward) w else -w) / 5 } +
                            fadeIn(tween(450, delayMillis = 80, easing = FluidEasing)))
                            .togetherWith(
                                slideOutHorizontally(tween(350, easing = FluidEasing)) { w -> (if (forward) -w else w) / 5 } +
                                    fadeOut(tween(200)),
                            )
                    },
                    label = "step",
                ) { s ->
                    StepPage(
                        s, last, config, update, isHome, watchNext, remoteOn,
                        onHome = { context.startSafely(Intent(Settings.ACTION_HOME_SETTINGS)) },
                        onWatchNext = { askWatchNext.launch(WatchNextRepository.PERMISSION) },
                        onNowPlaying = { accessDialog = true },
                        onRemote = { context.startSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        onCity = { cityDialog = true },
                        onBack = { step-- },
                        onNext = { if (s == last) onDone() else step++ },
                        onSkip = onDone,
                    )
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

@Composable
private fun StepPage(
    step: Int,
    last: Int,
    config: LauncherConfig,
    update: ((LauncherConfig) -> LauncherConfig) -> Unit,
    isHome: Boolean,
    watchNext: Boolean,
    remoteOn: Boolean,
    onHome: () -> Unit,
    onWatchNext: () -> Unit,
    onNowPlaying: () -> Unit,
    onRemote: () -> Unit,
    onCity: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val (title, intro) = when (step) {
        0 -> "Welcome to Aerial" to "A few questions to set up your TV. Everything can be changed later in Settings."
        1 -> "Home button" to "Aerial works best as the app the Home button opens."
        2 -> "Aerials" to "What plays behind the home screen and as the screensaver."
        3 -> "Clock & weather" to "How the time and temperature are shown, and where the weather is for."
        4 -> "Home screen" to "How your apps look."
        5 -> "Access" to "Optional extras that need permission from the TV."
        else -> "All set" to "Enjoy the view. Hold OK on an app for more options, and find the rest in Settings."
    }
    val first = remember { FocusRequester() }
    // Steps with choices start on the first one; the others on Start, Next or Finish.
    val buttonsOnly = step == 0 || step == last || (step == 1 && isHome)

    Column(Modifier.fillMaxWidth()) {
        Text(
            title, color = MaterialTheme.colorScheme.onSurface,
            style = if (step == 0 || step == last) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(intro, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        Column(
            (if (buttonsOnly) Modifier else Modifier.focusRequester(first)).focusGroup(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            when (step) {
                1 -> MenuRow(
                    Symbols.Home, if (isHome) "Aerial is your Home app" else "Make Aerial the Home app",
                    value = if (isHome) "Nothing to do here" else "Opens the TV's settings. Choose Aerial there.",
                    onClick = { if (!isHome) onHome() },
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
                        onClick = onCity,
                    )
                }
                4 -> {
                    MenuRow(
                        Symbols.Grid, "Tile size",
                        value = when (config.tilesPerRow) { 4 -> "Large, 4 per row"; 5 -> "Medium, 5 per row"; else -> "Normal, 6 per row" },
                        onClick = { update { it.copy(tilesPerRow = if (it.tilesPerRow <= 4) 6 else it.tilesPerRow - 1) } },
                    )
                    MenuRow(
                        Symbols.Style, "Tile style", value = if (config.tintedTiles) "Tinted to the aerial" else "App banners",
                        onClick = { update { it.copy(tintedTiles = !it.tintedTiles) } },
                    )
                    MenuRow(Symbols.Blur, "Panels", value = config.glass.label, onClick = { update { it.copy(glass = it.glass.next()) } })
                }
                5 -> {
                    MenuRow(
                        Symbols.Play, "Continue Watching", value = if (watchNext) "On" else "Off · select to allow",
                        onClick = { if (!watchNext) onWatchNext() },
                    )
                    val nowPlaying = NowPlayingRepository.hasAccess(context)
                    MenuRow(
                        Symbols.Music, "Now Playing & notifications",
                        value = if (nowPlaying) "On" else "Off · select to see how to turn it on",
                        onClick = { if (!nowPlaying) onNowPlaying() },
                    )
                    MenuRow(
                        Symbols.Remote, "Remote buttons", value = if (remoteOn) "On" else "Off · for TVs where Input or Home stop working",
                        onClick = { if (!remoteOn) onRemote() },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                when (step) {
                    0 -> DialogButton("Skip", primary = false, onClick = onSkip)
                    last -> {}
                    else -> DialogButton("Back", primary = false, onClick = onBack)
                }
                ArrowButton(
                    when (step) { 0 -> "Start"; last -> "Finish"; else -> "Next" },
                    modifier = if (buttonsOnly) Modifier.focusRequester(first) else Modifier,
                    onClick = onNext,
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        // One frame, so the rows and buttons are laid out and can take focus.
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
}

/** "STEP 2 OF 7" in a small pill, like the eyebrow labels on the home screen's tiles. */
@Composable
private fun StepEyebrow(step: Int, total: Int) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .background(cs.primaryContainer.copy(alpha = 0.55f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(tr("Step $step of $total").uppercase(), style = EyebrowStyle, color = cs.onPrimaryContainer)
    }
}

/** One dot per step; the current one stretches into a pill, the ones done stay lit. */
@Composable
private fun ProgressDots(current: Int, total: Int) {
    val cs = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val width by animateDpAsState(if (i == current) 24.dp else 7.dp, tween(500, easing = FluidEasing), label = "dot")
            Box(
                Modifier
                    .size(width, 7.dp)
                    .background(if (i <= current) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.3f), CircleShape),
            )
        }
    }
}

/**
 * The primary pill with its arrow in a circle of its own, flush with the
 * right edge; the arrow leans forward when the button has focus.
 */
@Composable
private fun ArrowButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    FocusSurface(
        onClick = onClick,
        shape = CircleShape,
        container = cs.primary,
        contentColor = cs.onPrimary,
        modifier = modifier.height(48.dp),
    ) { focused ->
        val lean by animateFloatAsState(if (focused) 1f else 0f, tween(400, easing = FluidEasing), label = "lean")
        Row(
            Modifier.align(Alignment.Center).padding(start = 24.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(14.dp))
            Box(
                Modifier
                    .size(36.dp)
                    .graphicsLayer { scaleX = 1f + 0.06f * lean; scaleY = 1f + 0.06f * lean }
                    .background(cs.onPrimary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(Symbols.Arrow), contentDescription = null,
                    modifier = Modifier.size(18.dp).graphicsLayer { translationX = 3.dp.toPx() * lean },
                )
            }
        }
    }
}
