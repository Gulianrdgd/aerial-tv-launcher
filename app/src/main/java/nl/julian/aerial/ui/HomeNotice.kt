package nl.julian.aerial.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.julian.aerial.theme.SectionPanel
import nl.julian.aerial.ui.menus.DialogButton
import nl.julian.aerial.ui.widgets.startSafely

/** True when the Home button opens Aerial (no other launcher is the default). */
fun isDefaultHome(context: Context): Boolean {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo?.packageName == context.packageName
}

/**
 * Shown above the dock when Home no longer opens Aerial: firmware updates
 * and reboots often hand it back to the stock launcher.
 */
@Composable
fun HomeNotice(onHide: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    SectionPanel(modifier, title = "Aerial isn't your Home app any more") {
        Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "The Home button opens another launcher, often after a system update. Choose Aerial in the settings. " +
                    "If the TV doesn't offer it, switch the stock launcher off from a computer:",
                style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
            )
            Text(
                "adb shell pm disable-user --user 0 com.google.android.tvlauncher",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = cs.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogButton("Open settings", primary = true) { context.startSafely(Intent(Settings.ACTION_HOME_SETTINGS)) }
                DialogButton("Don't show again", primary = false, onClick = onHide)
            }
        }
    }
}
