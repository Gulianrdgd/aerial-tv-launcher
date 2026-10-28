package nl.julian.aerial.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The handful of colour roles the launcher paints with, derived from the
 * aerial's current frame by DynamicTheme.
 */
@Immutable
data class LauncherColors(
    /** Big translucent panels (widget band, dock band). */
    val panel: Color,
    /** Tiles and pills sitting on a panel. */
    val tile: Color,
    /** Highlighted elements: clock digits, active pill icon, focus ring. */
    val accent: Color,
    val onAccent: Color,
    val content: Color,
    val contentMuted: Color,
)

val DefaultLauncherColors = LauncherColors(
    panel = Color(0xFF1E2340).copy(alpha = 0.55f),
    tile = Color(0xFF3A4170).copy(alpha = 0.70f),
    accent = Color(0xFFE9C46A),
    onAccent = Color(0xFF2B2000),
    content = Color(0xFFF2F0FA),
    contentMuted = Color(0xFFF2F0FA).copy(alpha = 0.65f),
)

// Not static: colours animate, and only the readers should recompose.
val LocalLauncherColors = compositionLocalOf { DefaultLauncherColors }
