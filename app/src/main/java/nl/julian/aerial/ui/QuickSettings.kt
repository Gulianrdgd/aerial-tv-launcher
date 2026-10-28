package nl.julian.aerial.ui

import android.content.Intent
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.data.NetKind
import nl.julian.aerial.data.Scenery
import nl.julian.aerial.data.bluetoothOnFlow
import nl.julian.aerial.data.networkFlow
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.SectionPanel
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.ui.widgets.startSafely

/**
 * Quick settings: a row of Material tiles under the dock. Active tiles use
 * the primary container. Android doesn't let apps switch Wi-Fi or
 * Bluetooth themselves, so those tiles show state and open the system page.
 */
@Composable
fun QuickSettings(
    scenery: Scenery,
    onCycleScenery: () -> Unit,
    onStartScreensaver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext
    val ctx = LocalContext.current
    val net by networkFlow(app).collectAsStateWithLifecycle(NetKind.None)
    val bt by bluetoothOnFlow(app).collectAsStateWithLifecycle(null)

    SectionPanel(modifier, title = "Quick settings") {
        Row(
            Modifier.fillMaxWidth().focusRestorer().focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val (netIcon, netTitle) = when (net) {
                NetKind.Ethernet -> Symbols.Ethernet to "Ethernet"
                NetKind.WiFi -> Symbols.Wifi to "Wi-Fi"
                NetKind.Other -> Symbols.Wifi to "Network"
                NetKind.None -> Symbols.Offline to "Network"
            }
            QuickTile(
                netIcon, netTitle, if (net == NetKind.None) "Offline" else "Connected", active = net != NetKind.None,
                onClick = { ctx.startSafely(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                if (bt == true) Symbols.Bluetooth else Symbols.BluetoothOff, "Bluetooth",
                when (bt) { true -> "On"; false -> "Off"; null -> "Not available" }, active = bt == true,
                onClick = { ctx.startSafely(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                Symbols.Tune, "Display", "Picture & sound", active = false,
                onClick = { ctx.startSafely(Intent(Settings.ACTION_DISPLAY_SETTINGS)) },
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                Symbols.Landscape, "Scenery", scenery.label, active = scenery != Scenery.All,
                onClick = onCycleScenery,
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                Symbols.Screensaver, "Screensaver", "Start now", active = false,
                onClick = onStartScreensaver,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Quick-settings pill: icon in its own circle, then title and state. */
@Composable
private fun QuickTile(
    @DrawableRes icon: Int,
    title: String,
    state: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    FocusSurface(
        onClick = onClick,
        shape = CircleShape,
        container = if (active) cs.primaryContainer else LocalSurfaces.current.high,
        contentColor = if (active) cs.onPrimaryContainer else cs.onSurface,
        modifier = modifier.height(64.dp),
        focusedScale = 1.05f,
    ) { focused ->
        Row(Modifier.fillMaxSize().padding(start = 8.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(
                    when {
                        focused -> cs.primary
                        active -> cs.primary.copy(alpha = 0.18f)
                        else -> LocalSurfaces.current.highest
                    },
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp),
                    tint = if (focused) cs.onPrimary else LocalContentColor.current,
                )
            }
            Column(Modifier.padding(start = 10.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    state, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = LocalContentColor.current.copy(alpha = 0.72f),
                )
            }
        }
    }
}
