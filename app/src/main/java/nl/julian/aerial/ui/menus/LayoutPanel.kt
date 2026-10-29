package nl.julian.aerial.ui.menus

import nl.julian.aerial.data.TopTile
import nl.julian.aerial.data.QuickTileKind
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.ui.Text
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
        fun next(current: HomeSection?): HomeSection? {
            val all = listOf<HomeSection?>(null) + HomeSection.entries
            return all[(all.indexOf(current) + 1) % all.size]
        }
        MenuRow(
            Symbols.Sun, "Mornings, 06:00 to 12:00", value = config.morningFirst?.let { "${it.label} first" } ?: "The usual order",
            onClick = { update { it.copy(morningFirst = next(it.morningFirst)) } },
        )
        MenuRow(
            Symbols.Moon, "Evenings, 18:00 to midnight", value = config.eveningFirst?.let { "${it.label} first" } ?: "The usual order",
            onClick = { update { it.copy(eveningFirst = next(it.eveningFirst)) } },
        )
        Text(
            "Top bar, left to right",
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
        )
        (config.topBarOrder + TopTile.entries).distinct().forEachIndexed { i, part ->
            MenuRow(
                Symbols.Tune, part.label, value = "Position ${i + 1} · hold OK to move left",
                onClick = {},
                onLongClick = {
                    update {
                        val list = (it.topBarOrder + TopTile.entries).distinct().toMutableList()
                        val at = list.indexOf(part)
                        if (at > 0) list.add(at - 1, list.removeAt(at))
                        it.copy(topBarOrder = list)
                    }
                },
            )
        }
        Text(
            "Quick settings",
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
        )
        (config.quickOrder + QuickTileKind.entries).distinct().forEach { kind ->
            val off = kind in config.quickHidden
            MenuRow(
                if (off) Symbols.Hide else Symbols.Show, kind.label,
                value = if (off) "Hidden · select to show" else "Shown · select to hide",
                onClick = { update { it.copy(quickHidden = if (kind in it.quickHidden) it.quickHidden - kind else it.quickHidden + kind) } },
                onLongClick = {
                    update {
                        val list = (it.quickOrder + QuickTileKind.entries).distinct().toMutableList()
                        val at = list.indexOf(kind)
                        if (at > 0) list.add(at - 1, list.removeAt(at))
                        it.copy(quickOrder = list)
                    }
                },
            )
        }
        Text(
            "Hold OK on a section or tile to move it up. The top bar always stays at the top.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}
