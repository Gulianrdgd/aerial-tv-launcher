package nl.julian.aerial.ui.menus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color

/**
 * A modal layer drawn in the home screen's own window. Opening a separate
 * dialog window takes a noticeable moment on TV hardware; this doesn't.
 * Dims what's behind, keeps focus inside, and closes on Back, also when
 * nothing inside it has focus (an empty panel, or the focused row was
 * just removed).
 */
@Composable
fun Overlay(
    onDismiss: () -> Unit,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .swallowStrayOkRelease()
            // Focus can't wander out to the home screen behind.
            .focusProperties { onExit = { cancelFocusChange() } }
            .focusGroup(),
        contentAlignment = contentAlignment,
        content = content,
    )
}
