package nl.julian.aerial.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import nl.julian.aerial.theme.FluidEasing
import nl.julian.aerial.theme.FocusSpring
import nl.julian.aerial.theme.LocalLauncherColors
import nl.julian.aerial.theme.glassCore

/**
 * The one focusable building block. On focus the surface springs up in scale
 * and gains a diffuse accent glow and a crisp light ring that reads across a room.
 * Only transform, alpha and shadow change — never layout.
 */
@Composable
fun FocusSurface(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    tint: Color = LocalLauncherColors.current.tile,
    focusedScale: Float = 1.08f,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) focusedScale else 1f, FocusSpring, label = "scale")
    val lift by animateFloatAsState(
        if (focused) 1f else 0f, tween(450, easing = FluidEasing), label = "lift",
    )
    val accent = LocalLauncherColors.current.accent

    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .dropShadow(
                shape,
                Shadow(radius = 28.dp, color = accent.copy(alpha = 0.38f * lift), spread = 2.dp),
            )
            .clip(shape)
            .glassCore(shape, tint)
            .border(2.dp, Color.White.copy(alpha = 0.92f * lift), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        content(focused)
    }
}
