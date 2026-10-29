package nl.julian.aerial.ui.menus

import android.content.Context
import android.media.tv.TvInputInfo
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.ui.Text
import nl.julian.aerial.data.AppEntry
import nl.julian.aerial.data.InputDevice
import nl.julian.aerial.data.InputsRepository
import nl.julian.aerial.data.TvInput
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.ui.widgets.startSafely

/** Input switcher: every TV input (HDMI ports, antenna, …) in the side panel. */
@Composable
fun InputsPanel(
    inputs: List<TvInput>,
    /** Dock keys ([InputsRepository.DOCK_PREFIX] + id) of the inputs pinned to the dock. */
    docked: Set<String>,
    onTogglePin: (TvInput) -> Unit,
    onDismiss: () -> Unit,
) {
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
                value = when {
                    input.dockKey in docked -> "In the dock"
                    !input.connected -> "Not connected"
                    else -> null
                },
                focusRequester = if (i == 0) first else null,
                onLongClick = { onTogglePin(input) },
                onClick = {
                    onDismiss()
                    context.startSafely(InputsRepository.switchIntent(input))
                },
            )
        }
        if (inputs.isNotEmpty()) {
            Text(
                "Hold OK to pin an input to the dock, or to unpin it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

internal val TvInput.dockKey get() = InputsRepository.DOCK_PREFIX + id

/** A dock tile for a pinned input: its name with the input's icon. */
internal fun TvInput.dockEntry(context: Context) = AppEntry(
    pkg = dockKey,
    label = label,
    banner = null,
    icon = ContextCompat.getDrawable(context, iconFor(this))?.mutate()
        ?.apply { setTint(android.graphics.Color.WHITE) }?.toBitmap(96, 96)?.asImageBitmap(),
    stamp = 0,
    isSystem = true,
)

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
