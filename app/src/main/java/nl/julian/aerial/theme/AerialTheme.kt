package nl.julian.aerial.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeTonalSpot

/** Used until the first aerial frame has been sampled: a dusk blue. */
private const val DEFAULT_SEED = 0xFF4A5BA8.toInt()

/**
 * Material 3 surface-container roles. tv-material's ColorScheme predates
 * them, so they ride alongside it.
 */
@Immutable
data class SurfaceContainers(
    val lowest: Color,
    val low: Color,
    val default: Color,
    val high: Color,
    val highest: Color,
)

val LocalSurfaces = compositionLocalOf {
    SurfaceContainers(Color.Black, Color.DarkGray, Color.DarkGray, Color.Gray, Color.Gray)
}

/** A complete dark Material You scheme (M3 Expressive spec) for [seed]. */
fun schemeFromSeed(seed: Int): Pair<ColorScheme, SurfaceContainers> {
    val s = SchemeTonalSpot(Hct.fromInt(seed), true, 0.0, ColorSpec.SpecVersion.SPEC_2025)
    val m = MaterialDynamicColors()
    fun c(role: com.materialkolor.dynamiccolor.DynamicColor) = Color(role.getArgb(s))
    val scheme = darkColorScheme(
        primary = c(m.primary()), onPrimary = c(m.onPrimary()),
        primaryContainer = c(m.primaryContainer()), onPrimaryContainer = c(m.onPrimaryContainer()),
        inversePrimary = c(m.inversePrimary()),
        secondary = c(m.secondary()), onSecondary = c(m.onSecondary()),
        secondaryContainer = c(m.secondaryContainer()), onSecondaryContainer = c(m.onSecondaryContainer()),
        tertiary = c(m.tertiary()), onTertiary = c(m.onTertiary()),
        tertiaryContainer = c(m.tertiaryContainer()), onTertiaryContainer = c(m.onTertiaryContainer()),
        background = c(m.background()), onBackground = c(m.onBackground()),
        surface = c(m.surface()), onSurface = c(m.onSurface()),
        surfaceVariant = c(m.surfaceVariant()), onSurfaceVariant = c(m.onSurfaceVariant()),
        surfaceTint = c(m.surfaceTint()),
        inverseSurface = c(m.inverseSurface()), inverseOnSurface = c(m.inverseOnSurface()),
        error = c(m.error()), onError = c(m.onError()),
        errorContainer = c(m.errorContainer()), onErrorContainer = c(m.onErrorContainer()),
        border = c(m.outline()), borderVariant = c(m.outlineVariant()),
        scrim = c(m.scrim()),
    )
    val surfaces = SurfaceContainers(
        lowest = c(m.surfaceContainerLowest()),
        low = c(m.surfaceContainerLow()),
        default = c(m.surfaceContainer()),
        high = c(m.surfaceContainerHigh()),
        highest = c(m.surfaceContainerHighest()),
    )
    return scheme to surfaces
}

/** True when two seeds are close enough that re-theming isn't worth it. */
fun seedsSimilar(a: Int, b: Int): Boolean {
    val x = Hct.fromInt(a)
    val y = Hct.fromInt(b)
    val dh = Math.abs(x.hue - y.hue).let { if (it > 180) 360 - it else it }
    return dh < 10 && Math.abs(x.chroma - y.chroma) < 8
}

private val ColorShift = tween<Color>(durationMillis = 1600, easing = FluidEasing)

@Composable
private fun Color.drift(): Color = animateColorAsState(this, ColorShift, label = "role").value

/**
 * Material 3 theme whose colours come from the playing aerial ([seed]) and
 * drift slowly from one scheme to the next.
 */
@Composable
fun AerialTheme(seed: Int?, content: @Composable () -> Unit) {
    val (target, surfaces) = remember(seed) { schemeFromSeed(seed ?: DEFAULT_SEED) }
    val scheme = target.copy(
        primary = target.primary.drift(), onPrimary = target.onPrimary.drift(),
        primaryContainer = target.primaryContainer.drift(), onPrimaryContainer = target.onPrimaryContainer.drift(),
        secondary = target.secondary.drift(), onSecondary = target.onSecondary.drift(),
        secondaryContainer = target.secondaryContainer.drift(), onSecondaryContainer = target.onSecondaryContainer.drift(),
        tertiary = target.tertiary.drift(), onTertiary = target.onTertiary.drift(),
        tertiaryContainer = target.tertiaryContainer.drift(), onTertiaryContainer = target.onTertiaryContainer.drift(),
        surface = target.surface.drift(), onSurface = target.onSurface.drift(),
        surfaceVariant = target.surfaceVariant.drift(), onSurfaceVariant = target.onSurfaceVariant.drift(),
        inverseSurface = target.inverseSurface.drift(), inverseOnSurface = target.inverseOnSurface.drift(),
        border = target.border.drift(), borderVariant = target.borderVariant.drift(),
    )
    val drifted = SurfaceContainers(
        surfaces.lowest.drift(), surfaces.low.drift(), surfaces.default.drift(),
        surfaces.high.drift(), surfaces.highest.drift(),
    )
    MaterialTheme(colorScheme = scheme, typography = AerialTypography, shapes = AerialShapes) {
        CompositionLocalProvider(LocalSurfaces provides drifted, content = content)
    }
}
