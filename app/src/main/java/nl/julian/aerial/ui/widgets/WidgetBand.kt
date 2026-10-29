package nl.julian.aerial.ui.widgets

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import nl.julian.aerial.data.ClockFace
import nl.julian.aerial.data.networkFlow
import nl.julian.aerial.data.lowStorageFlow
import nl.julian.aerial.data.bluetoothOnFlow
import nl.julian.aerial.data.NetKind
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.Immutable
import android.content.Intent
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import nl.julian.aerial.data.ClockFormat
import nl.julian.aerial.data.NowPlaying
import nl.julian.aerial.data.Weather
import nl.julian.aerial.theme.ClockStyle
import nl.julian.aerial.theme.EyebrowStyle
import nl.julian.aerial.theme.FluidEasing
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.PANEL_RADIUS
import nl.julian.aerial.theme.panel
import nl.julian.aerial.theme.topHighlight
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.ui.FocusSurface
import nl.julian.aerial.wallpaper.Aerial
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Height of the top bar: compact, so the aerial stays the main thing on screen. */
private val BAR_HEIGHT = 140.dp
private val BAR_RADIUS = PANEL_RADIUS
private val BAR_INSET = 12.dp
internal val INNER = BAR_RADIUS - BAR_INSET
/** Every tile in the bar shares this inner padding. */
internal val TILE_PADDING = 16.dp

/** Tile background: the theme's container, letting a little aerial through. */
@Composable
internal fun tileColor() = LocalSurfaces.current.high.copy(alpha = 0.88f)

/**
 * The top bar: one translucent bar with the clock, the weather and a
 * toolbar with inputs, notifications and settings. Optionally, between
 * them: what's playing ([showMusic]) or the aerial on screen ([showAerial]).
 */
@Composable
fun WidgetBand(
    clockFormat: ClockFormat,
    clockFace: ClockFace = ClockFace.Standard,
    media: NowPlaying?,
    clip: Aerial?,
    weather: Weather?,
    showWeather: Boolean,
    showAerial: Boolean,
    showMusic: Boolean,
    compactButtons: Boolean,
    notificationCount: Int,
    onOpenInputs: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    /** Shows a search button first in the rail. */
    onSearch: (() -> Unit)? = null,
    /** Makes the weather tile selectable, opening the forecast. */
    onWeather: (() -> Unit)? = null,
    status: StatusIcons = StatusIcons(),
    /** Focus targets for the toolbar buttons, so focus can return there when their panel closes. */
    railFocus: RailFocus = RailFocus(),
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .panel(RoundedCornerShape(BAR_RADIUS))
            .padding(BAR_INSET),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val playing = if (showMusic) media else null
        val middle = playing != null || showAerial
        // While music shows the weather narrows (dropping the place) so the
        // Now Playing card gets the room. With nothing in the middle the
        // clock and weather split the free space equally.
        val compact = playing != null
        val weatherWidth by animateDpAsState(if (compact) 132.dp else 196.dp, tween(500, easing = FluidEasing), label = "weatherW")
        ClockTile(clockFormat, clockFace, Modifier.fillMaxHeight().then(if (middle) Modifier.width(176.dp) else Modifier.weight(1f)), status)
        when {
            playing != null -> NowPlayingCard(playing, INNER, Modifier.weight(1f).fillMaxHeight())
            showAerial -> NowShowingTile(clip, Modifier.weight(1f).fillMaxHeight())
        }
        weather?.takeIf { showWeather }?.let {
            val size = if (middle) Modifier.width(weatherWidth) else Modifier.weight(1f)
            WeatherTile(it, INNER, Modifier.fillMaxHeight().then(size), compact, wide = !middle, onClick = onWeather)
        }
        ControlRail(notificationCount, onSearch, onOpenInputs, onOpenNotifications, onOpenSettings, railFocus, stacked = compactButtons)
    }
}

/**
 * Clock, Material 3 Expressive: hours and minutes each in their own filled
 * pill (primary and tertiary containers, so they follow the aerial's
 * colours), with the date underneath.
 */
@Composable
private fun ClockTile(format: ClockFormat, face: ClockFace, modifier: Modifier, status: StatusIcons = StatusIcons()) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    // Ticks exactly on the minute boundary, not every second.
    val now by produceState(Date()) {
        while (true) {
            value = Date()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val h24 = format.is24Hour(context)
    val hours = SimpleDateFormat(if (h24) "HH" else "h", Locale.getDefault()).format(now)
    val minutes = SimpleDateFormat("mm", Locale.getDefault()).format(now)

    Column(
        modifier
            .background(tileColor(), RoundedCornerShape(INNER))
            .topHighlight(RoundedCornerShape(INNER))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        when (face) {
            ClockFace.Standard -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DigitPill(hours, cs.primaryContainer, cs.onPrimaryContainer)
                DigitPill(minutes, cs.tertiaryContainer, cs.onTertiaryContainer)
            }
            ClockFace.Thin -> Text(
                "$hours:$minutes", maxLines = 1, color = cs.onSurface,
                style = ClockStyle.copy(fontWeight = FontWeight(250), fontSize = 54.sp, lineHeight = 58.sp),
            )
            ClockFace.Analog -> AnalogClock(now, 64.dp, cs.onSurface, cs.primary, cs.onSurfaceVariant.copy(alpha = 0.5f))
            ClockFace.Words -> Text(
                timeInWords(now), maxLines = 2, color = cs.onSurface, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(now),
                style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant, maxLines = 1,
            )
            StatusRow(status)
        }
    }
}

/** Which status icons the top bar shows. */
@Immutable
data class StatusIcons(val network: Boolean = false, val bluetooth: Boolean = false, val storage: Boolean = false)

@Composable
private fun StatusRow(show: StatusIcons) {
    if (!show.network && !show.bluetooth && !show.storage) return
    val app = LocalContext.current.applicationContext
    val net by remember { networkFlow(app) }.collectAsStateWithLifecycle(NetKind.Other)
    val bt by remember { bluetoothOnFlow(app) }.collectAsStateWithLifecycle(null)
    val low by remember { lowStorageFlow(app) }.collectAsStateWithLifecycle(false)
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val icon: @Composable (Int, String, Color) -> Unit = { res, label, color ->
        Icon(painterResource(res), contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
    }
    if (show.network) when (net) {
        NetKind.Ethernet -> icon(Symbols.Ethernet, "Ethernet", tint)
        NetKind.WiFi, NetKind.Other -> icon(Symbols.Wifi, "Wi-Fi", tint)
        NetKind.None -> icon(Symbols.Offline, "Offline", MaterialTheme.colorScheme.error)
    }
    if (show.bluetooth && bt == true) icon(Symbols.Bluetooth, "Bluetooth on", tint)
    if (show.storage && low) icon(Symbols.Storage, "Storage almost full", MaterialTheme.colorScheme.error)
}

@Composable
private fun DigitPill(text: String, container: Color, content: Color) {
    Box(
        Modifier
            .height(60.dp)
            .widthIn(min = 64.dp)
            .background(container, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = ClockStyle, color = content, maxLines = 1)
    }
}

/** The aerial on screen: its scenery as an eyebrow over its name. Display only. */
@Composable
private fun NowShowingTile(clip: Aerial?, modifier: Modifier) {
    Column(
        modifier
            .background(tileColor(), RoundedCornerShape(INNER))
            .topHighlight(RoundedCornerShape(INNER))
            .padding(horizontal = TILE_PADDING + 4.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            clip?.let { sceneryLabel(it.kind) }?.uppercase() ?: "NOW SHOWING",
            style = EyebrowStyle, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            clip?.title ?: "Aerials", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun sceneryLabel(kind: String) = when (kind) {
    "city" -> "City"
    "nature" -> "Nature"
    "water" -> "Underwater"
    "space" -> "From space"
    else -> "Ambient"
}

/**
 * Inputs, notifications and settings as one segmented control: a single
 * tile split into three equal parts by hairline separators, each an icon
 * with a small label. The outer corners follow the tile; on focus only
 * that part inverts.
 */
@Composable
private fun ControlRail(
    notifications: Int,
    onSearch: (() -> Unit)?,
    onInputs: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    focus: RailFocus,
    /** Space saver: a narrow column of icons instead of a labelled row. */
    stacked: Boolean,
) {
    val tile = RoundedCornerShape(INNER)
    // Inner corners stay small; outer ones match the tile (inset by the 4dp padding).
    val outer = INNER - 4.dp
    val inner = 8.dp
    val first = if (stacked) RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
    else RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner)
    val last = if (stacked) RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
    else RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer)
    val container = Modifier
        .fillMaxHeight()
        .background(tileColor(), tile)
        .topHighlight(tile)
        .padding(4.dp)
    val segments: @Composable (Modifier) -> Unit = { size ->
        if (onSearch != null) {
            RailSegment(Symbols.Search, "Search", onSearch, focus.search, first, size, stacked)
            SegmentDivider(stacked)
        }
        RailSegment(Symbols.Input, "Inputs", onInputs, focus.inputs, if (onSearch != null) RoundedCornerShape(inner) else first, size, stacked)
        SegmentDivider(stacked)
        RailSegment(Symbols.Notifications, "Alerts", onNotifications, focus.notifications, RoundedCornerShape(inner), size, stacked, notifications)
        SegmentDivider(stacked)
        RailSegment(Symbols.Settings, "Settings", onSettings, focus.settings, last, size, stacked)
    }
    if (stacked) {
        Column(container.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            segments(Modifier.fillMaxWidth().weight(1f))
        }
    } else {
        Row(container, verticalAlignment = Alignment.CenterVertically) {
            segments(Modifier.width(68.dp).fillMaxHeight())
        }
    }
}

/** Hairline between two segments, like the edge of the glass panels. */
@Composable
private fun SegmentDivider(horizontal: Boolean) {
    val line = Color.White.copy(alpha = 0.10f)
    if (horizontal) Box(Modifier.height(1.dp).fillMaxWidth(0.5f).background(line))
    else Box(Modifier.width(1.dp).fillMaxHeight(0.5f).background(line))
}

class RailFocus(
    val search: FocusRequester = FocusRequester(),
    val inputs: FocusRequester = FocusRequester(),
    val notifications: FocusRequester = FocusRequester(),
    val settings: FocusRequester = FocusRequester(),
)

@Composable
private fun RailSegment(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    focus: FocusRequester,
    shape: Shape,
    size: Modifier,
    iconOnly: Boolean,
    badge: Int = 0,
) {
    FocusSurface(
        onClick = onClick,
        shape = shape,
        container = Color.Transparent,
        modifier = size.focusRequester(focus),
        focusedScale = 1.04f,
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box {
                Icon(
                    painterResource(icon), contentDescription = if (iconOnly) label else null,
                    modifier = Modifier.size(if (iconOnly) 22.dp else 26.dp),
                )
                if (badge > 0) Badge(badge, Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-6).dp))
            }
            if (!iconOnly) Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/** M3 badge: a small error-coloured pill with the count. */
@Composable
private fun Badge(count: Int, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier.height(18.dp).aspectRatio(if (count > 9) 1.5f else 1f, matchHeightConstraintsFirst = true)
            .background(cs.error, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 12.sp),
            color = cs.onError,
        )
    }
}

/** Whether to show a 24-hour clock: the user's choice, or the TV's own setting. */
internal fun ClockFormat.is24Hour(context: android.content.Context) = when (this) {
    ClockFormat.H24 -> true
    ClockFormat.H12 -> false
    ClockFormat.System -> DateFormat.is24HourFormat(context)
}

/**
 * Voice search: the Assistant, else the system's global search, else
 * whatever search the TV hands an activity (TVs differ in what they register).
 */
internal fun android.content.Context.startSearch() {
    val tries = listOf(Intent.ACTION_ASSIST, android.app.SearchManager.INTENT_ACTION_GLOBAL_SEARCH)
    if (tries.any { runCatching { startActivity(Intent(it).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess }) return
    (this as? android.app.Activity)?.startSearch(null, false, null, true)
}

internal fun android.content.Context.startSafely(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
