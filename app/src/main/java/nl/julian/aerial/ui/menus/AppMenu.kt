package nl.julian.aerial.ui.menus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import nl.julian.aerial.data.AppEntry
import nl.julian.aerial.theme.AerialType
import nl.julian.aerial.theme.LineIcons
import nl.julian.aerial.theme.LocalLauncherColors
import nl.julian.aerial.theme.glass

enum class AppAction { Open, Move, Pin, Unpin, Hide, Info, Uninstall }

/** Long-press menu for one app, a glass card centred over the dimmed home screen. */
@Composable
fun AppMenu(app: AppEntry, inDock: Boolean, onAction: (AppAction) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalLauncherColors.current
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .width(340.dp)
                    .glass(RoundedCornerShape(36.dp), colors.panel.copy(alpha = 0.92f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("APP", style = AerialType.eyebrow, color = colors.contentMuted, modifier = Modifier.padding(start = 18.dp, top = 8.dp))
                Text(
                    app.label, style = AerialType.pillTitle.copy(fontSize = AerialType.clock.fontSize * 0.5f),
                    color = colors.content, maxLines = 1, modifier = Modifier.padding(start = 18.dp, bottom = 8.dp),
                )
                fun act(a: AppAction) = { onAction(a) }
                MenuRow(LineIcons.Play, "Open", act(AppAction.Open), focusRequester = first)
                MenuRow(LineIcons.Move, "Move", act(AppAction.Move))
                if (inDock) MenuRow(LineIcons.Unpin, "Remove from dock", act(AppAction.Unpin))
                else MenuRow(LineIcons.Pin, "Pin to dock", act(AppAction.Pin))
                MenuRow(LineIcons.EyeOff, "Hide", act(AppAction.Hide))
                MenuRow(LineIcons.Info, "App info", act(AppAction.Info))
                if (!app.isSystem) MenuRow(LineIcons.Trash, "Uninstall", act(AppAction.Uninstall))
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}
