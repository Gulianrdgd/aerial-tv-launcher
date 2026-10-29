package nl.julian.aerial.ui.menus

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.ui.Text
import nl.julian.aerial.data.AppRepository
import nl.julian.aerial.data.LauncherConfig
import nl.julian.aerial.data.RemoteKey
import nl.julian.aerial.theme.Symbols

/** Remote shortcuts: what holding Home or a colour button does. Back returns to Settings. */
@Composable
fun ShortcutsPanel(
    config: LauncherConfig,
    serviceOn: Boolean,
    update: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val apps = remember { AppRepository.memoryCache.orEmpty().sortedBy { it.label.lowercase() } }
    fun label(action: String?) = when {
        action == null -> "Nothing"
        action == "inputs" -> "Input switcher"
        action == "screensaver" -> "Screensaver"
        else -> apps.firstOrNull { "app:" + it.pkg == action }?.label ?: "An app"
    }
    var picking by remember { mutableStateOf<RemoteKey?>(null) }
    val first = remember { FocusRequester() }
    val key = picking
    if (key == null) {
        SidePanel("Remote shortcuts", onDismiss, initialFocus = first) {
            if (!serviceOn) {
                Text(
                    "Turn on Remote buttons first (More options > System), so Aerial can see these keys.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            RemoteKey.entries.forEachIndexed { i, k ->
                MenuRow(
                    Symbols.Tune, k.label, value = label(config.shortcuts[k]),
                    focusRequester = if (i == 0) first else null,
                    onClick = { picking = k },
                )
            }
        }
    } else {
        // The choices for one key; Back goes back to the list of keys.
        val firstChoice = remember { FocusRequester() }
        fun set(action: String?) {
            update { it.copy(shortcuts = if (action == null) it.shortcuts - key else it.shortcuts + (key to action)) }
            picking = null
        }
        SidePanel(key.label, onDismiss = { picking = null }, initialFocus = firstChoice) {
            MenuRow(Symbols.Close, "Nothing", onClick = { set(null) }, focusRequester = firstChoice)
            MenuRow(Symbols.Input, "Input switcher", onClick = { set("inputs") })
            MenuRow(Symbols.Screensaver, "Screensaver", onClick = { set("screensaver") })
            apps.forEach { a -> MenuRow(Symbols.Play, a.label, onClick = { set("app:" + a.pkg) }) }
        }
    }
}
