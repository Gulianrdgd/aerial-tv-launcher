package nl.julian.aerial.ui.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import nl.julian.aerial.data.NowPlaying
import nl.julian.aerial.theme.AerialType
import nl.julian.aerial.theme.LineIcons
import nl.julian.aerial.theme.LocalLauncherColors
import nl.julian.aerial.theme.glassCore
import nl.julian.aerial.ui.FocusSurface
import kotlin.math.PI
import kotlin.math.sin

/** Monet's media card: album art, title, a living wavy progress line, transport keys. */
@Composable
fun NowPlayingCard(media: NowPlaying, radius: Dp, modifier: Modifier = Modifier) {
    val colors = LocalLauncherColors.current
    val inset = 10.dp
    Row(
        modifier.glassCore(RoundedCornerShape(radius), colors.tile).padding(inset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val artShape = RoundedCornerShape(radius - inset)
        Box(Modifier.fillMaxHeight().aspectRatio(1f).clip(artShape).background(colors.panel)) {
            if (media.art != null) {
                Image(media.art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (media.artUri != null) {
                AsyncImage(media.artUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Column(
            Modifier.weight(1f).padding(start = 14.dp, end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                media.title, style = AerialType.pillTitle, color = colors.content,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            media.artist?.let {
                Text(it, style = AerialType.pillSub, color = colors.contentMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            WavyProgress(media, Modifier.padding(vertical = 10.dp).fillMaxWidth().height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TransportKey(LineIcons.SkipPrevious, "Previous", small = true) { media.previous() }
                TransportKey(if (media.playing) LineIcons.Pause else LineIcons.Play, "Play/pause", small = false) {
                    media.playPause()
                }
                TransportKey(LineIcons.SkipNext, "Next", small = true) { media.next() }
            }
        }
    }
}

/**
 * Elapsed part drawn as a travelling sine wave (still when paused), the rest
 * as a flat track. Position is read at draw time, so it advances smoothly
 * without recomposition.
 */
@Composable
private fun WavyProgress(media: NowPlaying, modifier: Modifier) {
    val colors = LocalLauncherColors.current
    val phase = if (media.playing) {
        val t = rememberInfiniteTransition(label = "wave")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "phase").value
    } else 0f
    val path = remember { Path() }
    Canvas(modifier) {
        val stroke = 3.dp.toPx()
        val amp = if (media.playing) 2.5.dp.toPx() else 0f
        val wavelength = 18.dp.toPx()
        val mid = size.height / 2
        val fraction = if (media.duration > 0) (media.positionNow().toFloat() / media.duration).coerceIn(0f, 1f) else 0f
        val split = size.width * fraction

        path.reset()
        path.moveTo(0f, mid)
        var x = 0f
        while (x <= split) {
            path.lineTo(x, mid + amp * sin(2 * PI * (x / wavelength - phase)).toFloat())
            x += 2f
        }
        drawPath(path, colors.accent, style = Stroke(stroke, cap = StrokeCap.Round))
        drawLine(
            colors.content.copy(alpha = 0.25f), Offset(split + stroke * 2, mid), Offset(size.width, mid),
            strokeWidth = stroke, cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun TransportKey(icon: ImageVector, label: String, small: Boolean, onClick: () -> Unit) {
    val colors = LocalLauncherColors.current
    val main = !small
    FocusSurface(
        onClick = onClick,
        shape = CircleShape,
        tint = if (main) colors.accent else colors.panel,
        modifier = if (main) Modifier.width(48.dp).height(36.dp) else Modifier.size(30.dp),
        focusedScale = 1.12f,
    ) {
        Icon(
            icon, contentDescription = label,
            tint = if (main) colors.onAccent else colors.content,
            modifier = Modifier.align(Alignment.Center).size(if (main) 20.dp else 16.dp),
        )
    }
}
