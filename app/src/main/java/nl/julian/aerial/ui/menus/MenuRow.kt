package nl.julian.aerial.ui.menus

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import nl.julian.aerial.ui.Text
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.ui.okPressAndHold

/**
 * One row in a menu or the settings panel: a leading symbol, a headline and
 * an optional supporting value. On focus it inverts and lifts slightly, like
 * the system Settings on Google TV.
 *
 * Drawn with plain foundation pieces and the same measurements as
 * tv-material's ListItem (which it replaced): ListItem wraps every row in a
 * Surface and several layers of slots, and a settings page builds dozens of
 * rows at once, which made opening and moving through Settings slow on a TV.
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
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.03f else 1f, tween(150), label = "row")
    val content = if (focused) cs.inverseOnSurface else cs.onSurface
    Row(
        modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(if (focused) cs.inverseSurface else LocalSurfaces.current.high, CircleShape)
            .then(if (onLongClick != null) Modifier.okPressAndHold(onClick, onLongClick) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .heightIn(min = if (value != null) 64.dp else 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                value?.let {
                    Text(
                        it, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.8f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing?.let {
                Spacer(Modifier.width(8.dp))
                ProvideTextStyle(MaterialTheme.typography.labelLarge) { it() }
            }
        }
    }
}
