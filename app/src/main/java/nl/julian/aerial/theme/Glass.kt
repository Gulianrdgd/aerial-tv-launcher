package nl.julian.aerial.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** Corner radius of the translucent panels (the top bar and every section). */
val PANEL_RADIUS = 32.dp
/** Inner padding of a section panel. */
val PANEL_PADDING = 14.dp

/** Corner radius for tiles inside a section panel: concentric with the panel's own corner. */
val SECTION_TILE_RADIUS = PANEL_RADIUS - PANEL_PADDING

/**
 * The panels' fill: the theme's darkest container, mostly opaque. A lighter
 * tint washed out to a muddy grey over bright clips; a dark one stays crisp.
 */
@Composable
fun panelColor(): Color = LocalSurfaces.current.lowest.copy(alpha = 0.62f)

/** Hairline around a panel, like the edge of a glass plate. */
private val PanelRim = Color.White.copy(alpha = 0.10f)

/** Translucent panel: dark fill plus a hairline rim. */
@Composable
fun Modifier.panel(shape: Shape): Modifier = this
    .background(panelColor(), shape)
    .border(1.dp, PanelRim, shape)

/** Thin light line along the inside top edge of a tile, so it reads as a raised surface. */
fun Modifier.topHighlight(shape: Shape): Modifier =
    innerShadow(shape, Shadow(radius = 1.dp, color = Color.White.copy(alpha = 0.14f), offset = DpOffset(0.dp, 1.dp)))

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
            .panel(RoundedCornerShape(PANEL_RADIUS))
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
