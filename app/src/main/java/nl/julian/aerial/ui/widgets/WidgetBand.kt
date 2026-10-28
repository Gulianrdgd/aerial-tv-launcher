package nl.julian.aerial.ui.widgets

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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
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
 * The top bar: one compact translucent bar holding the tiles: clock, Now Playing (or, when nothing plays, the aerial on screen),
 * weather, and a toolbar with inputs, notifications and settings.
 */
@Composable
fun WidgetBand(
    clockFormat: ClockFormat,
    media: NowPlaying?,
    clip: Aerial?,
    weather: Weather?,
    notificationCount: Int,
    onSkipAerial: () -> Unit,
    onOpenInputs: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
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
        // While something plays the weather narrows (dropping the place) so
        // the Now Playing card gets the room.
        val compact = media != null
        val weatherWidth by animateDpAsState(if (compact) 132.dp else 196.dp, tween(500, easing = FluidEasing), label = "weatherW")
        ClockTile(clockFormat, Modifier.fillMaxHeight().width(176.dp))
        when {
            media != null -> NowPlayingCard(media, INNER, Modifier.weight(1f).fillMaxHeight())
            else -> NowShowingTile(clip, onSkipAerial, Modifier.weight(1f).fillMaxHeight())
        }
        weather?.let { WeatherTile(it, INNER, Modifier.fillMaxHeight().width(weatherWidth), compact) }
        ControlRail(notificationCount, onOpenInputs, onOpenNotifications, onOpenSettings, railFocus)
    }
}

/**
 * Clock, Material 3 Expressive: hours and minutes each in their own filled
 * pill (primary and tertiary containers, so they follow the aerial's
 * colours), with the date underneath.
 */
@Composable
private fun ClockTile(format: ClockFormat, modifier: Modifier) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DigitPill(hours, cs.primaryContainer, cs.onPrimaryContainer)
            DigitPill(minutes, cs.tertiaryContainer, cs.onTertiaryContainer)
        }
        Text(
            SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(now),
            style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant, maxLines = 1,
        )
    }
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

/**
 * When nothing is playing: the aerial's place, Apple TV style. Selecting it
 * skips to the next clip.
 */
@Composable
private fun NowShowingTile(clip: Aerial?, onSkip: () -> Unit, modifier: Modifier) {
    FocusSurface(
        onClick = onSkip,
        shape = RoundedCornerShape(INNER),
        container = tileColor(),
        modifier = modifier,
        focusedScale = 1.03f,
    ) { focused ->
        Row(Modifier.fillMaxSize().padding(start = TILE_PADDING + 4.dp, end = TILE_PADDING), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                // The scenery goes in the eyebrow, so the title has two lines
                // instead of being cut off.
                Text(
                    clip?.let { sceneryLabel(it.kind) }?.uppercase() ?: "NOW SHOWING",
                    style = EyebrowStyle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                )
                Text(
                    clip?.title ?: "Aerials", style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .background(if (focused) MaterialTheme.colorScheme.primary else LocalSurfaces.current.highest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(Symbols.SkipNext), contentDescription = "Next aerial",
                    tint = if (focused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
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
    onInputs: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    focus: RailFocus,
) {
    val tile = RoundedCornerShape(INNER)
    // Inner corners stay small; outer ones match the tile (inset by the 4dp padding).
    val outer = INNER - 4.dp
    val inner = 8.dp
    Row(
        Modifier
            .fillMaxHeight()
            .background(tileColor(), tile)
            .topHighlight(tile)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RailSegment(
            Symbols.Input, "Inputs", onInputs, focus.inputs,
            RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner),
        )
        SegmentDivider()
        RailSegment(
            Symbols.Notifications, "Alerts", onNotifications, focus.notifications,
            RoundedCornerShape(inner), badge = notifications,
        )
        SegmentDivider()
        RailSegment(
            Symbols.Settings, "Settings", onSettings, focus.settings,
            RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer),
        )
    }
}

/** Hairline between two segments, like the edge of the glass panels. */
@Composable
private fun SegmentDivider() {
    Box(Modifier.width(1.dp).fillMaxHeight(0.5f).background(Color.White.copy(alpha = 0.10f)))
}

class RailFocus(
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
    badge: Int = 0,
) {
    FocusSurface(
        onClick = onClick,
        shape = shape,
        container = Color.Transparent,
        modifier = Modifier.width(68.dp).fillMaxHeight().focusRequester(focus),
        focusedScale = 1.04f,
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp))
                if (badge > 0) Badge(badge, Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-6).dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
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

internal fun android.content.Context.startSafely(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
