package nl.julian.aerial.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** Hairline rim, brighter along the top edge where "light" hits the glass. */
private val RimBrush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
)

/**
 * Monet-style frosted glass. A SurfaceView video can't be blurred from
 * Compose, so the frost is a tinted translucency plus a light rim.
 */
fun Modifier.glass(shape: Shape, tint: Color): Modifier = this
    .background(tint, shape)
    .border(Dp.Hairline, RimBrush, shape)

/** Inner core: the tile sitting in the tray, with an inset top highlight. */
fun Modifier.glassCore(shape: Shape, tint: Color): Modifier = this
    .background(tint, shape)
    .innerShadow(shape, Shadow(radius = 1.dp, color = Color.White.copy(alpha = 0.16f), offset = DpOffset(0.dp, 1.dp)))

/**
 * Double-bezel tray: an outer glass shell of [outerRadius] with [inset]
 * padding. Children use [innerRadius] (outer − inset) so the curves stay
 * concentric, like a glass plate machined into an aluminium tray.
 */
@Composable
fun Bezel(
    modifier: Modifier = Modifier,
    outerRadius: Dp = 36.dp,
    inset: Dp = 10.dp,
    tint: Color = LocalLauncherColors.current.panel,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .glass(RoundedCornerShape(outerRadius), tint)
            .padding(inset),
        content = content,
    )
}

fun innerRadius(outer: Dp = 36.dp, inset: Dp = 10.dp): Dp = outer - inset
