package nl.julian.aerial.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import nl.julian.aerial.data.AppEntry
import nl.julian.aerial.theme.FluidEasing
import nl.julian.aerial.theme.LocalSurfaces

/** Opening animation: the chosen tile grows from [from] (its bounds on screen) to full screen, then [onDone] opens the app. */
@Composable
fun LaunchZoom(app: AppEntry, from: Rect, durationMs: Int, onDone: () -> Unit) {
    val p = remember(app) { Animatable(0f) }
    LaunchedEffect(app) {
        p.animateTo(1f, tween(durationMs, easing = FluidEasing))
        onDone()
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        Box(
            Modifier
                .layout { measurable, _ ->
                    val t = p.value
                    val left = lerp(from.left, 0f, t)
                    val top = lerp(from.top, 0f, t)
                    val width = lerp(from.width, w, t).toInt()
                    val height = lerp(from.height, h, t).toInt()
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(width, height) { placeable.place(left.toInt(), top.toInt()) }
                }
                .clip(RoundedCornerShape(lerp(18f, 0f, p.value).dp))
                .background(LocalSurfaces.current.high),
            contentAlignment = Alignment.Center,
        ) {
            val art = app.banner ?: app.icon
            if (art != null) {
                Image(
                    art, contentDescription = null,
                    contentScale = if (app.banner != null) ContentScale.Crop else ContentScale.Fit,
                    modifier = if (app.banner != null) Modifier.fillMaxSize() else Modifier.size(96.dp),
                )
            }
        }
    }
}
