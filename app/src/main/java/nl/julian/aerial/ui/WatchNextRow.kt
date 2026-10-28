package nl.julian.aerial.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.emptyFlow
import nl.julian.aerial.data.WatchNextItem
import nl.julian.aerial.data.WatchNextRepository
import nl.julian.aerial.theme.AerialType
import nl.julian.aerial.theme.LineIcons
import nl.julian.aerial.theme.LocalLauncherColors
import nl.julian.aerial.ui.widgets.startSafely

private const val VISIBLE_CARDS = 3
private val GAP = 20.dp

/**
 * Monet's big top row: what you were watching, newest first. Without
 * TV-listings access it collapses to one pill that asks for it.
 */
@Composable
fun WatchNextRow(
    radius: Dp,
    modifier: Modifier = Modifier,
    /** Distance to the screen edges; the row scrolls out to them instead of clipping at its margin. */
    bleed: Dp = 0.dp,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Re-checked on every resume: access can be granted in Settings or via adb.
    var hasAccess by remember { mutableStateOf(WatchNextRepository.hasAccess(context)) }
    LaunchedEffect(lifecycle) {
        lifecycle.currentStateFlow.collect {
            if (it == Lifecycle.State.RESUMED) hasAccess = WatchNextRepository.hasAccess(context)
        }
    }
    val askAccess = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasAccess = it
    }

    val itemsFlow = remember(hasAccess) {
        if (hasAccess) WatchNextRepository.items(context.applicationContext) else emptyFlow()
    }
    val items by itemsFlow.collectAsStateWithLifecycle(emptyList())

    when {
        !hasAccess -> AccessPill(
            onClick = { askAccess.launch(WatchNextRepository.PERMISSION) },
            modifier = modifier,
        )
        items.isNotEmpty() -> BoxWithConstraints(modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth - GAP * (VISIBLE_CARDS - 1)) / VISIBLE_CARDS
            LazyRow(
                Modifier.bleed(bleed).focusRestorer(),
                contentPadding = PaddingValues(horizontal = bleed),
                horizontalArrangement = Arrangement.spacedBy(GAP),
            ) {
                items(items, key = { it.id }) { item ->
                    WatchNextCard(
                        item, radius, Modifier.width(cardWidth),
                        onClick = { WatchNextRepository.launchIntent(context, item)?.let(context::startSafely) },
                    )
                }
            }
        }
    }
}

/** Widens the element by [h] on each side without moving its content. */
private fun Modifier.bleed(h: Dp) = layout { measurable, constraints ->
    val extra = (h * 2).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.minWidth + extra)
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}

@Composable
private fun WatchNextCard(item: WatchNextItem, radius: Dp, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalLauncherColors.current
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(radius),
        modifier = modifier.aspectRatio(16f / 9f),
        focusedScale = 1.06f,
    ) {
        AsyncImage(
            model = item.imageUri,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Bottom shade so the title reads on any artwork.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.45f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.72f),
                )
            )
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                item.title,
                style = AerialType.pillTitle.copy(fontSize = 17.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.subtitle?.let {
                Text(
                    it, style = AerialType.pillSub, color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            item.progress?.let { p ->
                Box(
                    Modifier.padding(top = 10.dp).fillMaxWidth().height(3.dp)
                        .background(Color.White.copy(alpha = 0.22f), CircleShape)
                ) {
                    Box(Modifier.fillMaxWidth(p).height(3.dp).background(colors.accent, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun AccessPill(onClick: () -> Unit, modifier: Modifier) {
    val colors = LocalLauncherColors.current
    Row(modifier.fillMaxWidth()) {
        FocusSurface(onClick = onClick, shape = CircleShape, modifier = Modifier.height(56.dp), focusedScale = 1.05f) {
            Row(
                Modifier.fillMaxHeight().padding(start = 8.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).background(colors.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LineIcons.Play, null, tint = colors.onAccent, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text("Show Continue Watching", style = AerialType.pillTitle, color = colors.content)
                    Text(
                        "Allow access to what your apps are playing",
                        style = AerialType.pillSub, color = colors.contentMuted,
                    )
                }
            }
        }
    }
}
