package nl.julian.aerial.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.util.lerp
import androidx.compose.runtime.remember
import nl.julian.aerial.theme.LocalSurfaces

/**
 * Fallback backdrop (no aerial yet, or offline): near-black with slow-glowing
 * dusk orbs, the same palette as the app banner.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().background(Color(0xFF07080F))) {
        fun orb(cx: Float, cy: Float, r: Float, c: Color) = drawCircle(
            Brush.radialGradient(listOf(c, Color.Transparent), Offset(cx, cy), r),
            radius = r, center = Offset(cx, cy),
        )
        orb(size.width * 0.18f, size.height * 0.85f, size.width * 0.55f, Color(0x66E8A36B))
        orb(size.width * 0.82f, size.height * 0.20f, size.width * 0.50f, Color(0x556B4E8C))
        orb(size.width * 0.50f, size.height * 0.05f, size.width * 0.40f, Color(0x442A3A80))
    }
}

/**
 * Haze over the aerial while the home screen is up: a veil in the current
 * theme colour, so the video recedes behind the glass instead of competing
 * with it. Heavier at the bottom, where the dock and grid sit. The legibility
 * scrim (darker top and bottom bands, clear centre) is folded into the same
 * gradient: one full-screen blend per frame instead of two, which a TV's GPU
 * notices.
 */
@Composable
fun Haze(modifier: Modifier = Modifier) {
    val tint = LocalSurfaces.current.lowest
    val stops = remember(tint) {
        fun haze(t: Float) = tint.copy(alpha = if (t < 0.5f) lerp(0.08f, 0.14f, t / 0.5f) else lerp(0.14f, 0.30f, (t - 0.5f) / 0.5f))
        fun scrim(t: Float) = Color.Black.copy(
            alpha = when {
                t < 0.25f -> 0.45f * (1f - t / 0.25f)
                t < 0.70f -> 0f
                else -> 0.55f * (t - 0.70f) / 0.30f
            }
        )
        listOf(0f, 0.125f, 0.25f, 0.5f, 0.7f, 0.85f, 1f).map { it to scrim(it).compositeOver(haze(it)) }.toTypedArray()
    }
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(*stops)))
}
