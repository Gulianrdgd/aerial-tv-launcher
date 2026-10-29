package nl.julian.aerial.ui.menus

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.ui.Text
import nl.julian.aerial.theme.LocalSurfaces

/**
 * One row in a menu or the settings panel: a Material TV list item with a
 * leading symbol, a headline and an optional supporting value. On focus it
 * inverts, like the system Settings on Google TV.
 */
@Composable
fun MenuRow(
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    focusRequester: FocusRequester? = null,
    onLongClick: (() -> Unit)? = null,
    /** Shown at the end of the row, e.g. a switch or the current value. */
    trailing: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    ListItem(
        selected = false,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        headlineContent = { Text(title, maxLines = 1) },
        supportingContent = value?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
        trailingContent = trailing,
        leadingContent = {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(ListItemDefaults.IconSize))
        },
        shape = ListItemDefaults.shape(shape = CircleShape),
        colors = ListItemDefaults.colors(
            containerColor = LocalSurfaces.current.high,
            contentColor = cs.onSurface,
            focusedContainerColor = cs.inverseSurface,
            focusedContentColor = cs.inverseOnSurface,
        ),
        scale = ListItemDefaults.scale(focusedScale = 1.03f),
    )
}
