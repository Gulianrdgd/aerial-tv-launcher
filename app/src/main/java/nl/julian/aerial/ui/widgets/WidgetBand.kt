package nl.julian.aerial.ui.widgets

import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
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
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import nl.julian.aerial.data.NetKind
import nl.julian.aerial.data.NowPlayingRepository
import nl.julian.aerial.data.WeatherRepository
import nl.julian.aerial.data.bluetoothOnFlow
import nl.julian.aerial.data.networkFlow
import nl.julian.aerial.theme.AerialType
import nl.julian.aerial.theme.Bezel
import nl.julian.aerial.theme.LineIcons
import nl.julian.aerial.theme.LocalLauncherColors
import nl.julian.aerial.theme.glassCore
import nl.julian.aerial.ui.FocusSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OUTER = 36.dp
private val INSET = 10.dp
private val INNER = OUTER - INSET

/**
 * Monet's top "widget band": a glass tray holding a stacked clock tile,
 * status pills and a round settings key. A bento of unequal cells.
 */
@Composable
fun WidgetBand(modifier: Modifier = Modifier) {
    Bezel(modifier.fillMaxWidth().height(160.dp), outerRadius = OUTER, inset = INSET) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val context = LocalContext.current.applicationContext
            val media by remember { NowPlayingRepository.nowPlaying(context) }.collectAsStateWithLifecycle(null)
            val weather by remember { WeatherRepository.weather() }.collectAsStateWithLifecycle(null)

            ClockTile(Modifier.fillMaxHeight().aspectRatio(0.96f))
            StatusPills(Modifier.width(188.dp).fillMaxHeight())
            media?.let { NowPlayingCard(it, INNER, Modifier.weight(1f).fillMaxHeight()) }
                ?: Spacer(Modifier.weight(1f))
            weather?.let { WeatherTile(it, INNER, Modifier.fillMaxHeight().aspectRatio(0.84f)) }
            SettingsKey()
        }
    }
}

@Composable
private fun ClockTile(modifier: Modifier) {
    val context = LocalContext.current
    val colors = LocalLauncherColors.current
    // Ticks exactly on the minute boundary, not every second.
    val now by produceState(Date()) {
        while (true) {
            value = Date()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val h24 = DateFormat.is24HourFormat(context)
    val hours = SimpleDateFormat(if (h24) "HH" else "hh", Locale.getDefault()).format(now)
    val minutes = SimpleDateFormat("mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now).uppercase()

    Column(
        modifier.glassCore(RoundedCornerShape(INNER), colors.tile),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(date, style = AerialType.eyebrow, color = colors.contentMuted)
        Spacer(Modifier.height(6.dp))
        Text(hours, style = AerialType.clock, color = colors.accent)
        Text(minutes, style = AerialType.clock, color = colors.accent)
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
            NetKind.Ethernet -> LineIcons.Ethernet to "Ethernet"
            NetKind.WiFi -> LineIcons.Wifi to "Wi-Fi"
            NetKind.Other -> LineIcons.Wifi to "Online"
            NetKind.None -> LineIcons.Offline to "Offline"
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
                icon = if (bt == true) LineIcons.Bluetooth else LineIcons.BluetoothOff,
                title = "Bluetooth",
                subtitle = if (bt == true) "On" else "Off",
                active = bt == true,
                onClick = { ctx.startSafely(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Pill with its icon nested in its own circle, flush to the leading edge. */
@Composable
private fun StatusPill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    FocusSurface(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier.fillMaxWidth(),
        focusedScale = 1.05f,
    ) {
        Row(
            Modifier.fillMaxSize().padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .background(if (active) colors.accent else colors.panel, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon, contentDescription = null, modifier = Modifier.size(22.dp),
                    tint = if (active) colors.onAccent else colors.contentMuted,
                )
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, style = AerialType.pillTitle, color = colors.content, maxLines = 1)
                Text(subtitle, style = AerialType.pillSub, color = colors.contentMuted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SettingsKey() {
    val ctx = LocalContext.current
    val colors = LocalLauncherColors.current
    FocusSurface(
        onClick = { ctx.startSafely(Intent(Settings.ACTION_SETTINGS)) },
        shape = CircleShape,
        modifier = Modifier.padding(horizontal = 2.dp).size(58.dp),
        focusedScale = 1.12f,
    ) {
        Icon(
            LineIcons.Gear, contentDescription = "Settings",
            tint = colors.content, modifier = Modifier.align(Alignment.Center).size(26.dp),
        )
    }
}

internal fun android.content.Context.startSafely(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
