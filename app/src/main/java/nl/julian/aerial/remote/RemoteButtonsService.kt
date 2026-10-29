package nl.julian.aerial.remote

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import nl.julian.aerial.MainActivity
import nl.julian.aerial.data.ConfigStore
import nl.julian.aerial.data.RemoteButtons

/**
 * Optional fix for TVs where the remote's Input (and sometimes Home)
 * button stops working once the stock launcher is disabled: those keys
 * open Aerial instead. Every other key passes through untouched.
 */
class RemoteButtonsService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @Volatile
    private var mode = RemoteButtons.Off

    override fun onServiceConnected() {
        scope.launch { ConfigStore.get(this@RemoteButtonsService).config.collect { mode = it.remoteButtons } }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val intent = when {
            event.keyCode == KeyEvent.KEYCODE_TV_INPUT && mode != RemoteButtons.Off ->
                Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_INPUTS, true)
            event.keyCode == KeyEvent.KEYCODE_HOME && mode == RemoteButtons.InputHome ->
                Intent(this, MainActivity::class.java).addCategory(Intent.CATEGORY_HOME)
            else -> return false
        }
        if (event.action == KeyEvent.ACTION_UP) {
            runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val me = ComponentName(context, RemoteButtonsService::class.java)
            val on = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            return on?.split(':')?.any { ComponentName.unflattenFromString(it) == me } == true
        }
    }
}
