package nl.julian.aerial.ui

import nl.julian.aerial.data.FocusColor
import kotlinx.coroutines.launch
import com.materialkolor.score.Score
import com.materialkolor.quantize.QuantizerCelebi
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.data.AppEntry

/**
 * 16:9 rounded app tile. [tinted]: a theme-coloured tile with the app's
 * icon and name, so every app sits in the same palette as the aerial.
 * Otherwise the app's own leanback banner (icon + name when it has none).
 */
@Composable
fun AppTile(
    app: AppEntry,
    radius: Dp,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
    tinted: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    /** Lifted with a primary-colour ring while the user is moving it. */
    moving: Boolean = false,
    /** Tilt in degrees toward the direction focus arrived from; 0 = flat. */
    depth: Float = 0f,
    focusColor: FocusColor = FocusColor.White,
) {
    val showBanner = !tinted && app.banner != null
    val ring = when (focusColor) {
        FocusColor.White -> null
        FocusColor.Aerial -> MaterialTheme.colorScheme.primary
        FocusColor.App -> remember(app.banner, app.icon) { (app.banner ?: app.icon)?.let(::accentOf) }
    }
    val scope = rememberCoroutineScope()
    val tilt = remember { Animatable(0f) }
    val tiltX = remember { Animatable(0f) }
    FocusSurface(
        ring = ring,
        onClick = onLaunch,
        shape = RoundedCornerShape(radius),
        // Expressive touch: corners open up slightly as the tile lifts.
        focusedShape = RoundedCornerShape(radius + 6.dp),
        modifier = modifier
            .aspectRatio(16f / 9f)
            .then(
                if (depth == 0f) Modifier else Modifier
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            // Lean into the move, then spring back flat.
                            scope.launch { tilt.snapTo(FocusMove.dx * depth); tilt.animateTo(0f, spring(0.45f, 120f)) }
                            scope.launch { tiltX.snapTo(-FocusMove.dy * depth); tiltX.animateTo(0f, spring(0.45f, 120f)) }
                        }
                    }
                    .graphicsLayer {
                        rotationY = tilt.value
                        rotationX = tiltX.value
                        cameraDistance = 14 * density
                    }
            ),
        style = if (showBanner) FocusStyle.Ring else FocusStyle.Invert,
        onLongClick = onLongPress,
        highlighted = moving,
    ) {
        if (showBanner) {
            Image(
                bitmap = app.banner!!,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)),
                    )
                }
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Keeps a grid row's columns aligned when the last row is short. */
@Composable
fun EmptyTileSlot(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Spacer(modifier.aspectRatio(16f / 9f))
}

/** The last D-pad move, set by MainActivity, so a tile can lean into it when it takes focus. */
object FocusMove {
    @Volatile var dx = 0f
    @Volatile var dy = 0f
}

/** The banner's most characteristic colour (Material's quantize + score), for the focus ring. */
private fun accentOf(art: ImageBitmap): Color? = runCatching {
    val small = Bitmap.createScaledBitmap(art.asAndroidBitmap(), 32, 18, true)
    val pixels = IntArray(32 * 18).also { small.getPixels(it, 0, 32, 0, 0, 32, 18) }
    Score.score(QuantizerCelebi.quantize(pixels, 16)).firstOrNull()?.let { Color(it) }
}.getOrNull()
