package nl.julian.aerial.ui

import nl.julian.aerial.data.sections
import nl.julian.aerial.data.HomeSection
import nl.julian.aerial.theme.LocalPanelAlpha
import nl.julian.aerial.data.ThemeSource
import nl.julian.aerial.data.ClockFace
import nl.julian.aerial.theme.LocalReveal
import kotlinx.coroutines.delay
import nl.julian.aerial.data.LaunchAnimation
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import nl.julian.aerial.data.Glass
import nl.julian.aerial.theme.LocalFrost
import androidx.compose.ui.graphics.ImageBitmap
import nl.julian.aerial.remote.RemoteButtonsService
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.julian.aerial.data.AmbientInfo
import nl.julian.aerial.data.AppEntry
import nl.julian.aerial.data.AppRepository
import nl.julian.aerial.data.COLUMNS
import nl.julian.aerial.data.ClockFormat
import nl.julian.aerial.data.ConfigStore
import nl.julian.aerial.data.LauncherConfig
import nl.julian.aerial.data.InputsRepository
import nl.julian.aerial.data.NotificationsRepository
import nl.julian.aerial.data.NowPlaying
import nl.julian.aerial.data.NowPlayingRepository
import nl.julian.aerial.data.Weather
import nl.julian.aerial.data.WeatherDetails
import nl.julian.aerial.data.Scenery
import nl.julian.aerial.data.WeatherRepository
import nl.julian.aerial.data.arrange
import nl.julian.aerial.data.moved
import nl.julian.aerial.data.pinned
import nl.julian.aerial.theme.AerialTheme
import nl.julian.aerial.theme.FluidEasing
import nl.julian.aerial.theme.SectionPanel
import nl.julian.aerial.theme.SECTION_TILE_RADIUS
import nl.julian.aerial.theme.reveal
import nl.julian.aerial.ui.menus.AppAction
import nl.julian.aerial.ui.menus.AppMenu
import nl.julian.aerial.ui.menus.ClipPanel
import nl.julian.aerial.ui.menus.InputsPanel
import nl.julian.aerial.ui.menus.WeatherPanel
import nl.julian.aerial.ui.menus.dockEntry
import nl.julian.aerial.ui.menus.dockKey
import nl.julian.aerial.ui.menus.NotificationsPanel
import nl.julian.aerial.ui.menus.SettingsPanel
import nl.julian.aerial.ui.widgets.RailFocus
import nl.julian.aerial.ui.widgets.StatusIcons
import nl.julian.aerial.ui.widgets.startSearch
import nl.julian.aerial.ui.widgets.WidgetBand
import nl.julian.aerial.ui.widgets.startSafely
import nl.julian.aerial.wallpaper.Aerial
import nl.julian.aerial.wallpaper.AerialBackground

/** TV overscan-safe side margin (Android TV guideline: 48dp). */
private val SCREEN_MARGIN = 48.dp
/** Open aerial between the top bar and the dock. */
private val DOCK_GAP = 24.dp

@Composable
fun HomeScreen(
    homeTick: Int,
    idle: Boolean,
    clipMenuTick: Int,
    inputsTick: Int,
    /** Black screen while sound keeps playing; any key turns the picture back on (MainActivity). */
    pictureOff: Boolean,
    onStartScreensaver: () -> Unit,
    onPictureOff: () -> Unit,
) {
    val context = LocalContext.current
    var rescanTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        AppRepository.packageChanges(context.applicationContext).collect { rescanTick++ }
    }
    val apps by produceState(AppRepository.memoryCache, rescanTick) {
        value = withContext(Dispatchers.Default) { AppRepository.scan(context.applicationContext) }
    }
    val store = remember { ConfigStore.get(context) }
    val config by store.config.collectAsStateWithLifecycle(null)

    val media by remember { NowPlayingRepository.nowPlaying(context.applicationContext) }.collectAsStateWithLifecycle(null)
    val weather by remember(config?.weatherCity, config?.tempUnit) {
        config?.let { WeatherRepository.weather(it.weatherCity, it.tempUnit) } ?: emptyFlow()
    }.collectAsStateWithLifecycle(null)

    var seed by remember { mutableStateOf<Int?>(null) }
    var clip by remember { mutableStateOf<Aerial?>(null) }
    val themeSeed = if (config?.themeSource == ThemeSource.Fixed) config?.themeColor else seed
    val frost = remember { mutableStateOf<ImageBitmap?>(null) }
    // The staggered entrance plays on cold start and when waking from the
    // screensaver, not on every Home press.
    var revealTick by remember { mutableIntStateOf(0) }
    var wasIdle by remember { mutableStateOf(false) }
    LaunchedEffect(idle) {
        if (wasIdle && !idle) revealTick++
        wasIdle = idle
    }
    // Idle: everything but the aerial fades away (slowly out, quickly back).
    val uiAlpha by animateFloatAsState(
        if (idle) 0f else 1f,
        tween(if (idle) 1600 else 500, easing = FluidEasing),
        label = "ui",
    )
    Box(Modifier.fillMaxSize()) {
        // Wait for the saved settings, or the first clip ignores the chosen scenery.
        config?.let { cfg ->
        AerialBackground(
            config = cfg,
            isDay = weather?.isDay,
            onSeed = { seed = it },
            onClip = { clip = it },
            onFrame = { frost.value = it },
        )
        } ?: AmbientBackdrop()
        config?.let { Seasonal(it) }
        AerialTheme(themeSeed, (config?.textSize ?: 100) / 100f, config?.highContrast == true, config?.themeSource == ThemeSource.Mono) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = uiAlpha }) {
                Haze()
                Scrim()
                val a = apps
                val c = config
                if (a != null && c != null) CompositionLocalProvider(LocalFrost provides frost.takeIf { c.glass != Glass.Tinted }, LocalReveal provides c.reveal, LocalPanelAlpha provides c.panelOpacity / 100f) {
                    HomeContent(a, c, store, homeTick, revealTick, clipMenuTick, inputsTick, clip, media, weather, onStartScreensaver, onPictureOff)
                }
            }
        }
        AerialTheme(themeSeed, (config?.textSize ?: 100) / 100f, config?.highContrast == true, config?.themeSource == ThemeSource.Mono) {
            Ambient(
                clip = clip,
                weather = weather,
                media = media,
                clockFormat = config?.clockFormat ?: ClockFormat.System,
                visible = idle,
                info = config?.ambientInfo ?: AmbientInfo.All,
                showPlaces = config?.showPlaces == true,
                shiftText = config?.shiftText == true,
                face = config?.ambientClockFace ?: ClockFace.Standard,
            )
        }
        config?.let { NightDim(it, weather?.isDay) }
        if (pictureOff) Box(Modifier.fillMaxSize().background(Color.Black))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    apps: List<AppEntry>,
    config: LauncherConfig,
    store: ConfigStore,
    homeTick: Int,
    revealTick: Int,
    /** Bumped when OK is held during the screensaver: open the panel for the clip on screen. */
    clipMenuTick: Int,
    /** Bumped when the remote's Input button opens the inputs panel. */
    inputsTick: Int,
    clip: Aerial?,
    media: NowPlaying?,
    weather: Weather?,
    onStartScreensaver: () -> Unit,
    onPictureOff: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val dockFocus = remember { FocusRequester() }
    val inputModes = LocalInputModeManager.current
    // One requester per app, so focus can follow a tile while it's being moved.
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requester(pkg: String) = requesters.getOrPut(pkg) { FocusRequester() }

    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    var moving by remember { mutableStateOf<String?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    var inputsOpen by remember { mutableStateOf(false) }
    var notificationsOpen by remember { mutableStateOf(false) }
    var clipOpen by remember { mutableStateOf(false) }
    var weatherOpen by remember { mutableStateOf(false) }
    LaunchedEffect(clipMenuTick) { if (clipMenuTick > 0 && clip != null) clipOpen = true }
    val railFocus = remember { RailFocus() }
    /** Where focus goes back to when the open panel closes. */
    var returnFocus by remember { mutableStateOf<FocusRequester?>(null) }
    LaunchedEffect(inputsTick) { if (inputsTick > 0) { returnFocus = railFocus.inputs; inputsOpen = true } }
    LaunchedEffect(settingsOpen, inputsOpen, notificationsOpen, clipOpen) {
        if (!settingsOpen && !inputsOpen && !notificationsOpen && !clipOpen) {
            returnFocus?.let {
                withFrameNanos { }
                runCatching { it.requestFocus() }
            }
            returnFocus = null
        }
    }

    val app = context.applicationContext
    val inputs by remember { InputsRepository.inputs(app) }.collectAsStateWithLifecycle(emptyList())
    val notifications by NotificationsRepository.items.collectAsStateWithLifecycle()
    /** Set when the focused tile disappears (hidden/unpinned), so focus lands on the dock, not anywhere. */
    var refocusDock by remember { mutableStateOf(false) }

    // Inputs pinned to the dock join the apps as tiles of their own.
    val withInputs = remember(apps, inputs, config.dock) {
        apps + inputs.filter { config.dock.orEmpty().contains(it.dockKey) }.map { it.dockEntry(app) }
    }
    val home = remember(withInputs, config) { arrange(withInputs, config) }
    val gridRows = remember(home) { home.grid.chunked(COLUMNS) }
    fun update(change: (LauncherConfig) -> LauncherConfig) = scope.launch { store.update(change) }
    /** Where each tile is on screen, for the opening animation. Not state: nothing redraws when it changes. */
    val tileBounds = remember { mutableMapOf<String, Rect>() }
    var opening by remember { mutableStateOf<Pair<AppEntry, Rect>?>(null) }
    val launch: (AppEntry) -> Unit = { app ->
        val from = tileBounds[app.pkg]
        if (config.launchAnimation != LaunchAnimation.Off && from != null) opening = app to from
        else AppRepository.launchIntent(context, app.pkg)?.let { context.startSafely(it) }
        update {
            it.copy(
                launches = it.launches + (app.pkg to (it.launches[app.pkg] ?: 0) + 1),
                recent = (listOf(app.pkg) + it.recent - app.pkg).take(COLUMNS * 2),
            )
        }
    }

    /** Moves [pkg] within its list; computed from the latest stored config so fast presses don't race. */
    fun move(pkg: String, delta: Int) = update { cfg ->
        val now = arrange(withInputs, cfg)
        val dock = now.dock.map { it.pkg }
        if (pkg in dock) cfg.copy(dock = moved(dock, pkg, delta))
        else cfg.copy(order = moved(now.grid.map { it.pkg }, pkg, delta))
    }

    fun act(app: AppEntry, action: AppAction) {
        menuFor = null
        val pkg = app.pkg
        when (action) {
            AppAction.Open -> launch(app)
            AppAction.Move -> moving = pkg
            AppAction.Pin -> update { cfg -> cfg.copy(dock = pinned(arrange(withInputs, cfg).dock.map { it.pkg }, pkg)) }
            AppAction.Unpin -> {
                refocusDock = true
                update { cfg -> cfg.copy(dock = arrange(withInputs, cfg).dock.map { it.pkg } - pkg) }
            }
            AppAction.Hide -> {
                refocusDock = true
                update { cfg -> cfg.copy(hidden = cfg.hidden + pkg, dock = cfg.dock?.minus(pkg)) }
            }
            AppAction.Info -> context.startSafely(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
            )
            AppAction.Uninstall -> context.startSafely(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
        }
    }

    // First show and every Home press: back to the top, focus on the dock.
    LaunchedEffect(homeTick) {
        moving = null
        listState.scrollToItem(0)
        // Wait one frame so the dock's tiles are laid out and focusable.
        withFrameNanos { }
        // A TV is never in touch mode, but the window can start out in it,
        // which makes clickable tiles unfocusable until the first key press.
        inputModes.requestInputMode(InputMode.Keyboard)
        runCatching { dockFocus.requestFocus() }
    }
    // A moved tile may land in another row (a new composable): keep focus on it.
    LaunchedEffect(home) {
        if (refocusDock) {
            refocusDock = false
            withFrameNanos { }
            runCatching { dockFocus.requestFocus() }
            listState.animateScrollToItem(0)
        }
        val pkg = moving ?: return@LaunchedEffect
        withFrameNanos { }
        runCatching { requester(pkg).requestFocus() }
    }

    val tile: @Composable (AppEntry, Modifier, Boolean) -> Unit = { app, modifier, inDock ->
        val isMoving = moving == app.pkg
        AppTile(
            app,
            radius = SECTION_TILE_RADIUS,
            onLaunch = { launch(app) },
            tinted = config.tintedTiles,
            onLongPress = { menuFor = app },
            moving = isMoving,
            depth = config.tileDepth.degrees,
            focusColor = config.focusColor,
            modifier = modifier
                .onGloballyPositioned { tileBounds[app.pkg] = it.boundsInRoot() }
                .focusRequester(requester(app.pkg))
                .then(if (isMoving) Modifier.moveKeys(inDock, onMove = { move(app.pkg, it) }, onDone = { moving = null }) else Modifier),
        )
    }

    // Compose's TV default pivots the focused row to ~30% from the top, which
    // pushes the widget band off-screen as soon as the dock is focused.
    // Only scroll when the focused item (plus its glow) would be cut off.
    val minimalScroll = rememberMinimalScroll(margin = 24.dp)
    val scrollTopOnFocus = Modifier.onFocusChanged {
        if (it.hasFocus && listState.firstVisibleItemIndex + listState.firstVisibleItemScrollOffset > 0) {
            scope.launch { listState.animateScrollToItem(0) }
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides minimalScroll) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = SCREEN_MARGIN, end = SCREEN_MARGIN, top = 27.dp, bottom = 27.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // The bar and the dock sit at the very top. When focus moves into
        // either, scroll all the way up so the bar and the open aerial gap
        // above the dock are in view, not just the focused tile.
        item(key = "widgets") {
            WidgetBand(
                clockFormat = config.clockFormat,
                clockFace = config.clockFace,
                media = media,
                clip = clip,
                weather = weather,
                showWeather = config.barShowsWeather,
                showAerial = config.barShowsAerial,
                showMusic = config.barShowsMusic,
                compactButtons = config.compactButtons,
                notificationCount = notifications.size,
                onOpenInputs = { returnFocus = railFocus.inputs; inputsOpen = true },
                onOpenNotifications = { returnFocus = railFocus.notifications; notificationsOpen = true },
                onOpenSettings = { returnFocus = railFocus.settings; settingsOpen = true },
                modifier = Modifier.reveal(0, revealTick).then(scrollTopOnFocus),
                onSearch = if (config.barShowsSearch) { { context.startSearch() } } else null,
                status = StatusIcons(config.statusNetwork, config.statusBluetooth, config.statusStorage),
                onWeather = if (config.weatherDetails != WeatherDetails.Off) { { weatherOpen = true } } else null,
                railFocus = railFocus,
            )
        }
        // Every section sits on the same translucent panel as the top bar.
        // Open space above the dock keeps the aerial the main thing on screen.
        config.sections.forEachIndexed { index, section -> when (section) {
        HomeSection.Dock -> item(key = "dock") {
            SectionPanel(Modifier.then(scrollTopOnFocus).padding(top = if (index == 0) DOCK_GAP else 0.dp).reveal(index + 1, revealTick)) {
                TileRow(home.dock, firstFocus = dockFocus, wrap = moving == null) { app, m -> tile(app, m, true) }
            }
        }
        HomeSection.WatchNext -> item(key = "watch-next") {
            WatchNextRow(radius = SECTION_TILE_RADIUS, modifier = Modifier.reveal(index + 1, revealTick))
        }
        HomeSection.Recent -> {
            val byPkg = withInputs.associateBy { it.pkg }
            val recent = config.recent.filter { pkg -> home.dock.none { it.pkg == pkg } && pkg !in config.hidden }
                .mapNotNull { byPkg[it] }.take(COLUMNS)
            if (recent.isNotEmpty()) item(key = "recent") {
                SectionPanel(Modifier.reveal(index + 1, revealTick), title = "Recent") {
                    TileRow(recent) { app, m ->
                        AppTile(
                            app, radius = SECTION_TILE_RADIUS, onLaunch = { launch(app) }, tinted = config.tintedTiles,
                            onLongPress = { menuFor = app }, moving = false,
                            depth = config.tileDepth.degrees, focusColor = config.focusColor,
                            modifier = m.onGloballyPositioned { tileBounds[app.pkg] = it.boundsInRoot() },
                        )
                    }
                }
            }
        }
        HomeSection.Apps -> if (gridRows.isNotEmpty()) {
            item(key = "apps") {
                SectionPanel(Modifier.reveal(index + 1, revealTick), title = "All apps") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        gridRows.forEach { row -> TileRow(row, restoreFocus = false) { app, m -> tile(app, m, false) } }
                    }
                }
            }
        }
        HomeSection.Quick -> item(key = "quick") {
            QuickSettings(
                scenery = config.scenery,
                onCycleScenery = {
                    update {
                        val all = Scenery.entries
                        it.copy(scenery = all[(all.indexOf(it.scenery) + 1) % all.size])
                    }
                },
                onStartScreensaver = onStartScreensaver,
                sleepAt = config.sleepAt,
                onCycleSleep = {
                    if (!RemoteButtonsService.isEnabled(context)) {
                        context.startSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } else update {
                        val left = ((it.sleepAt - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
                        val next = SLEEP_STEPS.firstOrNull { m -> m > left + 1 } ?: 0
                        it.copy(sleepAt = if (next == 0) 0 else System.currentTimeMillis() + next * 60_000L)
                    }
                },
                onPictureOff = onPictureOff,
                modifier = Modifier.reveal(index + 1, revealTick),
            )
        }
        } }
    }

    }

    menuFor?.let { app ->
        AppMenu(
            app = app,
            inDock = home.dock.any { it.pkg == app.pkg },
            onAction = { act(app, it) },
            onDismiss = { menuFor = null },
        )
    }
    if (settingsOpen) SettingsPanel(config, onDismiss = { settingsOpen = false })
    if (weatherOpen) weather?.let { WeatherPanel(it, config.weatherDetails, onDismiss = { weatherOpen = false }) }
    if (clipOpen && clip != null) ClipPanel(
        clip,
        favourite = clip.uri in config.favourites,
        onFavourite = {
            update { it.copy(favourites = if (clip.uri in it.favourites) it.favourites - clip.uri else it.favourites + clip.uri) }
            clipOpen = false
        },
        onSkip = {
            update { it.copy(skipped = it.skipped + clip.uri, favourites = it.favourites - clip.uri) }
            clipOpen = false
        },
        onDismiss = { clipOpen = false },
    )
    opening?.let { (app, from) ->
        LaunchZoom(app, from, config.launchAnimation.ms) {
            AppRepository.launchIntent(context, app.pkg)?.let { context.startSafely(it) }
            scope.launch {
                // Keep covering the home screen until the app is in front.
                delay(800)
                opening = null
            }
        }
    }
    if (inputsOpen) InputsPanel(
        inputs,
        docked = home.dock.map { it.pkg }.toSet(),
        onTogglePin = { input ->
            update { cfg ->
                val dock = arrange(withInputs, cfg).dock.map { it.pkg }
                cfg.copy(dock = if (input.dockKey in dock) dock - input.dockKey else pinned(dock, input.dockKey))
            }
        },
        onDismiss = { inputsOpen = false },
    )
    if (notificationsOpen) {
        NotificationsPanel(
            notifications,
            hasAccess = remember { NowPlayingRepository.hasAccess(context) },
            onDismiss = { notificationsOpen = false },
        )
    }
}

/** Scrolls only as far as needed to show the focused item with [margin] around it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberMinimalScroll(margin: Dp): BringIntoViewSpec {
    val px = with(LocalDensity.current) { margin.toPx() }
    return remember(px) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                val start = offset - px
                val end = offset + size + px
                return when {
                    start >= 0f && end <= containerSize -> 0f
                    start < 0f || size + 2 * px > containerSize -> start
                    else -> end - containerSize
                }
            }
        }
    }
}

/**
 * While a tile is being moved the D-pad moves it instead of focus: left/right
 * by one, up/down by a row (grid only). OK or Back drops it.
 */
private fun Modifier.moveKeys(inDock: Boolean, onMove: (Int) -> Unit, onDone: () -> Unit) =
    onPreviewKeyEvent { e ->
        val delta = when (e.key) {
            Key.DirectionLeft -> -1
            Key.DirectionRight -> 1
            Key.DirectionUp -> if (inDock) 0 else -COLUMNS
            Key.DirectionDown -> if (inDock) 0 else COLUMNS
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Back -> {
                if (e.type == KeyEventType.KeyDown) onDone()
                return@onPreviewKeyEvent true
            }
            else -> return@onPreviewKeyEvent false
        }
        if (e.type == KeyEventType.KeyDown && delta != 0) onMove(delta)
        true
    }

@Composable
private fun TileRow(
    apps: List<AppEntry>,
    modifier: Modifier = Modifier,
    firstFocus: FocusRequester? = null,
    /** Right on the last tile jumps back to the first (needs [firstFocus]). */
    wrap: Boolean = false,
    /**
     * Re-entering the row returns to the tile focused last. Right for the
     * dock (coming back from the top bar), wrong between rows of a grid:
     * there Down must go straight down, not back to an earlier column.
     */
    restoreFocus: Boolean = true,
    tile: @Composable (AppEntry, Modifier) -> Unit,
) {
    Row(
        modifier.fillMaxWidth().then(if (restoreFocus) Modifier.focusRestorer() else Modifier).focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        apps.forEachIndexed { i, app ->
            // Keyed by package so a moved tile keeps its composable (and focus).
            key(app.pkg) {
                val m = when {
                    i == 0 && firstFocus != null -> Modifier.focusRequester(firstFocus)
                    i == apps.lastIndex && wrap && firstFocus != null -> Modifier.onPreviewKeyEvent { e ->
                        if (e.key != Key.DirectionRight) return@onPreviewKeyEvent false
                        if (e.type == KeyEventType.KeyDown) runCatching { firstFocus.requestFocus() }
                        true
                    }
                    else -> Modifier
                }
                tile(app, m.weight(1f))
            }
        }
        repeat(COLUMNS - apps.size) { EmptyTileSlot(Modifier.weight(1f)) }
    }
}

/** Sleep timer steps, in minutes. */
private val SLEEP_STEPS = listOf(30, 60, 90)
