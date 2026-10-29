package nl.julian.aerial.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import java.util.Calendar
import java.util.Date
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Analog face: hour and minute hands in [hand] and [accent], twelve ticks, no numbers. */
@Composable
fun AnalogClock(now: Date, size: Dp, hand: Color, accent: Color, ticks: Color) {
    val cal = Calendar.getInstance().apply { time = now }
    val minutes = cal.get(Calendar.MINUTE)
    val hours = cal.get(Calendar.HOUR) + minutes / 60f
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2
        val c = center
        fun at(fraction: Float, length: Float): Offset {
            val a = fraction * 2 * PI - PI / 2
            return Offset(c.x + cos(a).toFloat() * length, c.y + sin(a).toFloat() * length)
        }
        for (i in 0 until 12) {
            drawLine(ticks, at(i / 12f, r * 0.82f), at(i / 12f, r * 0.95f), strokeWidth = r * 0.05f, cap = StrokeCap.Round)
        }
        drawLine(hand, c, at(hours / 12f, r * 0.5f), strokeWidth = r * 0.11f, cap = StrokeCap.Round)
        drawLine(accent, c, at(minutes / 60f, r * 0.75f), strokeWidth = r * 0.07f, cap = StrokeCap.Round)
        drawCircle(accent, r * 0.08f, c)
    }
}

/** The time in words, to the nearest five minutes: "ten past seven", "quarter to eight", "seven o'clock". */
fun timeInWords(now: Date): String {
    val cal = Calendar.getInstance().apply { time = now }
    val rounded = (cal.get(Calendar.MINUTE) + 2) / 5 * 5
    var hour = cal.get(Calendar.HOUR)
    val past = rounded <= 30
    if (!past || rounded == 60) hour += 1
    val name = HOURS[hour % 12]
    return when (rounded % 60) {
        0 -> "$name o'clock"
        15 -> "quarter past $name"
        30 -> "half past $name"
        45 -> "quarter to $name"
        else -> if (past) "${MINUTES[rounded]} past $name" else "${MINUTES[60 - rounded]} to $name"
    }
}

private val HOURS = listOf("twelve", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven")
private val MINUTES = mapOf(5 to "five", 10 to "ten", 20 to "twenty", 25 to "twenty-five")
