package nl.julian.aerial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.julian.aerial.data.AppRepository
import nl.julian.aerial.data.JellyfinLogin
import nl.julian.aerial.data.JellyfinRepository
import nl.julian.aerial.theme.PANEL_PADDING
import nl.julian.aerial.theme.SectionPanel
import nl.julian.aerial.ui.widgets.startSafely

/**
 * What to watch next on your Jellyfin server. Jellyfin's TV app has no
 * documented link to one show, so a card opens the app.
 */
@Composable
fun JellyfinRow(login: JellyfinLogin, radius: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val items by remember(login) { JellyfinRepository.items(context.applicationContext, login) }.collectAsStateWithLifecycle(emptyList())
    if (items.isEmpty()) return
    SectionPanel(modifier, title = "Jellyfin", horizontalPadding = 0.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth - PANEL_PADDING * 2 - GAP * (VISIBLE_CARDS - 1)) / VISIBLE_CARDS
            LazyRow(
                Modifier.focusRestorer(),
                contentPadding = PaddingValues(horizontal = PANEL_PADDING, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(GAP),
            ) {
                items(items, key = { it.id }) { item ->
                    WatchNextCard(
                        item, radius, Modifier.width(cardWidth),
                        onClick = { AppRepository.launchIntent(context, JellyfinRepository.APP)?.let(context::startSafely) },
                    )
                }
            }
        }
    }
}
