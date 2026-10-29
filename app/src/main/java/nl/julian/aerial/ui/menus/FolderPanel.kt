package nl.julian.aerial.ui.menus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.data.AppEntry
import nl.julian.aerial.data.Folder
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.theme.SECTION_TILE_RADIUS
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.ui.AppTile

/** Dock and grid entries for folders use this in place of a package name. */
const val FOLDER_PREFIX = "folder:"

/** A folder's tile: its name, with a folder glyph on a disc of the folder's colour. */
fun Folder.entry(context: Context): AppEntry {
    val size = 96
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = this@entry.color })
    ContextCompat.getDrawable(context, Symbols.Folder)?.mutate()?.apply { setTint(android.graphics.Color.WHITE) }
        ?.toBitmap(56, 56)?.let { canvas.drawBitmap(it, 20f, 20f, null) }
    return AppEntry(FOLDER_PREFIX + id, name, banner = null, icon = bmp.asImageBitmap(), stamp = 0, isSystem = true)
}

/** An open folder: its apps as tiles, four to a row. Hold OK on one to take it out of the folder. */
@Composable
fun FolderPanel(
    folder: Folder,
    apps: List<AppEntry>,
    tinted: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onRemove: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    Overlay(onDismiss) {
        Column(
            Modifier
                .width(900.dp)
                .background(LocalSurfaces.current.default, MaterialTheme.shapes.extraLarge)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(folder.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            if (apps.isEmpty()) {
                Text(
                    "Empty. Hold OK on an app and choose Add to folder.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            apps.chunked(4).forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEachIndexed { c, app ->
                        AppTile(
                            app, radius = SECTION_TILE_RADIUS, tinted = tinted,
                            onLaunch = { onLaunch(app) }, onLongPress = { onRemove(app) },
                            modifier = Modifier.weight(1f).then(if (r == 0 && c == 0) Modifier.focusRequester(first) else Modifier),
                        )
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (apps.isNotEmpty()) {
                Text(
                    "Hold OK on an app to take it out of the folder.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}

/** "Add to folder": the existing folders, or a new one. */
@Composable
fun FolderChooser(folders: List<Folder>, onPick: (Folder?) -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    SidePanel("Add to folder", onDismiss, initialFocus = first) {
        MenuRow(Symbols.NewFolder, "New folder", onClick = { onPick(null) }, focusRequester = first)
        folders.forEach { f ->
            MenuRow(Symbols.Folder, f.name, value = "${f.apps.size} apps", onClick = { onPick(f) })
        }
    }
}
