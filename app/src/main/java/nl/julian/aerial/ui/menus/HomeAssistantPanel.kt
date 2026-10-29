package nl.julian.aerial.ui.menus

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.data.HaConfig
import nl.julian.aerial.data.HaEntity
import nl.julian.aerial.data.HomeAssistant
import nl.julian.aerial.data.LauncherConfig
import nl.julian.aerial.theme.Symbols

/** What Aerial shows from Home Assistant: buttons, the doorbell and its camera. Back returns to Settings. */
@Composable
fun HomeAssistantPanel(
    config: LauncherConfig,
    update: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val ha = config.homeAssistant ?: return
    val entities by produceState<List<HaEntity>?>(null, ha.server, ha.token) {
        value = HomeAssistant.states(ha).getOrDefault(emptyList()).sortedBy { it.name.lowercase() }
    }
    fun change(f: (HaConfig) -> HaConfig) = update { c -> c.copy(homeAssistant = c.homeAssistant?.let(f)) }
    fun <T> next(list: List<T>, current: T?): T? = (listOf<T?>(null) + list).let { it[(it.indexOf(current) + 1) % it.size] }
    val first = remember { FocusRequester() }
    val all = entities.orEmpty()
    val bells = all.filter { it.domain == "binary_sensor" || it.domain == "event" }.map { it.id }
    val cams = all.filter { it.domain == "camera" }.map { it.id }
    fun name(id: String?) = id?.let { i -> all.firstOrNull { it.id == i }?.name ?: i } ?: "None"

    SidePanel("Home Assistant", onDismiss, initialFocus = first) {
        MenuRow(
            Symbols.Close, "Sign out", value = ha.server.substringAfter("://"), focusRequester = first,
            onClick = { update { it.copy(homeAssistant = null) }; onDismiss() },
        )
        MenuRow(Symbols.Notifications, "Doorbell", value = name(ha.doorbell), onClick = { change { it.copy(doorbell = next(bells, it.doorbell)) } })
        MenuRow(Symbols.MediaPlayer, "Doorbell camera", value = name(ha.camera), onClick = { change { it.copy(camera = next(cams, it.camera)) } })
        Text(
            if (entities == null) "Loading…" else "Buttons on the home screen",
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
        )
        all.filter { it.domain in HomeAssistant.BUTTON_DOMAINS }.forEach { e ->
            val shown = e.id in ha.buttons
            MenuRow(
                if (shown) Symbols.Show else Symbols.Hide, e.name, value = if (shown) "On the home screen" else e.id,
                onClick = { change { it.copy(buttons = if (e.id in it.buttons) it.buttons - e.id else it.buttons + e.id) } },
            )
        }
    }
}
