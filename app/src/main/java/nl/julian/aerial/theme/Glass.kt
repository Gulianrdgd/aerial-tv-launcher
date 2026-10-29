package nl.julian.aerial.theme

import androidx.compose.runtime.State
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
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
val PANEL_RADIUS: Dp @Composable get() = 32.dp * LocalRoundness.current

/** Multiplies every corner radius (Settings > Corners). */
val LocalRoundness = staticCompositionLocalOf { 1f }
/** Inner padding of a section panel. */
val PANEL_PADDING = 14.dp

/** Corner radius for tiles inside a section panel: concentric with the panel's own corner. */
val SECTION_TILE_RADIUS: Dp @Composable get() = (PANEL_RADIUS - PANEL_PADDING).coerceAtLeast(4.dp)

/**
 * The panels' fill: the theme's darkest container, mostly opaque. A lighter
 * tint washed out to a muddy grey over bright clips; a dark one stays crisp.
 */
@Composable
fun panelColor(): Color = LocalSurfaces.current.lowest.copy(alpha = LocalPanelAlpha.current)

/** How opaque the panels are (Settings > Panel opacity). */
val LocalPanelAlpha = staticCompositionLocalOf { 0.62f }

/** Hairline around a panel, like the edge of a glass plate. */
private val PanelRim = Color.White.copy(alpha = 0.10f)

/**
 * Frosted glass: holds a tiny frame of the video, stretched across the screen
 * behind the panels; null = tinted. A State, read only while drawing, so a
 * new frame redraws the panels without recomposing them.
 */
val LocalFrost = staticCompositionLocalOf<State<ImageBitmap?>?> { null }

/** Translucent panel: dark fill plus a hairline rim; over the blurred video when [LocalFrost] has a frame. */
@Composable
fun Modifier.panel(shape: Shape): Modifier {
    val frost = LocalFrost.current ?: return this.background(panelColor(), shape).border(1.dp, PanelRim, shape)
    val root = LocalView.current.rootView
    var origin by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            val frame = frost.value ?: return@drawBehind
            // Lined up with the screen, so the panel shows the blurred picture right behind it.
            drawImage(
                frame,
                dstOffset = IntOffset(-origin.x.toInt(), -origin.y.toInt()),
                dstSize = IntSize(root.width, root.height),
                filterQuality = FilterQuality.Low,
            )
        }
        .background(LocalSurfaces.current.lowest.copy(alpha = LocalPanelAlpha.current - 0.24f), shape)
        .border(1.dp, PanelRim, shape)
}

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
