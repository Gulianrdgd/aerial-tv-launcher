package nl.julian.aerial.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.julian.aerial.data.AppEntry
import nl.julian.aerial.data.AppRepository
import nl.julian.aerial.data.COLUMNS
import nl.julian.aerial.data.ConfigStore
import nl.julian.aerial.data.LauncherConfig
import nl.julian.aerial.data.arrange
import nl.julian.aerial.data.moved
import nl.julian.aerial.data.pinned
import nl.julian.aerial.theme.AerialType
import nl.julian.aerial.theme.Bezel
import nl.julian.aerial.theme.DynamicTheme
import nl.julian.aerial.theme.FluidEasing
import nl.julian.aerial.theme.LocalLauncherColors
import nl.julian.aerial.theme.reveal
import nl.julian.aerial.ui.menus.AppAction
import nl.julian.aerial.ui.menus.AppMenu
import nl.julian.aerial.ui.menus.SettingsPanel
import nl.julian.aerial.ui.widgets.WidgetBand
import nl.julian.aerial.ui.widgets.startSafely
import nl.julian.aerial.wallpaper.AerialBackground

/** TV overscan-safe side margin (Android TV guideline: 48dp). */
private val SCREEN_MARGIN = 48.dp
private val DOCK_OUTER = 40.dp
private val DOCK_INSET = 12.dp

@Composable
fun HomeScreen(homeTick: Int, idle: Boolean) {
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

    var seed by remember { mutableStateOf<Int?>(null) }
    var clipTitle by remember { mutableStateOf<String?>(null) }
    // Idle: everything but the aerial fades away (slowly out, quickly back).
    val uiAlpha by animateFloatAsState(
        if (idle) 0f else 1f,
        tween(if (idle) 1600 else 500, easing = FluidEasing),
        label = "ui",
    )
    Box(Modifier.fillMaxSize()) {
        AerialBackground(
            sources = config?.aerialSource?.keys.orEmpty(),
            kind = config?.scenery?.kind,
            onSeed = { seed = it },
            onClip = { clipTitle = it.title },
        )
        DynamicTheme(seed) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = uiAlpha }) {
                Haze()
                Scrim()
                val a = apps
                val c = config
                if (a != null && c != null) HomeContent(a, c, store, homeTick)
            }
        }
        AerialCaption(clipTitle, visible = idle)
    }
}

@Composable
private fun HomeContent(apps: List<AppEntry>, config: LauncherConfig, store: ConfigStore, homeTick: Int) {
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
    /** Set when the focused tile disappears (hidden/unpinned), so focus lands on the dock, not anywhere. */
    var refocusDock by remember { mutableStateOf(false) }

    val home = remember(apps, config) { arrange(apps, config) }
    val gridRows = remember(home) { home.grid.chunked(COLUMNS) }
    val launch: (AppEntry) -> Unit = { app ->
        AppRepository.launchIntent(context, app.pkg)?.let { context.startSafely(it) }
    }
    fun update(change: (LauncherConfig) -> LauncherConfig) = scope.launch { store.update(change) }

    /** Moves [pkg] within its list; computed from the latest stored config so fast presses don't race. */
    fun move(pkg: String, delta: Int) = update { cfg ->
        val now = arrange(apps, cfg)
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
            AppAction.Pin -> update { cfg -> cfg.copy(dock = pinned(arrange(apps, cfg).dock.map { it.pkg }, pkg)) }
            AppAction.Unpin -> {
                refocusDock = true
                update { cfg -> cfg.copy(dock = arrange(apps, cfg).dock.map { it.pkg } - pkg) }
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
            radius = DOCK_OUTER - DOCK_INSET,
            onLaunch = { launch(app) },
            onLongPress = { menuFor = app },
            moving = isMoving,
            modifier = modifier
                .focusRequester(requester(app.pkg))
                .then(if (isMoving) Modifier.moveKeys(inDock, onMove = { move(app.pkg, it) }, onDone = { moving = null }) else Modifier),
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = SCREEN_MARGIN, end = SCREEN_MARGIN, top = 27.dp, bottom = 27.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item(key = "widgets") {
            WidgetBand(onOpenSettings = { settingsOpen = true }, modifier = Modifier.reveal(0, homeTick))
        }
        item(key = "watch-next") {
            WatchNextRow(
                radius = DOCK_OUTER - DOCK_INSET,
                // Inset to the dock's inner edge so all rows share one column grid.
                modifier = Modifier.padding(horizontal = DOCK_INSET).reveal(1, homeTick),
                bleed = SCREEN_MARGIN + DOCK_INSET,
            )
        }
        item(key = "dock") {
            Bezel(
                Modifier.fillMaxWidth().reveal(2, homeTick),
                outerRadius = DOCK_OUTER,
                inset = DOCK_INSET,
            ) {
                TileRow(home.dock, firstFocus = dockFocus) { app, m -> tile(app, m, true) }
            }
        }
        if (gridRows.isNotEmpty()) {
            item(key = "apps-label") {
                Text(
                    "ALL APPS",
                    style = AerialType.sectionLabel,
                    color = LocalLauncherColors.current.contentMuted,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp).reveal(3, homeTick),
                )
            }
        }
        itemsIndexed(gridRows, key = { i, _ -> "row-$i" }) { i, row ->
            // Line up with the dock's inner tiles.
            TileRow(row, Modifier.padding(horizontal = DOCK_INSET).reveal(4 + i, homeTick)) { app, m ->
                tile(app, m, false)
            }
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
    tile: @Composable (AppEntry, Modifier) -> Unit,
) {
    Row(
        modifier.fillMaxWidth().focusRestorer().focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        apps.forEachIndexed { i, app ->
            // Keyed by package so a moved tile keeps its composable (and focus).
            key(app.pkg) {
                val m = if (i == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier
                tile(app, m.weight(1f))
            }
        }
        repeat(COLUMNS - apps.size) { EmptyTileSlot(Modifier.weight(1f)) }
    }
}
