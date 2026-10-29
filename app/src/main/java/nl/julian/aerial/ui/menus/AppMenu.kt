package nl.julian.aerial.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.ui.Text
import nl.julian.aerial.data.AppEntry
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.Symbols

enum class AppAction { Open, Move, Pin, Unpin, Rename, Look, AddToFolder, Colour, DeleteFolder, Hide, Info, Uninstall }

/** Long-press menu for one app: a Material dialog centred over the dimmed home screen. */
@Composable
fun AppMenu(
    app: AppEntry,
    inDock: Boolean,
    onAction: (AppAction) -> Unit,
    onDismiss: () -> Unit,
    /** Shown with its icon instead of its banner (null: the app has no banner to switch to). */
    iconOnly: Boolean? = null,
    /** A folder tile: folder actions instead of app ones. */
    folder: Boolean = false,
) {
    val first = remember { FocusRequester() }
    Overlay(onDismiss) {
        Box {
            Column(
                Modifier
                    .width(360.dp)
                    .background(LocalSurfaces.current.default, MaterialTheme.shapes.extraLarge)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    app.label, style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 10.dp),
                )
                fun act(a: AppAction) = { onAction(a) }
                MenuRow(Symbols.Play, "Open", act(AppAction.Open), focusRequester = first)
                MenuRow(Symbols.Move, "Move", act(AppAction.Move))
                if (inDock) MenuRow(Symbols.Unpin, "Remove from dock", act(AppAction.Unpin))
                else MenuRow(Symbols.Pin, "Pin to dock", act(AppAction.Pin))
                MenuRow(Symbols.Edit, "Rename", act(AppAction.Rename))
                if (folder) {
                    MenuRow(Symbols.Palette, "Change colour", act(AppAction.Colour))
                    MenuRow(Symbols.Delete, "Delete folder", act(AppAction.DeleteFolder))
                } else {
                    iconOnly?.let { MenuRow(Symbols.Show, if (it) "Show its banner" else "Show icon and name", act(AppAction.Look)) }
                    MenuRow(Symbols.Folder, "Add to folder", act(AppAction.AddToFolder))
                    MenuRow(Symbols.Hide, "Hide", act(AppAction.Hide))
                    MenuRow(Symbols.Info, "App info", act(AppAction.Info))
                    if (!app.isSystem) MenuRow(Symbols.Delete, "Uninstall", act(AppAction.Uninstall))
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}
