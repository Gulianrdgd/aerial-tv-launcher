package nl.julian.aerial.ui

import nl.julian.aerial.ui.widgets.timeInWords
import nl.julian.aerial.ui.widgets.AnalogClock
import nl.julian.aerial.data.ClockFace
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import nl.julian.aerial.ui.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import nl.julian.aerial.data.AmbientInfo
import nl.julian.aerial.data.ClockFormat
import nl.julian.aerial.data.NowPlaying
import nl.julian.aerial.data.Weather
import nl.julian.aerial.theme.FluidEasing
import nl.julian.aerial.theme.GoogleSansFlex
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.ui.widgets.is24Hour
import nl.julian.aerial.ui.widgets.sceneryLabel
import nl.julian.aerial.ui.widgets.skyIcon
import nl.julian.aerial.ui.widgets.skyTint
import nl.julian.aerial.wallpaper.Aerial
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Soft = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 2f), blurRadius = 18f)
private val Faint = Color.White.copy(alpha = 0.78f)

private fun style(size: Int, weight: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = GoogleSansFlex, fontWeight = FontWeight(weight), fontSize = size.sp,
    letterSpacing = tracking.em, shadow = Soft, color = Color.White,
)

/**
 * The screensaver overlay, laid out like Chromecast's ambient mode: the
 * artwork's title and source with the time and weather below, bottom left;
 * what's playing, top right. Soft shades keep the text readable on bright
 * clips.
 */
@Composable
fun Ambient(
    clip: Aerial?,
    weather: Weather?,
    media: NowPlaying?,
    clockFormat: ClockFormat,
    visible: Boolean,
    info: AmbientInfo = AmbientInfo.All,
    /** Show where the camera is (for clips that have captions) instead of the scenery. */
    showPlaces: Boolean = false,
    shiftText: Boolean = false,
    face: ClockFace = ClockFace.Standard,
) {
    val alpha by animateFloatAsState(
        if (visible) 1f else 0f,
        tween(durationMillis = if (visible) 1400 else 300, delayMillis = if (visible) 900 else 0, easing = FluidEasing),
        label = "ambient",
    )
    if (alpha == 0f || info == AmbientInfo.None) return
    val all = info == AmbientInfo.All

    // Burn-in protection: a new small offset every three minutes, eased in.
    val shift by produceState(Offset.Zero, shiftText) {
        value = Offset.Zero
        while (shiftText) {
            delay(180_000)
            value = Offset((-16..16).random().toFloat(), (-12..12).random().toFloat())
        }
    }
    val dx by animateFloatAsState(shift.x, tween(4_000, easing = FluidEasing), label = "dx")
    val dy by animateFloatAsState(shift.y, tween(4_000, easing = FluidEasing), label = "dy")
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            this.alpha = alpha
            translationX = dx.dp.toPx()
            translationY = dy.dp.toPx()
        }
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = if (media != null && all) 0.3f else 0f),
                    0.2f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.4f),
                )
            )
        )
        media?.takeIf { all }?.let {
            Box(Modifier.align(Alignment.TopEnd).padding(horizontal = 48.dp, vertical = 28.dp)) { AmbientMedia(it) }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 56.dp, vertical = 28.dp)) {
                clip?.takeIf { all }?.let {
                    Text(it.title, style = style(22, 500), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        (if (showPlaces) placeNow(it) else null) ?: sceneryLabel(it.kind),
                        style = style(15, 400, 0.02).copy(color = Faint), maxLines = 1,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AmbientClock(clockFormat, face)
                    weather?.takeIf { all }?.let {
                        Spacer(Modifier.width(20.dp))
                        Icon(
                            painterResource(skyIcon(it)), contentDescription = it.sky.name,
                            tint = skyTint(it), modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${it.temperature}°", style = style(26, 400))
                    }
                }
        }
    }
}

/** The clip's current place caption, following the time since it started. */
@Composable
private fun placeNow(clip: Aerial): String? {
    if (clip.places.isEmpty()) return null
    val caption by produceState(clip.placeAt(0), clip) {
        val start = System.currentTimeMillis()
        while (true) {
            delay(1_000)
            value = clip.placeAt(((System.currentTimeMillis() - start) / 1000).toInt())
        }
    }
    return caption
}

@Composable
private fun AmbientClock(format: ClockFormat, face: ClockFace) {
    val context = LocalContext.current
    val now by produceState(Date()) {
        while (true) {
            value = Date()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val time = SimpleDateFormat(if (format.is24Hour(context)) "HH:mm" else "h:mm", Locale.getDefault()).format(now)
    when (face) {
        ClockFace.Standard -> Text(time, style = style(26, 500).copy(fontFeatureSettings = "tnum"))
        ClockFace.Thin -> Text(time, style = style(64, 200).copy(fontFeatureSettings = "tnum"))
        ClockFace.Analog -> AnalogClock(now, 56.dp, Color.White, Color.White, Faint)
        ClockFace.Words -> Text(timeInWords(now), style = style(26, 700))
    }
}

/** What's playing: album art with title and artist, on a dark translucent chip. */
@Composable
private fun AmbientMedia(media: NowPlaying) {
    Row(
        Modifier
            .widthIn(max = 420.dp)
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f))) {
            if (media.art != null) {
                Image(media.art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (media.artUri != null) {
                AsyncImage(media.artUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Column(Modifier.padding(end = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(if (media.playing) Symbols.Music else Symbols.Pause), null,
                    tint = Faint, modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (media.playing) "Now playing" else "Paused", style = style(14, 500).copy(color = Faint))
            }
            Text(media.title, style = style(19, 500), maxLines = 1, overflow = TextOverflow.Ellipsis)
            media.artist?.let {
                Text(it, style = style(15, 400).copy(color = Faint), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
