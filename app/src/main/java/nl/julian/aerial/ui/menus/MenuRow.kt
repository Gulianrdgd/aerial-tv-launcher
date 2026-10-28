package nl.julian.aerial.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import nl.julian.aerial.theme.AerialType
import nl.julian.aerial.theme.LocalLauncherColors
import nl.julian.aerial.ui.FocusSurface

/**
 * One pill-shaped row in a menu or the settings panel: the icon nested in its
 * own circle, a label, and an optional value or hint underneath.
 */
@Composable
fun MenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    focusRequester: FocusRequester? = null,
) {
    val colors = LocalLauncherColors.current
    FocusSurface(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier
            .fillMaxWidth()
            .height(if (value != null) 52.dp else 48.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        focusedScale = 1.03f,
    ) { focused ->
        Row(Modifier.padding(start = 6.dp, end = 20.dp).align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(if (focused) colors.accent else colors.panel, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = if (focused) colors.onAccent else colors.content, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, style = AerialType.pillTitle, color = colors.content, maxLines = 1)
                value?.let {
                    Text(it, style = AerialType.pillSub, color = colors.contentMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
