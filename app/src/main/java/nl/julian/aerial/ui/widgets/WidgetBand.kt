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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import nl.julian.aerial.theme.Bezel
import nl.julian.aerial.theme.ClockStyle
import nl.julian.aerial.theme.EyebrowStyle
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.ui.FocusSurface
import nl.julian.aerial.wallpaper.Aerial
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OUTER = 36.dp
private val INSET = 10.dp
internal val INNER = OUTER - INSET
/** Every tile in the band shares this inner padding. */
internal val TILE_PADDING = 20.dp

/**
 * Monet's top band, a bento of equal-height tiles: clock, Now Playing (or,
 * when nothing plays, the aerial on screen), weather, and a vertical
 * toolbar with inputs, notifications and settings.
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
    Bezel(modifier.fillMaxWidth().height(180.dp), outerRadius = OUTER, inset = INSET) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClockTile(clockFormat, Modifier.fillMaxHeight().width(232.dp))
            when {
                media != null -> NowPlayingCard(media, INNER, Modifier.weight(1f).fillMaxHeight())
                else -> NowShowingTile(clip, onSkipAerial, Modifier.weight(1f).fillMaxHeight())
            }
            weather?.let { WeatherTile(it, INNER, Modifier.fillMaxHeight().width(150.dp)) }
            ControlRail(notificationCount, onOpenInputs, onOpenNotifications, onOpenSettings, railFocus)
        }
    }
}

/**
 * Clock: the date as an eyebrow over the time, both centred in a
 * fixed-width tile so nothing shifts as the minutes change. The colon is
 * picked out in the primary colour.
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
    val date = SimpleDateFormat("EEEE d MMM", Locale.getDefault()).format(now).uppercase()
    val time = buildAnnotatedString {
        append(hours)
        withStyle(SpanStyle(color = cs.primary)) { append(":") }
        append(minutes)
    }

    Column(
        modifier
            .background(LocalSurfaces.current.high, RoundedCornerShape(INNER))
            .padding(horizontal = TILE_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(date, style = EyebrowStyle, color = cs.onSurfaceVariant, maxLines = 1)
        Text(time, style = ClockStyle, color = cs.onSurface, maxLines = 1)
    }
}

/**
 * When nothing is playing: the aerial's place, Apple TV style. Selecting it
 * skips to the next clip.
 */
@Composable
private fun NowShowingTile(clip: Aerial?, onSkip: () -> Unit, modifier: Modifier) {
    FocusSurface(onClick = onSkip, shape = RoundedCornerShape(INNER), modifier = modifier, focusedScale = 1.03f) { focused ->
        Row(Modifier.fillMaxSize().padding(horizontal = TILE_PADDING), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "NOW SHOWING", style = EyebrowStyle, maxLines = 1,
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                )
                Text(
                    clip?.title ?: "Aerials", style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                clip?.let {
                    Text(
                        "${sceneryLabel(it.kind)} · ${collectionLabel(it.source)}",
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                        color = LocalContentColor.current.copy(alpha = 0.7f),
                    )
                }
            }
            Box(
                Modifier
                    .size(44.dp)
                    .background(if (focused) MaterialTheme.colorScheme.primary else LocalSurfaces.current.highest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(Symbols.SkipNext), contentDescription = "Next aerial",
                    tint = if (focused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private fun sceneryLabel(kind: String) = when (kind) {
    "city" -> "City"
    "nature" -> "Nature"
    "water" -> "Underwater"
    "space" -> "From space"
    else -> "Ambient"
}

private fun collectionLabel(source: String) = when (source) {
    "A" -> "Apple TV"
    "Z" -> "Fire TV"
    else -> "Community"
}

/**
 * M3 Expressive vertical toolbar: one rounded container holding the
 * inputs, notifications and settings buttons.
 */
@Composable
private fun ControlRail(
    notifications: Int,
    onInputs: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    focus: RailFocus,
) {
    Column(
        Modifier
            .fillMaxHeight()
            .background(LocalSurfaces.current.high, CircleShape)
            .padding(6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RailButton(Symbols.Input, "Inputs", onInputs, focus.inputs)
        Box {
            RailButton(Symbols.Notifications, "Notifications", onNotifications, focus.notifications)
            if (notifications > 0) Badge(notifications, Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp))
        }
        RailButton(Symbols.Settings, "Settings", onSettings, focus.settings)
    }
}

class RailFocus(
    val inputs: FocusRequester = FocusRequester(),
    val notifications: FocusRequester = FocusRequester(),
    val settings: FocusRequester = FocusRequester(),
)

@Composable
private fun RailButton(@DrawableRes icon: Int, label: String, onClick: () -> Unit, focus: FocusRequester) {
    FocusSurface(
        onClick = onClick,
        shape = CircleShape,
        container = LocalSurfaces.current.high,
        // 48dp: the comfortable focus target from across the room.
        modifier = Modifier.size(48.dp).focusRequester(focus),
        focusedScale = 1.12f,
    ) {
        Icon(painterResource(icon), contentDescription = label, modifier = Modifier.align(Alignment.Center).size(24.dp))
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
