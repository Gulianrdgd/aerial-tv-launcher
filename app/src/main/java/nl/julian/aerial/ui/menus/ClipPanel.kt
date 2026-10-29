package nl.julian.aerial.ui.menus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.wallpaper.Aerial

/** Opened by holding OK on the screensaver: favourite the clip on screen, or never show it again. */
@Composable
fun ClipPanel(
    clip: Aerial,
    favourite: Boolean,
    onFavourite: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    SidePanel(clip.title, onDismiss, initialFocus = first) {
        MenuRow(
            Symbols.Favorite, if (favourite) "Remove from favourites" else "Add to favourites",
            onClick = onFavourite, focusRequester = first,
        )
        MenuRow(Symbols.Block, "Never show again", onClick = onSkip, value = "Undo in Settings")
    }
}
