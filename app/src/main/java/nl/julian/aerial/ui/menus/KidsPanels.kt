package nl.julian.aerial.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.ui.Text
import nl.julian.aerial.data.AppRepository
import nl.julian.aerial.data.LauncherConfig
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.Symbols

/** Shown when a kid opens an app outside the allowed hours. */
@Composable
fun TimesUp(message: String, onDismiss: () -> Unit) {
    Overlay(onDismiss) {
        Column(
            Modifier
                .width(480.dp)
                .background(LocalSurfaces.current.default, MaterialTheme.shapes.extraLarge)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Time's up", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DialogButton("OK", primary = true, onClick = onDismiss)
        }
    }
}

/** Which apps kids may open: select one to allow or disallow it. Back returns to Settings. */
@Composable
fun KidsAppsPanel(config: LauncherConfig, update: ((LauncherConfig) -> LauncherConfig) -> Unit, onDismiss: () -> Unit) {
    val apps = remember { AppRepository.memoryCache.orEmpty().sortedBy { it.label.lowercase() } }
    val first = remember { FocusRequester() }
    SidePanel("Kids' apps", onDismiss, initialFocus = first) {
        apps.forEachIndexed { i, a ->
            val allowed = a.pkg in config.kidsApps
            MenuRow(
                if (allowed) Symbols.Show else Symbols.Hide, a.label, value = if (allowed) "Allowed" else "Not allowed",
                focusRequester = if (i == 0) first else null,
                onClick = { update { it.copy(kidsApps = if (a.pkg in it.kidsApps) it.kidsApps - a.pkg else it.kidsApps + a.pkg) } },
            )
        }
    }
}
