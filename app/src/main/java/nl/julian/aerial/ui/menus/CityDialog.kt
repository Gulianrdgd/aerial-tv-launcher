package nl.julian.aerial.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.ui.Text
import kotlinx.coroutines.launch
import nl.julian.aerial.data.WeatherRepository
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.ui.FocusSurface

/**
 * Weather location: type a city with the TV keyboard. The name is checked
 * with the geocoder before it's saved; "Automatic" goes back to the city
 * derived from the TV's timezone.
 */
@Composable
fun CityDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val field = remember { FocusRequester() }
    var text by remember { mutableStateOf(current) }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }

    fun save() {
        if (text.isBlank()) return onSave("")
        checking = true
        scope.launch {
            val found = WeatherRepository.findPlace(text)
            checking = false
            if (found != null) onSave(text.trim()) else error = "Couldn't find \"${text.trim()}\". Try another spelling."
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().swallowStrayOkRelease(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .width(480.dp)
                    .background(LocalSurfaces.current.default, MaterialTheme.shapes.extraLarge)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Weather location", style = MaterialTheme.typography.headlineSmall, color = cs.onSurface)
                Text(
                    "Type a city. Leave it empty to use the TV's timezone.",
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                )
                BasicTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = cs.onSurface),
                    cursorBrush = SolidColor(cs.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(field)
                        .onFocusChanged { fieldFocused = it.isFocused }
                        .background(LocalSurfaces.current.highest, CircleShape)
                        .border(2.dp, if (fieldFocused) cs.primary else cs.borderVariant, CircleShape)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                )
                error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = cs.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.align(Alignment.End)) {
                    DialogButton("Automatic", primary = false) { onSave("") }
                    DialogButton(if (checking) "Checking…" else "Save", primary = true) { save() }
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { field.requestFocus() } }
}

@Composable
internal fun DialogButton(label: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    FocusSurface(
        onClick = onClick,
        shape = CircleShape,
        container = if (primary) cs.primary else LocalSurfaces.current.highest,
        contentColor = if (primary) cs.onPrimary else cs.onSurface,
        modifier = modifier.height(48.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp))
    }
}
