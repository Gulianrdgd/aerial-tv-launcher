package nl.julian.aerial

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.julian.aerial.data.ConfigStore
import nl.julian.aerial.ui.HomeScreen

class MainActivity : ComponentActivity() {
    /** Bumped on every Home press while we're already home: scroll up + replay entry. */
    private val homeTick = mutableIntStateOf(0)
    private val idle = mutableStateOf(false)
    /** No remote input for this long → the UI fades away and the aerial plays full screen; 0 = never. */
    @Volatile
    private var idleAfterMs = 2 * 60_000L
    private var lastInput = SystemClock.uptimeMillis()
    /** The key that woke us from idle: its repeats and key-up are swallowed too. */
    private var wakeKey: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The aerial SurfaceView sits behind the window; an opaque window
        // background would cover it.
        window.setBackgroundDrawable(null)
        // A launcher must never be "backed out of": swallow Back on the home
        // screen. Registered first, so panels' own Back handlers take priority.
        onBackPressedDispatcher.addCallback(this) { }
        setContent {
            HomeScreen(homeTick.intValue, idle.value, onStartScreensaver = { idle.value = true })
        }

        lifecycleScope.launch {
            ConfigStore.get(this@MainActivity).config.collect { idleAfterMs = it.idleMinutes * 60_000L }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                idle.value = false
                lastInput = SystemClock.uptimeMillis()
                while (true) {
                    if (idleAfterMs <= 0) {
                        delay(1_000)
                        continue
                    }
                    val remaining = lastInput + idleAfterMs - SystemClock.uptimeMillis()
                    if (remaining > 0) delay(minOf(remaining, 1_000)) else {
                        idle.value = true
                        delay(1_000)
                    }
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        lastInput = SystemClock.uptimeMillis()
        if (idle.value) {
            // Waking is all this press does; it must not also launch something.
            if (event.action == KeyEvent.ACTION_DOWN) {
                idle.value = false
                wakeKey = event.keyCode
            }
            return true
        }
        if (event.keyCode == wakeKey) {
            if (event.action == KeyEvent.ACTION_UP) wakeKey = null
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!intent.hasCategory(Intent.CATEGORY_HOME)) return
        lastInput = SystemClock.uptimeMillis()
        if (idle.value) idle.value = false else homeTick.intValue++
    }

}
