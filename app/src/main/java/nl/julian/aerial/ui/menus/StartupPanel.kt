package nl.julian.aerial.ui.menus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.julian.aerial.data.AppRepository
import nl.julian.aerial.data.InputsRepository
import nl.julian.aerial.data.LauncherConfig
import nl.julian.aerial.theme.Symbols

/** What the TV opens when it turns on: the home screen, an input, or an app. Back returns to Settings. */
@Composable
fun StartupPanel(config: LauncherConfig, update: ((LauncherConfig) -> LauncherConfig) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val inputs by remember { InputsRepository.inputs(context) }.collectAsStateWithLifecycle(emptyList())
    val apps = remember { AppRepository.memoryCache.orEmpty().sortedBy { it.label.lowercase() } }
    val first = remember { FocusRequester() }
    fun set(target: String?) {
        update { it.copy(startup = target) }
        onDismiss()
    }
    SidePanel("When the TV turns on", onDismiss, initialFocus = first) {
        MenuRow(Symbols.Home, "Home screen", onClick = { set(null) }, focusRequester = first,
            value = if (config.startup == null) "Chosen" else null)
        inputs.forEach { input ->
            val key = InputsRepository.DOCK_PREFIX + input.id
            MenuRow(Symbols.Input, input.label, onClick = { set(key) }, value = if (config.startup == key) "Chosen" else null)
        }
        apps.forEach { a ->
            val key = "app:" + a.pkg
            MenuRow(Symbols.Play, a.label, onClick = { set(key) }, value = if (config.startup == key) "Chosen" else null)
        }
    }
}

/** A short name for [LauncherConfig.startup], for the settings row. */
fun startupLabel(target: String?, inputs: List<nl.julian.aerial.data.TvInput>): String = when {
    target == null -> "Home screen"
    target.startsWith("app:") -> AppRepository.memoryCache?.firstOrNull { it.pkg == target.removePrefix("app:") }?.label ?: "An app"
    else -> inputs.firstOrNull { InputsRepository.DOCK_PREFIX + it.id == target }?.label ?: "An input"
}
