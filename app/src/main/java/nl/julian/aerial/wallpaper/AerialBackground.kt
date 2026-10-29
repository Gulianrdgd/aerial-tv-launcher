package nl.julian.aerial.wallpaper

import android.util.Log
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import nl.julian.aerial.theme.FluidEasing
import nl.julian.aerial.theme.seedsSimilar
import nl.julian.aerial.ui.AmbientBackdrop

/** No frame within this window = a dead stream; skip it. */
private const val STALL_MS = 8_000L
/** When, after a clip's first frame, the picture is sampled for the UI colours. */
private const val SAMPLE_DELAY_MS = 1_500L

/**
 * Apple TV-style aerial video behind the home screen: muted, aspect-filled,
 * one clip after another, each faded up from the ambient backdrop once its
 * first frame is on screen.
 *
 * Player lifecycle, SurfaceView choice and fade approach adapted from
 * couchy-launcher's VideoWallpaper (GPL-3.0, see NOTICE).
 */
@OptIn(UnstableApi::class)
@Composable
fun AerialBackground(
    modifier: Modifier = Modifier,
    /** Which scenery to play (city, nature, …); null = everything. */
    kind: String? = null,
    /** "day" or "night" to play only those clips (and ones that suit any time); null = all. */
    time: String? = null,
    /** Called with a new theme seed colour whenever the picture's palette shifts. */
    onSeed: (Int) -> Unit = {},
    /** Called when a clip starts, e.g. to caption it. */
    onClip: (Aerial) -> Unit = {},
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current

    val queue = remember(kind, time) {
        val all = Aerials.load(context)
        val picked = all.filter { kind == null || it.kind == kind }
        val timed = picked.filter { time == null || it.time.isEmpty() || it.time == time }
        // A scenery with no clips falls back to everything.
        AerialQueue(timed.ifEmpty { picked }.ifEmpty { all })
    }
    var current by remember { mutableStateOf<Aerial?>(null) }
    LaunchedEffect(queue) {
        // Begin with a clip that's already on disk: instant, and works offline.
        withContext(Dispatchers.IO) {
            queue.preferFirst { AerialMedia.isFullyCached(context, it.uri) }
        }
        current = queue.next()
    }

    // Callbacks and effects that outlive a scenery change must use the
    // current queue, not the one that existed when they were created.
    val liveQueue by rememberUpdatedState(queue)


    val reveal = remember { Animatable(0f) }
    var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var firstFrameGen by remember { mutableIntStateOf(0) }

    // The hardware decoder is held only while the launcher is visible.
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    DisposableEffect(lifecycleOwner) {
        fun build(): ExoPlayer = ExoPlayer.Builder(context)
            .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
            .setMediaSourceFactory(DefaultMediaSourceFactory(AerialMedia.dataSourceFactory(context)))
            // A muted wallpaper never seeks; the default 50 s buffer just pins RAM.
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(10_000, 15_000, 2_500, 5_000)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .build().apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_OFF
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                playWhenReady = true
            }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (player == null) player = build()
                Lifecycle.Event.ON_STOP -> {
                    player?.release()
                    player = null
                }
                else -> Unit
            }
        }
        if (player == null && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            player = build()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player?.release()
            player = null
        }
    }

    // Released player → snap the cover back so the return shows the backdrop, not black.
    LaunchedEffect(player == null) { if (player == null) reveal.snapTo(0f) }

    DisposableEffect(player) {
        val p = player ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrameGen++ }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }.forEach { g ->
                    val f = g.getTrackFormat(0)
                    Log.i("Aerial", "video ${f.sampleMimeType} ${f.codecs} ${f.width}x${f.height}")
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) current = liveQueue.next()
            }
            override fun onPlayerError(error: PlaybackException) {
                Log.w("Aerial", "play error ${error.errorCodeName}: ${current?.uri}")
                current = liveQueue.next()
            }
        }
        p.addListener(listener)
        onDispose { p.removeListener(listener) }
    }

    LaunchedEffect(player, current) {
        val p = player ?: return@LaunchedEffect
        val clip = current ?: return@LaunchedEffect
        // Fade the old clip down to the backdrop before swapping.
        if (reveal.value > 0f) reveal.animateTo(0f, tween(600, easing = FluidEasing))
        val genBefore = firstFrameGen
        Log.i("Aerial", "play ${clip.title}: ${clip.uri}")
        onClip(clip)
        p.setMediaItem(MediaItem.fromUri(clip.uri))
        p.prepare()
        delay(STALL_MS)
        val resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (firstFrameGen == genBefore && resumed) {
            Log.w("Aerial", "no frame in ${STALL_MS}ms, skipping: ${clip.uri}")
            current = liveQueue.next()
        }
    }

    // First frame is up: slow fade in. Clips load lazily: nothing is
    // fetched ahead, only what's on screen streams (and lands in the cache).
    LaunchedEffect(firstFrameGen) {
        if (firstFrameGen == 0) return@LaunchedEffect
        reveal.animateTo(1f, tween(1400, easing = FluidEasing))
    }

    // Sample the picture once per clip (a re-theme recolours the whole UI,
    // which is too much work to repeat every few seconds on a TV chip).
    // Skip palette shifts too small to notice.
    var lastSeed by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(firstFrameGen) {
        if (firstFrameGen == 0) return@LaunchedEffect
        delay(SAMPLE_DELAY_MS) // well into the fade-in, so the frame is the new clip
        val seed = surfaceView?.let { FrameSampler.seed(it) } ?: return@LaunchedEffect
        if (lastSeed == null || !seedsSimilar(lastSeed!!, seed)) {
            lastSeed = seed
            onSeed(seed)
        }
    }

    // SurfaceView, not TextureView: SurfaceFlinger composites it off the UI
    // thread, so the video never stutters while focus animations run. It can't
    // alpha-blend, so the fade is the backdrop drawn on top with falling alpha.
    Box(modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { SurfaceView(it).also { v -> surfaceView = v } },
            update = { view -> player?.setVideoSurfaceView(view) },
        )
        AmbientBackdrop(Modifier.graphicsLayer { alpha = 1f - reveal.value })
    }
}
