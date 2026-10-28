package nl.julian.aerial.ui.menus

import android.media.tv.TvInputInfo
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.data.InputDevice
import nl.julian.aerial.data.InputsRepository
import nl.julian.aerial.data.TvInput
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.ui.widgets.startSafely

/** Input switcher: every TV input (HDMI ports, antenna, …) in the side panel. */
@Composable
fun InputsPanel(inputs: List<TvInput>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    SidePanel("Inputs", onDismiss, initialFocus = first.takeIf { inputs.isNotEmpty() }) {
        if (inputs.isEmpty()) {
            Text(
                "This TV doesn't report any inputs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        inputs.forEachIndexed { i, input ->
            MenuRow(
                icon = iconFor(input),
                title = input.label,
                value = if (input.connected) null else "Not connected",
                focusRequester = if (i == 0) first else null,
                onClick = {
                    onDismiss()
                    context.startSafely(InputsRepository.switchIntent(input))
                },
            )
        }
    }
}

private fun iconFor(input: TvInput) = when (input.device) {
    InputDevice.Console -> Symbols.Console
    InputDevice.MediaPlayer -> Symbols.MediaPlayer
    InputDevice.Disc -> Symbols.Disc
    InputDevice.Computer -> Symbols.Computer
    InputDevice.Audio -> Symbols.Speaker
    InputDevice.Unknown -> iconForType(input.type)
}

private fun iconForType(type: Int) = when (type) {
    TvInputInfo.TYPE_HDMI, TvInputInfo.TYPE_DVI, TvInputInfo.TYPE_DISPLAY_PORT -> Symbols.InputHdmi
    TvInputInfo.TYPE_COMPONENT, TvInputInfo.TYPE_COMPOSITE, TvInputInfo.TYPE_SVIDEO,
    TvInputInfo.TYPE_SCART, TvInputInfo.TYPE_VGA -> Symbols.InputComponent
    TvInputInfo.TYPE_TUNER -> Symbols.InputAntenna
    else -> Symbols.Input
}
