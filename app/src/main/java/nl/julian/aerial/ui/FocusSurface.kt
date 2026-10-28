package nl.julian.aerial.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.os.SystemClock
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.topHighlight

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
        modifier = modifier.then(if (onLongClick != null) Modifier.okPressAndHold(onClick, onLongClick) else Modifier),
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
            // The ring sits a few dp outside the tile: on a white banner
            // (YouTube, Netflix) a ring on the edge itself disappears.
            focusedBorder = if (invert) Border.None
            else Border(BorderStroke(3.dp, ringColor), inset = RING_GAP, shape = focusedShape),
        ),
        // No glow: a blurred shadow per focus change is costly on TV GPUs,
        // and the scale + ring/inversion already mark focus clearly.
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow.None),
    ) {
        // Container tiles get the raised top edge; artwork tiles stay clean.
        if (style == FocusStyle.Invert) Box(Modifier.matchParentSize().topHighlight(shape))
        content(focused)
    }
}

/** Space between a focused tile and its ring. */
private val RING_GAP = 4.dp

/** Holding OK this long opens the long-press action instead of clicking. */
private const val LONG_PRESS_MS = 500L

/**
 * OK handled here instead of by the Surface, because TV remotes report a
 * hold differently: some send key repeats, some only a late release. A
 * short press clicks on release; a hold fires [onLongClick] once and its
 * release never clicks.
 */
private fun Modifier.okPressAndHold(onClick: () -> Unit, onLongClick: () -> Unit): Modifier = composed {
    var downAt by remember { mutableLongStateOf(0L) }
    var held by remember { mutableStateOf(false) }
    onPreviewKeyEvent { e ->
        val ok = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
        if (!ok) return@onPreviewKeyEvent false
        val now = SystemClock.uptimeMillis()
        when (e.type) {
            KeyEventType.KeyDown -> {
                if (e.nativeKeyEvent.repeatCount == 0) {
                    downAt = now
                    held = false
                } else if (!held && downAt != 0L && (e.nativeKeyEvent.isLongPress || now - downAt >= LONG_PRESS_MS)) {
                    held = true
                    onLongClick()
                }
            }
            KeyEventType.KeyUp -> {
                if (downAt != 0L && !held) {
                    // Remotes without key repeats: judge the hold by its length.
                    if (now - downAt >= LONG_PRESS_MS) onLongClick() else onClick()
                }
                downAt = 0L
            }
        }
        true
    }
}
