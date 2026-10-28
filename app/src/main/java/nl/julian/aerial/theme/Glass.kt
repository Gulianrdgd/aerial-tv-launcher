package nl.julian.aerial.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How much of the aerial shows through Material containers. */
const val CONTAINER_ALPHA = 0.6f

/**
 * A Material surface container over the aerial: tonal, slightly
 * translucent, no outline. Children use [outerRadius] − [inset] so the
 * corners stay concentric.
 */
@Composable
fun Bezel(
    modifier: Modifier = Modifier,
    outerRadius: Dp = 36.dp,
    inset: Dp = 10.dp,
    color: Color = LocalSurfaces.current.low.copy(alpha = CONTAINER_ALPHA),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .background(color, RoundedCornerShape(outerRadius))
            .padding(inset),
        content = content,
    )
}
