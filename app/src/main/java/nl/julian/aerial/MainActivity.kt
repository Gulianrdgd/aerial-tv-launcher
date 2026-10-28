package nl.julian.aerial

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import nl.julian.aerial.ui.HomeScreen

class MainActivity : ComponentActivity() {
    /** Bumped on every Home press while we're already home: scroll up + replay entry. */
    private val homeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The aerial SurfaceView sits behind the window; an opaque window
        // background would cover it.
        window.setBackgroundDrawable(null)
        setContent { HomeScreen(homeTick.intValue) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeTick.intValue++
    }

    // A launcher must never be "backed out of": swallow Back on the home screen.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = Unit
}
