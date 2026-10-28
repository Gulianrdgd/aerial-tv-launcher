package nl.julian.aerial.ui.menus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * For dialogs opened with OK: the rest of that same press (its key repeats
 * while held, and its release) arrives in the new dialog and would click
 * whatever just took focus there. Swallow OK until this dialog has seen a
 * fresh press of its own; a repeat doesn't count as one.
 */
fun Modifier.swallowStrayOkRelease(): Modifier = composed {
    var sawPress by remember { mutableStateOf(false) }
    onPreviewKeyEvent { e ->
        val ok = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
        when {
            !ok -> false
            e.type == KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount == 0 -> {
                sawPress = true
                false
            }
            else -> !sawPress
        }
    }
}
