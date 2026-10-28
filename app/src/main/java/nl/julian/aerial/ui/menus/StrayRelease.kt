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
 * For dialogs opened with OK: the release of that same press arrives in the
 * new dialog and would click whatever just took focus there. Swallow OK
 * releases until this dialog has seen an OK press of its own.
 */
fun Modifier.swallowStrayOkRelease(): Modifier = composed {
    var sawPress by remember { mutableStateOf(false) }
    onPreviewKeyEvent { e ->
        val ok = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
        when {
            !ok -> false
            e.type == KeyEventType.KeyDown -> {
                sawPress = true
                false
            }
            else -> !sawPress
        }
    }
}
