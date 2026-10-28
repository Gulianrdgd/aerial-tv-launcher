package nl.julian.aerial.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import nl.julian.aerial.theme.LocalSurfaces

/** How a focusable surface changes when it takes focus. */
enum class FocusStyle {
    /** Material TV pill/button: the container inverts to a light tone. */
    Invert,
    /** Artwork (banners, posters): stays as is, lifted with a light ring. */
    Ring,
}

/**
 * The one focusable building block, a tv-material [Surface] with Material's
 * TV focus behaviour: it scales up, and either inverts its container or
 * gains a ring. [highlighted] (e.g. while being moved) swaps the ring for
 * the primary colour.
 */
@Composable
fun FocusSurface(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    container: Color = LocalSurfaces.current.high,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    style: FocusStyle = FocusStyle.Invert,
    focusedShape: Shape = shape,
    focusedScale: Float = 1.06f,
    onLongClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val invert = style == FocusStyle.Invert && !highlighted
    val ringColor = if (highlighted) cs.primary else cs.onSurface

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        interactionSource = interaction,
        shape = ClickableSurfaceDefaults.shape(shape = shape, focusedShape = focusedShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = contentColor,
            focusedContainerColor = if (invert) cs.inverseSurface else container,
            focusedContentColor = if (invert) cs.inverseOnSurface else contentColor,
            pressedContainerColor = if (invert) cs.inverseSurface else container,
            pressedContentColor = if (invert) cs.inverseOnSurface else contentColor,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = if (highlighted) focusedScale + 0.04f else focusedScale),
        border = ClickableSurfaceDefaults.border(
            border = if (highlighted) Border(BorderStroke(3.dp, ringColor), shape = shape) else Border.None,
            focusedBorder = if (invert) Border.None
            else Border(BorderStroke(if (highlighted) 3.dp else 2.dp, ringColor), shape = focusedShape),
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(elevationColor = cs.primary.copy(alpha = 0.45f), elevation = 12.dp),
        ),
    ) {
        content(focused)
    }
}
