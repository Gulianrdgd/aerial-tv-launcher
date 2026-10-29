package nl.julian.aerial.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Heavy, decelerating curve: fast out of the gate, long soft landing. */
val FluidEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/** Focus lift: slightly under-damped so a tile "settles" like a physical key. */
val FocusSpring = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)

/**
 * Staggered fade-up entry. Each [trigger] change replays it (first show, and
 * every Home press), so the home screen "assembles" over the aerial.
 * Only transform + alpha are animated.
 */
fun Modifier.reveal(index: Int, trigger: Any?): Modifier = composed {
    val style = LocalReveal.current
    if (style == Reveal.None) return@composed this
    val progress = remember { Animatable(0f) }
    val rise = with(LocalDensity.current) { 28.dp.toPx() }
    val slide = with(LocalDensity.current) { 160.dp.toPx() }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        delay((if (style == Reveal.Slide) 90L else 70L) * index)
        launch { progress.animateTo(1f, tween(durationMillis = 800, easing = FluidEasing)) }
    }
    graphicsLayer {
        alpha = progress.value
        if (style == Reveal.Slide) translationX = -(1f - progress.value) * slide
        else translationY = (1f - progress.value) * rise
    }
}

/** How the home screen's panels come in: rise and fade (default), slide in from the left, or appear at once. */
enum class Reveal(val label: String) { Rise("Rise and fade"), Slide("Slide in from the side"), None("Appear at once") }

val LocalReveal = staticCompositionLocalOf { Reveal.Rise }
