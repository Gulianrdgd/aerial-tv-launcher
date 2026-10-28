package nl.julian.aerial.ui.widgets

import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import nl.julian.aerial.data.NetKind
import nl.julian.aerial.data.NowPlayingRepository
import nl.julian.aerial.data.WeatherRepository
import nl.julian.aerial.data.bluetoothOnFlow
import nl.julian.aerial.data.networkFlow
import nl.julian.aerial.theme.Bezel
import nl.julian.aerial.theme.ClockStyle
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.ui.FocusSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OUTER = 36.dp
private val INSET = 10.dp
internal val INNER = OUTER - INSET

/**
 * Monet's top "widget band": a Material surface container holding the
 * clock, status pills, Now Playing, weather and the settings button.
 */
@Composable
fun WidgetBand(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Bezel(modifier.fillMaxWidth().height(160.dp), outerRadius = OUTER, inset = INSET) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val context = LocalContext.current.applicationContext
            val media by remember { NowPlayingRepository.nowPlaying(context) }.collectAsStateWithLifecycle(null)
            val weather by remember { WeatherRepository.weather() }.collectAsStateWithLifecycle(null)

            ClockTile(Modifier.fillMaxHeight())
            StatusPills(Modifier.width(172.dp).fillMaxHeight())
            media?.let { NowPlayingCard(it, INNER, Modifier.weight(1f).fillMaxHeight()) }
                ?: Spacer(Modifier.weight(1f))
            weather?.let { WeatherTile(it, INNER, Modifier.fillMaxHeight().aspectRatio(0.74f)) }
            SettingsButton(onOpenSettings)
        }
    }
}

/**
 * Editorial clock: weekday and date as a label over one line of ultra-light
 * display digits, the colon in the primary colour.
 */
@Composable
private fun ClockTile(modifier: Modifier) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    // Ticks exactly on the minute boundary, not every second.
    val now by produceState(Date()) {
        while (true) {
            value = Date()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val h24 = DateFormat.is24HourFormat(context)
    val hours = SimpleDateFormat(if (h24) "HH" else "h", Locale.getDefault()).format(now)
    val minutes = SimpleDateFormat("mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now)
    val time = buildAnnotatedString {
        append(hours)
        withStyle(SpanStyle(color = cs.primary)) { append(":") }
        append(minutes)
    }

    Column(
        modifier
            .background(LocalSurfaces.current.high, RoundedCornerShape(INNER))
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(date, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant, maxLines = 1)
        Text(time, style = ClockStyle, color = cs.onSurface, maxLines = 1)
    }
}

@Composable
private fun StatusPills(modifier: Modifier) {
    val context = LocalContext.current.applicationContext
    val net by networkFlow(context).collectAsStateWithLifecycle(NetKind.None)
    val bt by bluetoothOnFlow(context).collectAsStateWithLifecycle(null)
    val ctx = LocalContext.current

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val (netIcon, netTitle) = when (net) {
            NetKind.Ethernet -> Symbols.Ethernet to "Ethernet"
            NetKind.WiFi -> Symbols.Wifi to "Wi-Fi"
            NetKind.Other -> Symbols.Wifi to "Online"
            NetKind.None -> Symbols.Offline to "Offline"
        }
        StatusPill(
            icon = netIcon,
            title = netTitle,
            subtitle = if (net == NetKind.None) "No connection" else "Connected",
            active = net != NetKind.None,
            onClick = { ctx.startSafely(Intent(Settings.ACTION_WIFI_SETTINGS)) },
            modifier = Modifier.weight(1f),
        )
        if (bt != null) {
            StatusPill(
                icon = if (bt == true) Symbols.Bluetooth else Symbols.BluetoothOff,
                title = "Bluetooth",
                subtitle = if (bt == true) "On" else "Off",
                active = bt == true,
                onClick = { ctx.startSafely(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Material pill: the icon sits in its own primary-container circle (or a
 * neutral one when inactive). On focus the whole pill inverts.
 */
@Composable
private fun StatusPill(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    FocusSurface(onClick = onClick, shape = CircleShape, modifier = modifier.fillMaxWidth(), focusedScale = 1.05f) { focused ->
        Row(Modifier.fillMaxSize().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .background(
                        when {
                            focused -> cs.primary
                            active -> cs.primaryContainer
                            else -> LocalSurfaces.current.highest
                        },
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp),
                    tint = when {
                        focused -> cs.onPrimary
                        active -> cs.onPrimaryContainer
                        else -> cs.onSurfaceVariant
                    },
                )
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                    color = LocalContentColor.current.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    FocusSurface(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.padding(horizontal = 2.dp).size(58.dp),
        container = LocalSurfaces.current.highest,
        focusedScale = 1.12f,
    ) {
        Icon(
            painterResource(Symbols.Settings), contentDescription = "Settings",
            modifier = Modifier.align(Alignment.Center).size(26.dp),
        )
    }
}

internal fun android.content.Context.startSafely(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
