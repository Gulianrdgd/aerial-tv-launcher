package nl.julian.aerial.ui.menus

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.data.NotificationsRepository
import nl.julian.aerial.data.TvNotification
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.Symbols

/**
 * Notifications in the side panel. Select one to open it; hold OK to
 * dismiss it. "Clear all" sits on top.
 */
@Composable
fun NotificationsPanel(items: List<TvNotification>, hasAccess: Boolean, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    val cs = MaterialTheme.colorScheme
    // Holding OK keeps sending key repeats. After a hold dismisses one
    // notification, focus moves to the next while OK is still down; without
    // this lock the repeats would dismiss that one too.
    var holdLock by remember { mutableStateOf(false) }
    val lockKeys = Modifier.onPreviewKeyEvent { e ->
        val ok = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
        if (!ok || !holdLock) return@onPreviewKeyEvent false
        if (e.type == KeyEventType.KeyUp) holdLock = false
        true
    }
    SidePanel("Notifications", onDismiss, initialFocus = first.takeIf { items.isNotEmpty() }) {
        if (items.isEmpty()) {
            Text(
                if (hasAccess) "You're all caught up." else "Notification access is off. Grant it with adb (see README).",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            return@SidePanel
        }
        MenuRow(Symbols.ClearAll, "Clear all", focusRequester = first, onClick = {
            NotificationsRepository.dismissAll()
            onDismiss()
        })
        val now = System.currentTimeMillis()
        items.forEach { n ->
            ListItem(
                selected = false,
                onClick = {
                    onDismiss()
                    NotificationsRepository.open(n)
                },
                onLongClick = {
                    if (!holdLock) {
                        holdLock = true
                        NotificationsRepository.dismiss(n)
                    }
                },
                modifier = Modifier.fillMaxWidth().then(lockKeys),
                overlineContent = {
                    Text(
                        "${n.app} · ${DateUtils.getRelativeTimeSpanString(n.postTime, now, DateUtils.MINUTE_IN_MILLIS)}",
                        maxLines = 1,
                    )
                },
                headlineContent = { Text(n.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = n.text?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
                leadingContent = {
                    n.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape)) }
                },
                shape = ListItemDefaults.shape(shape = RoundedCornerShape(24.dp)),
                colors = ListItemDefaults.colors(
                    containerColor = LocalSurfaces.current.high,
                    contentColor = cs.onSurface,
                    focusedContainerColor = cs.inverseSurface,
                    focusedContentColor = cs.inverseOnSurface,
                ),
                scale = ListItemDefaults.scale(focusedScale = 1.03f),
            )
        }
    }
}
