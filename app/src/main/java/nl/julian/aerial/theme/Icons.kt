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
