package nl.julian.aerial.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import nl.julian.aerial.data.LauncherConfig
import java.time.LocalDate
import java.time.Month
import kotlin.math.sin
import kotlin.random.Random

/** Snow in December and a gold frame on New Year's Eve and Day, each when switched on. */
@Composable
fun Seasonal(config: LauncherConfig) {
    val today = remember { LocalDate.now() }
    if (config.snow && today.month == Month.DECEMBER) Snow()
    val newYear = (today.month == Month.DECEMBER && today.dayOfMonth == 31) || (today.month == Month.JANUARY && today.dayOfMonth == 1)
    if (config.newYearFrame && newYear) {
        Box(
            Modifier.fillMaxSize().border(
                6.dp,
                Brush.linearGradient(listOf(Color(0xFFFFE08A), Color(0xFFC9A227), Color(0xFFFFF1C2), Color(0xFFC9A227))),
                RectangleShape,
            )
        )
    }
}

/** A light snowfall: 70 soft flakes drifting down, drawn on one canvas. */
@Composable
private fun Snow() {
    val flakes = remember { List(70) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), 0.4f + Random.nextFloat()) } }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t = (it - start) / 1e9f }
    }
    Canvas(Modifier.fillMaxSize()) {
        flakes.forEach { (x0, y0, depth) ->
            // Nearer flakes (larger depth) are bigger, brighter and fall faster.
            val y = (y0 + t * 0.04f * depth) % 1f
            val x = x0 + sin(t * 0.6f + y0 * 20f) * 0.01f
            drawCircle(
                Color.White.copy(alpha = 0.35f + 0.4f * (depth - 0.4f)),
                radius = 1.5f.dp.toPx() * depth + 1f,
                center = Offset(x * size.width, y * size.height),
            )
        }
    }
}
