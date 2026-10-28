package nl.julian.aerial.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.theme.LocalSurfaces

/**
 * Google TV-style side panel on the right: a Material surface container
 * with a headline and a scrolling column of rows. [initialFocus] is
 * focused when it opens.
 */
@Composable
fun SidePanel(
    title: String,
    onDismiss: () -> Unit,
    initialFocus: FocusRequester?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Overlay(onDismiss, contentAlignment = Alignment.CenterEnd) {
        val scroll = rememberScrollState()
        val panel = LocalSurfaces.current.default
        Box(Modifier.padding(horizontal = 24.dp, vertical = 16.dp).width(380.dp).fillMaxHeight()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(panel, MaterialTheme.shapes.extraLarge)
                    .verticalScroll(scroll)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 6.dp),
                )
                content()
            }
            // More rows below: fade the bottom edge so the cut-off row reads as "scroll for more".
            if (scroll.canScrollForward) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, panel))),
                )
            }
        }
    }
    LaunchedEffect(Unit) { initialFocus?.let { runCatching { it.requestFocus() } } }
}
