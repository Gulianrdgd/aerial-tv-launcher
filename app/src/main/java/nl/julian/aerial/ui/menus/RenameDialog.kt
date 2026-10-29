package nl.julian.aerial.ui.menus

import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import nl.julian.aerial.theme.LocalSurfaces

/** A name typed with the TV keyboard (apps, folders). [resetLabel] adds a button that saves "". */
@Composable
fun RenameDialog(
    title: String,
    current: String,
    resetLabel: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    /** Masks what's typed. */
    password: Boolean = false,
    /** Shown under the title, e.g. why a sign-in failed. */
    note: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    val field = remember { FocusRequester() }
    var text by remember { mutableStateOf(current) }
    var focused by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().swallowStrayOkRelease(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .width(480.dp)
                    .background(LocalSurfaces.current.default, MaterialTheme.shapes.extraLarge)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = cs.onSurface)
                note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant) }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = cs.onSurface),
                    cursorBrush = SolidColor(cs.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave(text.trim()) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(field)
                        .onFocusChanged { focused = it.isFocused }
                        .background(LocalSurfaces.current.highest, CircleShape)
                        .border(2.dp, if (focused) cs.primary else cs.borderVariant, CircleShape)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.align(Alignment.End)) {
                    resetLabel?.let { DialogButton(it, primary = false) { onSave("") } }
                    DialogButton("Save", primary = true) { onSave(text.trim()) }
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { field.requestFocus() } }
}
