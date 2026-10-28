package nl.julian.aerial.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** Corner radius of the translucent panels (the top bar and every section). */
val PANEL_RADIUS = 32.dp
/** Inner padding of a section panel. */
val PANEL_PADDING = 14.dp

/** The panels' fill: the theme's low container, letting the aerial through. */
@Composable
fun panelColor(): Color = LocalSurfaces.current.low.copy(alpha = 0.55f)

/**
 * A home-screen section on the same translucent panel as the top bar, with
 * an optional title inside it. [horizontalPadding] can be 0 for content
 * that scrolls sideways and handles its own edge padding.
 */
@Composable
fun SectionPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    horizontalPadding: Dp = PANEL_PADDING,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(panelColor(), RoundedCornerShape(PANEL_RADIUS))
            .padding(vertical = PANEL_PADDING),
    ) {
        title?.let {
            Text(
                it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = PANEL_PADDING + 8.dp, bottom = 8.dp),
            )
        }
        Column(Modifier.padding(horizontal = horizontalPadding), content = content)
    }
}
