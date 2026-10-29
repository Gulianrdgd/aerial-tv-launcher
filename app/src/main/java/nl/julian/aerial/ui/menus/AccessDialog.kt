package nl.julian.aerial.ui.menus

import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.ui.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.ui.widgets.startSafely

private const val GUIDE = "https://github.com/Gulianrdgd/aerial-tv-launcher#permissions"
private const val COMMAND =
    "adb shell cmd notification allow_listener nl.julian.aerial/nl.julian.aerial.media.LauncherListenerService"

/**
 * Why Now Playing and notifications need notification access, and how to
 * grant it: most TVs have no screen for it, so it's one adb command. The
 * QR code opens the full instructions on a phone.
 */
@Composable
fun AccessDialog(onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val qr = remember { qrBitmap(GUIDE, 33 * 8).asImageBitmap() }
    // Some TVs do have the screen; offer it only where it exists.
    val settings = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        .takeIf { it.resolveActivity(context.packageManager) != null }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().swallowStrayOkRelease(), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .width(760.dp)
                    .background(LocalSurfaces.current.default, MaterialTheme.shapes.extraLarge)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Turn on Now Playing", style = MaterialTheme.typography.headlineSmall, color = cs.onSurface)
                    Text(
                        "To show what's playing and your notifications, Aerial needs notification access. " +
                            "Most TVs have no setting for it, so run this once from a computer on the same network:",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                    )
                    Text(
                        COMMAND,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = cs.onSurface,
                        modifier = Modifier
                            .background(LocalSurfaces.current.highest, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    )
                    Text(
                        "Scan the code for step-by-step instructions.",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.align(Alignment.End)) {
                        settings?.let { DialogButton("Open settings", primary = false) { context.startSafely(it) } }
                        DialogButton("Close", primary = true, onClick = onDismiss)
                    }
                }
                Image(
                    qr, contentDescription = "QR code for the instructions",
                    filterQuality = FilterQuality.None,
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                )
            }
        }
    }
}

private fun qrBitmap(text: String, size: Int): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(com.google.zxing.EncodeHintType.MARGIN to 0))
    val pixels = IntArray(size * size) { if (m[it % size, it / size]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}
