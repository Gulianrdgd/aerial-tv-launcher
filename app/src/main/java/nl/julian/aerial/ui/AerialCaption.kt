package nl.julian.aerial.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import nl.julian.aerial.data.ClockFormat
import nl.julian.aerial.theme.FluidEasing
import nl.julian.aerial.ui.widgets.is24Hour
import nl.julian.aerial.theme.GoogleSansFlex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Soft = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 2f), blurRadius = 16f)

/**
 * Screensaver overlay, Apple TV style: the clip's place name bottom-left and
 * a small clock top-right, fading in after the UI has gone.
 */
@Composable
fun AerialCaption(title: String?, visible: Boolean, clockFormat: ClockFormat, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        if (visible) 1f else 0f,
        tween(durationMillis = if (visible) 1400 else 300, delayMillis = if (visible) 900 else 0, easing = FluidEasing),
        label = "caption",
    )
    if (alpha == 0f) return

    val context = LocalContext.current
    val now by produceState(Date()) {
        while (true) {
            value = Date()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val time = SimpleDateFormat(if (clockFormat.is24Hour(context)) "HH:mm" else "h:mm", Locale.getDefault()).format(now)

    Box(modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 48.dp).graphicsLayer { this.alpha = alpha }) {
        Text(
            time,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = GoogleSansFlex, fontWeight = FontWeight(500), fontSize = 22.sp, shadow = Soft,
            ),
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.align(Alignment.TopEnd),
        )
        title?.let {
            Text(
                it,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = GoogleSansFlex, fontWeight = FontWeight(300), fontSize = 30.sp,
                    letterSpacing = 0.01.em, shadow = Soft,
                ),
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}
