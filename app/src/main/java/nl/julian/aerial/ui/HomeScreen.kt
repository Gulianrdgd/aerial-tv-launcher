package nl.julian.aerial.ui

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.julian.aerial.data.AppEntry
import nl.julian.aerial.data.AppRepository
import nl.julian.aerial.theme.AerialType
import nl.julian.aerial.theme.Bezel
import nl.julian.aerial.theme.DynamicTheme
import nl.julian.aerial.theme.LocalLauncherColors
import nl.julian.aerial.theme.reveal
import nl.julian.aerial.ui.widgets.WidgetBand
import nl.julian.aerial.wallpaper.AerialBackground
import nl.julian.aerial.ui.widgets.startSafely

private const val COLUMNS = 5
/** TV overscan-safe side margin. */
private val SCREEN_MARGIN = 56.dp
private val DOCK_OUTER = 40.dp
private val DOCK_INSET = 12.dp

/** Apps that land in the dock until the user pins their own. */
private val DEFAULT_DOCK = listOf(
    "com.google.android.youtube.tv",
    "com.netflix.ninja",
    "com.disney.disneyplus",
    "com.amazon.amazonvideo.livingroom",
    "com.spotify.tv.android",
    "org.jellyfin.androidtv",
    "com.plexapp.android",
    "com.android.vending",
)

@Composable
fun HomeScreen(homeTick: Int) {
    val context = LocalContext.current
    var rescanTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        AppRepository.packageChanges(context.applicationContext).collect { rescanTick++ }
    }
    val apps by produceState(AppRepository.memoryCache, rescanTick) {
        value = withContext(Dispatchers.Default) { AppRepository.scan(context.applicationContext) }
    }

    var seed by remember { mutableStateOf<Int?>(null) }
    Box(Modifier.fillMaxSize()) {
        AerialBackground(onSeed = { seed = it })
        Scrim()
        DynamicTheme(seed) {
            apps?.let { HomeContent(it, homeTick) }
        }
    }
}

@Composable
private fun HomeContent(apps: List<AppEntry>, homeTick: Int) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val dockFocus = remember { FocusRequester() }
    val inputModes = LocalInputModeManager.current

    val (dock, rest) = remember(apps) {
        val byPkg = apps.associateBy { it.pkg }
        val preferred = DEFAULT_DOCK.mapNotNull { byPkg[it] }
        val dock = (preferred + apps.filter { it !in preferred }).take(COLUMNS)
        dock to apps.filter { it !in dock }
    }
    val gridRows = remember(rest) { rest.chunked(COLUMNS) }
    val launch: (AppEntry) -> Unit = { app ->
        AppRepository.launchIntent(context, app.pkg)?.let { context.startSafely(it) }
    }

    // First show and every Home press: back to the top, focus on the dock.
    LaunchedEffect(homeTick) {
        listState.scrollToItem(0)
        // Wait one frame so the dock's tiles are laid out and focusable.
        withFrameNanos { }
        // A TV is never in touch mode, but the window can start out in it,
        // which makes clickable tiles unfocusable until the first key press.
        inputModes.requestInputMode(InputMode.Keyboard)
        runCatching { dockFocus.requestFocus() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        // TV overscan-safe margins, then generous breathing room.
        contentPadding = PaddingValues(start = SCREEN_MARGIN, end = SCREEN_MARGIN, top = 32.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item(key = "widgets") {
            WidgetBand(Modifier.reveal(0, homeTick))
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
                TileRow(
                    apps = dock,
                    radius = DOCK_OUTER - DOCK_INSET,
                    onLaunch = launch,
                    firstFocus = dockFocus,
                )
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
        itemsIndexed(gridRows, key = { _, row -> row.first().pkg }) { i, row ->
            TileRow(
                apps = row,
                radius = DOCK_OUTER - DOCK_INSET,
                onLaunch = launch,
                // Line up with the dock's inner tiles.
                modifier = Modifier.padding(horizontal = DOCK_INSET).reveal(4 + i, homeTick),
            )
        }
    }
}

@Composable
private fun TileRow(
    apps: List<AppEntry>,
    radius: androidx.compose.ui.unit.Dp,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    firstFocus: FocusRequester? = null,
) {
    Row(
        modifier.fillMaxWidth().focusRestorer().focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        apps.forEachIndexed { i, app ->
            val m = if (i == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier
            AppTile(app, radius, onLaunch = { onLaunch(app) }, modifier = m.weight(1f))
        }
        repeat(COLUMNS - apps.size) { EmptyTileSlot(Modifier.weight(1f)) }
    }
}
