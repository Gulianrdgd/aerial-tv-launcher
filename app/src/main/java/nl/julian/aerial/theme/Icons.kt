package nl.julian.aerial.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hairline icon set (1.5 stroke on a 24 grid), drawn here instead of the
 * thick Material glyphs. Tint them with Icon(tint = …).
 */
object LineIcons {
    private fun icon(name: String, block: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()

    val Ethernet = icon("ethernet") {
        // RJ45 plug: body, latch notch, three pins
        moveTo(5f, 9f); lineTo(19f, 9f); lineTo(19f, 17f); lineTo(15f, 17f); lineTo(15f, 20f)
        lineTo(9f, 20f); lineTo(9f, 17f); lineTo(5f, 17f); close()
        moveTo(9f, 12f); lineTo(9f, 13.5f)
        moveTo(12f, 12f); lineTo(12f, 13.5f)
        moveTo(15f, 12f); lineTo(15f, 13.5f)
        moveTo(9f, 9f); lineTo(9f, 5f); lineTo(15f, 5f); lineTo(15f, 9f)
    }

    val Wifi = icon("wifi") {
        moveTo(2.5f, 9f); curveTo(7.8f, 4.3f, 16.2f, 4.3f, 21.5f, 9f)
        moveTo(5.5f, 12.5f); curveTo(9.2f, 9.4f, 14.8f, 9.4f, 18.5f, 12.5f)
        moveTo(8.7f, 16f); curveTo(10.6f, 14.6f, 13.4f, 14.6f, 15.3f, 16f)
        moveTo(12f, 19.2f); lineTo(12f, 19.3f)
    }

    val Offline = icon("offline") {
        moveTo(2.5f, 9f); curveTo(5.4f, 6.4f, 9.2f, 5.2f, 12.9f, 5.4f)
        moveTo(8.7f, 16f); curveTo(10.6f, 14.6f, 13.4f, 14.6f, 15.3f, 16f)
        moveTo(12f, 19.2f); lineTo(12f, 19.3f)
        moveTo(3f, 3f); lineTo(21f, 21f)
    }

    val Bluetooth = icon("bluetooth") {
        moveTo(7f, 7.5f); lineTo(16.5f, 16f); lineTo(12f, 20f); lineTo(12f, 4f)
        lineTo(16.5f, 8f); lineTo(7f, 16.5f)
    }

    val BluetoothOff = icon("bluetooth_off") {
        moveTo(12f, 12f); lineTo(12f, 20f); lineTo(16.5f, 16f); lineTo(14.2f, 13.9f)
        moveTo(12f, 9f); lineTo(12f, 4f); lineTo(16.5f, 8f); lineTo(14.6f, 9.7f)
        moveTo(7f, 16.5f); lineTo(10.5f, 13.4f)
        moveTo(4f, 4f); lineTo(20f, 20f)
    }

    val Play = icon("play") {
        moveTo(8f, 5.5f); lineTo(18.5f, 12f); lineTo(8f, 18.5f); close()
    }

    val Pause = icon("pause") {
        moveTo(9f, 6f); lineTo(9f, 18f)
        moveTo(15f, 6f); lineTo(15f, 18f)
    }

    val SkipNext = icon("skip_next") {
        moveTo(6.5f, 6.5f); lineTo(14.5f, 12f); lineTo(6.5f, 17.5f); close()
        moveTo(17.5f, 6.5f); lineTo(17.5f, 17.5f)
    }

    val SkipPrevious = icon("skip_previous") {
        moveTo(17.5f, 6.5f); lineTo(9.5f, 12f); lineTo(17.5f, 17.5f); close()
        moveTo(6.5f, 6.5f); lineTo(6.5f, 17.5f)
    }

    // --- Menus & settings ---

    val Move = icon("move") {
        moveTo(12f, 3f); lineTo(12f, 21f)
        moveTo(3f, 12f); lineTo(21f, 12f)
        moveTo(9.5f, 5.5f); lineTo(12f, 3f); lineTo(14.5f, 5.5f)
        moveTo(9.5f, 18.5f); lineTo(12f, 21f); lineTo(14.5f, 18.5f)
        moveTo(5.5f, 9.5f); lineTo(3f, 12f); lineTo(5.5f, 14.5f)
        moveTo(18.5f, 9.5f); lineTo(21f, 12f); lineTo(18.5f, 14.5f)
    }

    val Pin = icon("pin") {
        // Dock tray with an arrow dropping into it
        moveTo(4f, 15f); lineTo(4f, 19f); lineTo(20f, 19f); lineTo(20f, 15f)
        moveTo(12f, 4f); lineTo(12f, 14f)
        moveTo(8.5f, 10.5f); lineTo(12f, 14f); lineTo(15.5f, 10.5f)
    }

    val Unpin = icon("unpin") {
        moveTo(4f, 15f); lineTo(4f, 19f); lineTo(20f, 19f); lineTo(20f, 15f)
        moveTo(12f, 14f); lineTo(12f, 4f)
        moveTo(8.5f, 7.5f); lineTo(12f, 4f); lineTo(15.5f, 7.5f)
    }

    val EyeOff = icon("eye_off") {
        moveTo(2.5f, 12f); curveTo(5f, 7.5f, 8.5f, 5.5f, 12f, 5.5f)
        curveTo(15.5f, 5.5f, 19f, 7.5f, 21.5f, 12f)
        curveTo(19f, 16.5f, 15.5f, 18.5f, 12f, 18.5f)
        curveTo(8.5f, 18.5f, 5f, 16.5f, 2.5f, 12f)
        close()
        ring(12f, 12f, 3f)
        moveTo(4f, 4f); lineTo(20f, 20f)
    }

    val Info = icon("info") {
        ring(12f, 12f, 9f)
        moveTo(12f, 11f); lineTo(12f, 16.5f)
        moveTo(12f, 7.6f); lineTo(12f, 7.7f)
    }

    val Trash = icon("trash") {
        moveTo(4f, 6.5f); lineTo(20f, 6.5f)
        moveTo(9.5f, 6.5f); lineTo(9.5f, 4f); lineTo(14.5f, 4f); lineTo(14.5f, 6.5f)
        moveTo(6f, 6.5f); lineTo(7f, 20f); lineTo(17f, 20f); lineTo(18f, 6.5f)
        moveTo(10f, 10.5f); lineTo(10f, 16f)
        moveTo(14f, 10.5f); lineTo(14f, 16f)
    }

    val Home = icon("home") {
        moveTo(4f, 10.5f); lineTo(12f, 4f); lineTo(20f, 10.5f)
        moveTo(6f, 9f); lineTo(6f, 20f); lineTo(18f, 20f); lineTo(18f, 9f)
        moveTo(10f, 20f); lineTo(10f, 14f); lineTo(14f, 14f); lineTo(14f, 20f)
    }

    val Film = icon("film") {
        moveTo(3.5f, 5.5f); lineTo(20.5f, 5.5f); lineTo(20.5f, 18.5f); lineTo(3.5f, 18.5f); close()
        moveTo(3.5f, 12f); lineTo(20.5f, 12f)
        moveTo(8f, 5.5f); lineTo(8f, 18.5f)
        moveTo(16f, 5.5f); lineTo(16f, 18.5f)
    }

    val Mountains = icon("mountains") {
        moveTo(2.5f, 19f); lineTo(9f, 8f); lineTo(13f, 14.5f); lineTo(15.5f, 11f); lineTo(21.5f, 19f); close()
        moveTo(7f, 10.5f); lineTo(9f, 12f); lineTo(11f, 10.8f)
    }

    val Timer = icon("timer") {
        ring(12f, 13f, 8f)
        moveTo(12f, 13f); lineTo(12f, 8.5f)
        moveTo(9.5f, 2.5f); lineTo(14.5f, 2.5f)
    }

    val Eye = icon("eye") {
        moveTo(2.5f, 12f); curveTo(5f, 7.5f, 8.5f, 5.5f, 12f, 5.5f)
        curveTo(15.5f, 5.5f, 19f, 7.5f, 21.5f, 12f)
        curveTo(19f, 16.5f, 15.5f, 18.5f, 12f, 18.5f)
        curveTo(8.5f, 18.5f, 5f, 16.5f, 2.5f, 12f)
        close()
        ring(12f, 12f, 3f)
    }

    val Music = icon("music") {
        moveTo(9f, 17.5f); lineTo(9f, 5.5f); lineTo(19f, 3.5f); lineTo(19f, 15.5f)
        ring(6.75f, 17.5f, 2.25f)
        ring(16.75f, 15.5f, 2.25f)
    }

    // --- Weather ---

    /** Cloud outline, [dy] shifts it vertically so rain/snow fit underneath. */
    private fun PathBuilder.cloud(dy: Float) {
        moveTo(7.5f, 17f + dy); lineTo(17f, 17f + dy)
        curveTo(19.2f, 17f + dy, 20.5f, 15.4f + dy, 20.5f, 13.6f + dy)
        curveTo(20.5f, 11.8f + dy, 19.1f, 10.4f + dy, 17.4f, 10.3f + dy)
        curveTo(16.8f, 7.8f + dy, 14.6f, 6f + dy, 12f, 6f + dy)
        curveTo(9.2f, 6f + dy, 6.9f, 8f + dy, 6.5f, 10.6f + dy)
        curveTo(4.8f, 10.9f + dy, 3.5f, 12.3f + dy, 3.5f, 14f + dy)
        curveTo(3.5f, 15.7f + dy, 5f, 17f + dy, 7.5f, 17f + dy)
        close()
    }

    private fun PathBuilder.ring(cx: Float, cy: Float, r: Float) {
        moveTo(cx + r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
    }

    val Sun = icon("sun") {
        ring(12f, 12f, 4f)
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val c = Math.cos(a).toFloat()
            val sn = Math.sin(a).toFloat()
            moveTo(12f + 7f * c, 12f + 7f * sn); lineTo(12f + 9f * c, 12f + 9f * sn)
        }
    }

    val Moon = icon("moon") {
        moveTo(19.5f, 14.5f)
        arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 9.5f, y1 = 4.5f)
        arcTo(6.5f, 6.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 19.5f, y1 = 14.5f)
        close()
    }

    val Cloud = icon("cloud") { cloud(0f) }

    val PartlyCloudy = icon("partly_cloudy") {
        // Sun peeking from behind the cloud's upper left
        moveTo(5.2f, 9.2f)
        arcTo(3.6f, 3.6f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 11.8f, y1 = 6.4f)
        moveTo(8.5f, 1.8f); lineTo(8.5f, 2.8f)
        moveTo(3.2f, 4f); lineTo(3.9f, 4.7f)
        moveTo(1.8f, 8.5f); lineTo(2.8f, 8.5f)
        moveTo(13.8f, 4f); lineTo(13.1f, 4.7f)
        cloud(2f)
    }

    val Rain = icon("rain") {
        cloud(-3f)
        moveTo(8f, 17f); lineTo(7f, 20f)
        moveTo(12f, 17f); lineTo(11f, 20f)
        moveTo(16f, 17f); lineTo(15f, 20f)
    }

    val Snow = icon("snow") {
        cloud(-3f)
        moveTo(8f, 18f); lineTo(8f, 18.1f)
        moveTo(12f, 19.5f); lineTo(12f, 19.6f)
        moveTo(16f, 18f); lineTo(16f, 18.1f)
    }

    val Storm = icon("storm") {
        cloud(-3f)
        moveTo(12.5f, 15f); lineTo(10.5f, 18.5f); lineTo(13.5f, 18.5f); lineTo(11.5f, 22f)
    }

    val Fog = icon("fog") {
        moveTo(4f, 8f); lineTo(20f, 8f)
        moveTo(6f, 12f); lineTo(18f, 12f)
        moveTo(4f, 16f); lineTo(20f, 16f)
    }

    val Gear = icon("gear") {
        // Eight-tooth outline around a hub
        moveTo(10.3f, 3.5f); lineTo(13.7f, 3.5f); lineTo(14.3f, 5.9f); lineTo(16.1f, 6.9f)
        lineTo(18.5f, 6.2f); lineTo(20.2f, 9.1f); lineTo(18.4f, 10.9f); lineTo(18.4f, 13.1f)
        lineTo(20.2f, 14.9f); lineTo(18.5f, 17.8f); lineTo(16.1f, 17.1f); lineTo(14.3f, 18.1f)
        lineTo(13.7f, 20.5f); lineTo(10.3f, 20.5f); lineTo(9.7f, 18.1f); lineTo(7.9f, 17.1f)
        lineTo(5.5f, 17.8f); lineTo(3.8f, 14.9f); lineTo(5.6f, 13.1f); lineTo(5.6f, 10.9f)
        lineTo(3.8f, 9.1f); lineTo(5.5f, 6.2f); lineTo(7.9f, 6.9f); lineTo(9.7f, 5.9f); close()
        moveTo(12f, 9f)
        arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 11.99f, y1 = 9f)
    }
}
