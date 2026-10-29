package nl.julian.aerial.ui.menus

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.data.HomeSection
import nl.julian.aerial.data.LauncherConfig
import nl.julian.aerial.theme.Symbols

/** Home layout: show or hide each section, hold OK to move one up. Back returns to Settings. */
@Composable
fun LayoutPanel(config: LauncherConfig, update: ((LauncherConfig) -> LauncherConfig) -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    val order = (config.sectionOrder + HomeSection.entries).distinct()
    SidePanel("Home layout", onDismiss, initialFocus = first) {
        order.forEachIndexed { i, section ->
            val hidden = section != HomeSection.Dock && section in config.hiddenSections
            MenuRow(
                if (hidden) Symbols.Hide else Symbols.Show, section.label,
                value = when {
                    section == HomeSection.Dock -> "Always shown"
                    hidden -> "Hidden · select to show"
                    else -> "Shown · select to hide"
                },
                focusRequester = if (i == 0) first else null,
                onClick = {
                    if (section != HomeSection.Dock) update {
                        it.copy(hiddenSections = if (section in it.hiddenSections) it.hiddenSections - section else it.hiddenSections + section)
                    }
                },
                onLongClick = {
                    update {
                        val list = (it.sectionOrder + HomeSection.entries).distinct().toMutableList()
                        val at = list.indexOf(section)
                        if (at > 0) list.add(at - 1, list.removeAt(at))
                        it.copy(sectionOrder = list)
                    }
                },
            )
        }
        Text(
            "Hold OK on a section to move it up. The top bar always stays at the top.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}
