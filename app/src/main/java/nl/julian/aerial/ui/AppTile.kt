package nl.julian.aerial.ui

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.data.AppEntry

/** 16:9 rounded app tile: the app's leanback banner, or its icon + name. */
@Composable
fun AppTile(
    app: AppEntry,
    radius: Dp,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    /** Lifted with a primary-colour ring while the user is moving it. */
    moving: Boolean = false,
) {
    FocusSurface(
        onClick = onLaunch,
        shape = RoundedCornerShape(radius),
        // Expressive touch: corners open up slightly as the tile lifts.
        focusedShape = RoundedCornerShape(radius + 6.dp),
        modifier = modifier.aspectRatio(16f / 9f),
        style = if (app.banner != null) FocusStyle.Ring else FocusStyle.Invert,
        onLongClick = onLongPress,
        highlighted = moving,
    ) {
        if (app.banner != null) {
            Image(
                bitmap = app.banner,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) {
                if (app.icon != null) {
                    Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                }
                Text(
                    app.label,
                    style = MaterialTheme.typography.labelLarge,
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
