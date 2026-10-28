package nl.julian.aerial.ui.widgets

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import nl.julian.aerial.data.NowPlaying
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.theme.topHighlight
import nl.julian.aerial.ui.FocusSurface
import kotlin.math.PI
import kotlin.math.sin

/** Media card on one line: album art, title and wavy progress, then transport buttons. */
@Composable
fun NowPlayingCard(media: NowPlaying, radius: Dp, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val inset = 10.dp
    Row(
        modifier.background(tileColor(), RoundedCornerShape(radius)).topHighlight(RoundedCornerShape(radius)).padding(inset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(84.dp).clip(RoundedCornerShape(radius - inset)).background(cs.secondaryContainer)) {
            if (media.art != null) {
                Image(media.art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (media.artUri != null) {
                AsyncImage(media.artUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        // Title and artist get the full width; progress and buttons share the line below.
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                media.title, style = MaterialTheme.typography.titleSmall, color = cs.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            media.artist?.let {
                Text(
                    it, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                WavyProgress(media, Modifier.weight(1f).height(10.dp))
                Spacer(Modifier.width(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SkipButton(Symbols.SkipPrevious, "Previous") { media.previous() }
                    PlayPauseButton(media.playing) { media.playPause() }
                    SkipButton(Symbols.SkipNext, "Next") { media.next() }
                }
            }
        }
    }
}

/**
 * M3 Expressive wavy progress: the elapsed part is a travelling wave (flat
 * when paused), then a gap, the remaining track, and a stop dot at the end.
 * Position is read at draw time, so it advances without recomposition.
 */
@Composable
private fun WavyProgress(media: NowPlaying, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val phase = if (media.playing) {
        val t = rememberInfiniteTransition(label = "wave")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "phase").value
    } else 0f
    val path = remember { Path() }
    Canvas(modifier) {
        val stroke = 4.dp.toPx()
        val gap = 4.dp.toPx()
        val amp = if (media.playing) 2.5.dp.toPx() else 0f
        val wavelength = 20.dp.toPx()
        val mid = size.height / 2
        val fraction = if (media.duration > 0) (media.positionNow().toFloat() / media.duration).coerceIn(0f, 1f) else 0f
        val split = size.width * fraction

        path.reset()
        path.moveTo(stroke / 2, mid)
        var x = stroke / 2
        while (x <= split) {
            path.lineTo(x, mid + amp * sin(2 * PI * (x / wavelength - phase)).toFloat())
            x += 2f
        }
        drawPath(path, cs.primary, style = Stroke(stroke, cap = StrokeCap.Round))
        val trackStart = split + gap + stroke
        if (trackStart < size.width - stroke) {
            drawLine(cs.secondaryContainer, Offset(trackStart, mid), Offset(size.width - stroke / 2, mid), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        drawCircle(cs.primary, radius = stroke / 2, center = Offset(size.width - stroke / 2, mid))
    }
}

/** Filled primary button that morphs from a pill (paused) to a rounded square (playing). */
@Composable
private fun PlayPauseButton(playing: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val corner by animateDpAsState(if (playing) 12.dp else 18.dp, spring(dampingRatio = 0.6f), label = "morph")
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        container = cs.primary,
        modifier = Modifier.width(52.dp).height(36.dp),
        focusedScale = 1.1f,
    ) { focused ->
        Icon(
            painterResource(if (playing) Symbols.Pause else Symbols.Play),
            contentDescription = if (playing) "Pause" else "Play",
            tint = if (focused) cs.inverseOnSurface else cs.onPrimary,
            modifier = Modifier.align(Alignment.Center).size(22.dp),
        )
    }
}

@Composable
private fun SkipButton(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    FocusSurface(
        onClick = onClick,
        shape = CircleShape,
        container = LocalSurfaces.current.highest,
        modifier = Modifier.size(34.dp),
        focusedScale = 1.12f,
    ) {
        Icon(painterResource(icon), contentDescription = label, modifier = Modifier.align(Alignment.Center).size(18.dp))
    }
}
