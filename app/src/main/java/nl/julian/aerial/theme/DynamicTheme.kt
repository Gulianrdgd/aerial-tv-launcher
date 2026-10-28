package nl.julian.aerial.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette

/**
 * Maps a seed colour (sampled from the aerial) to the launcher's roles, the
 * way Material You builds a dark scheme: one vivid palette for the accent and
 * one low-chroma palette for the glass, both on the seed's hue.
 */
fun colorsFromSeed(seed: Int): LauncherColors {
    val hct = Hct.fromInt(seed)
    val accent = TonalPalette.fromHueAndChroma(hct.hue, maxOf(hct.chroma, 40.0))
    val glass = TonalPalette.fromHueAndChroma(hct.hue, minOf(hct.chroma, 18.0))
    val content = Color(glass.tone(96))
    return LauncherColors(
        panel = Color(glass.tone(18)).copy(alpha = 0.52f),
        tile = Color(glass.tone(30)).copy(alpha = 0.66f),
        accent = Color(accent.tone(82)),
        onAccent = Color(accent.tone(18)),
        content = content,
        contentMuted = content.copy(alpha = 0.66f),
    )
}

/** True when two seeds are close enough that re-theming isn't worth it. */
fun seedsSimilar(a: Int, b: Int): Boolean {
    val x = Hct.fromInt(a)
    val y = Hct.fromInt(b)
    val dh = Math.abs(x.hue - y.hue).let { if (it > 180) 360 - it else it }
    return dh < 10 && Math.abs(x.chroma - y.chroma) < 8
}

private val ColorShift = tween<Color>(durationMillis = 1600, easing = FluidEasing)

/** Provides [LocalLauncherColors] for [seed], drifting slowly between schemes. */
@Composable
fun DynamicTheme(seed: Int?, content: @Composable () -> Unit) {
    val target = remember(seed) { seed?.let(::colorsFromSeed) ?: DefaultLauncherColors }
    val panel by animateColorAsState(target.panel, ColorShift, label = "panel")
    val tile by animateColorAsState(target.tile, ColorShift, label = "tile")
    val accent by animateColorAsState(target.accent, ColorShift, label = "accent")
    val onAccent by animateColorAsState(target.onAccent, ColorShift, label = "onAccent")
    val text by animateColorAsState(target.content, ColorShift, label = "content")
    val muted by animateColorAsState(target.contentMuted, ColorShift, label = "muted")
    CompositionLocalProvider(
        LocalLauncherColors provides LauncherColors(panel, tile, accent, onAccent, text, muted),
        content = content,
    )
}
