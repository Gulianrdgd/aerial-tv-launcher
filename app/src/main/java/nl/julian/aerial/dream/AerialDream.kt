package nl.julian.aerial.dream

import android.service.dreams.DreamService
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.emptyFlow
import nl.julian.aerial.data.ConfigStore
import nl.julian.aerial.data.FavouriteMode
import nl.julian.aerial.data.NowPlayingRepository
import nl.julian.aerial.data.WeatherRepository
import nl.julian.aerial.data.aerialTimeTag
import nl.julian.aerial.theme.AerialTheme
import nl.julian.aerial.ui.Ambient
import nl.julian.aerial.wallpaper.Aerial
import nl.julian.aerial.wallpaper.AerialBackground

/**
 * Aerial as the TV's system screensaver, so the aerials also show over
 * other apps. Same clips, settings and overlay as the home screen's own
 * screensaver. Any button wakes the TV (non-interactive dream).
 */
class AerialDream : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val saved = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        saved.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        // The video's SurfaceView sits behind the window.
        window.setBackgroundDrawable(null)
        setContentView(ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AerialDream)
            setViewTreeSavedStateRegistryOwner(this@AerialDream)
            setContent { DreamScreen() }
        })
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}

@Composable
private fun DreamScreen() {
    val context = LocalContext.current.applicationContext
    val config by remember { ConfigStore.get(context).config }.collectAsStateWithLifecycle(null)
    val media by remember { NowPlayingRepository.nowPlaying(context) }.collectAsStateWithLifecycle(null)
    val weather by remember(config?.weatherCity, config?.tempUnit) {
        config?.let { WeatherRepository.weather(it.weatherCity, it.tempUnit) } ?: emptyFlow()
    }.collectAsStateWithLifecycle(null)
    var seed by remember { mutableStateOf<Int?>(null) }
    var clip by remember { mutableStateOf<Aerial?>(null) }

    val cfg = config ?: return
    Box(Modifier.fillMaxSize()) {
        AerialBackground(
            kind = cfg.scenery.kind,
            time = cfg.aerialTimeTag(weather?.isDay),
            skipped = cfg.skipped,
            favourites = if (cfg.favouriteMode == FavouriteMode.Normal) emptySet() else cfg.favourites,
            onlyFavourites = cfg.favouriteMode == FavouriteMode.Only,
            quality = cfg.videoQuality,
            onSeed = { seed = it },
            onClip = { clip = it },
        )
        AerialTheme(seed) {
            Ambient(clip, weather, media, cfg.clockFormat, visible = true, info = cfg.ambientInfo)
        }
    }
}
