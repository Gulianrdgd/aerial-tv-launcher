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
 * with it. Heavier at the bottom, where the dock and grid sit.
 */
@Composable
fun Haze(modifier: Modifier = Modifier) {
    val tint = LocalSurfaces.current.lowest
    Box(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to tint.copy(alpha = 0.18f),
                0.5f to tint.copy(alpha = 0.26f),
                1f to tint.copy(alpha = 0.42f),
            )
        )
    )
}

/** Legibility scrim: darkens only the top and bottom bands, keeping the centre clear. */
@Composable
fun Scrim(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0.0f to Color.Black.copy(alpha = 0.45f),
                0.25f to Color.Transparent,
                0.70f to Color.Transparent,
                1.0f to Color.Black.copy(alpha = 0.55f),
            )
        )
    )
}
