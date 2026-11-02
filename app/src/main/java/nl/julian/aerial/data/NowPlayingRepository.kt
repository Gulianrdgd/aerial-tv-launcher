package nl.julian.aerial.data

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.Immutable
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import nl.julian.aerial.media.LauncherListenerService

@Immutable
data class NowPlaying(
    val title: String,
    val artist: String?,
    val art: ImageBitmap?,
    val artUri: String?,
    val playing: Boolean,
    /** Position at [positionAt] (elapsedRealtime), advancing at [speed] while playing. */
    val position: Long,
    val positionAt: Long,
    val speed: Float,
    val duration: Long,
    private val controls: MediaController.TransportControls,
) {
    fun positionNow(): Long =
        if (!playing) position
        else position + ((SystemClock.elapsedRealtime() - positionAt) * speed).toLong()

    fun playPause() = if (playing) controls.pause() else controls.play()
    fun next() = controls.skipToNext()
    fun previous() = controls.skipToPrevious()
}

/** The media session that's playing (or paused) anywhere on the TV. */
object NowPlayingRepository {

    fun hasAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)
            .isNotificationListenerAccessGranted(listener(context))

    private fun listener(context: Context) = ComponentName(context, LauncherListenerService::class.java)

    fun nowPlaying(context: Context): Flow<NowPlaying?> = callbackFlow {
        if (!hasAccess(context)) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val manager = context.getSystemService(MediaSessionManager::class.java)
        val main = Handler(Looper.getMainLooper())
        var watched: MediaController? = null

        val controllerCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) { trySend(snapshot(watched)) }
            override fun onMetadataChanged(metadata: MediaMetadata?) { trySend(snapshot(watched)) }
            override fun onSessionDestroyed() { trySend(null) }
        }

        fun watch(controllers: List<MediaController>?) {
            // Prefer whatever is playing; else the most recent paused session.
            val pick = controllers.orEmpty().let { list ->
                list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                    ?: list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PAUSED }
            }
            if (pick?.sessionToken != watched?.sessionToken) {
                watched?.unregisterCallback(controllerCallback)
                pick?.registerCallback(controllerCallback, main)
                watched = pick
            }
            trySend(snapshot(pick))
        }

        val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { watch(it) }
        manager.addOnActiveSessionsChangedListener(sessionsListener, listener(context), main)
        watch(manager.getActiveSessions(listener(context)))

        awaitClose {
            manager.removeOnActiveSessionsChangedListener(sessionsListener)
            watched?.unregisterCallback(controllerCallback)
        }
    }.distinctUntilChanged()

    private fun snapshot(c: MediaController?): NowPlaying? {
        c ?: return null
        val state = c.playbackState ?: return null
        if (state.state != PlaybackState.STATE_PLAYING && state.state != PlaybackState.STATE_PAUSED) return null
        val meta = c.metadata ?: return null
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: return null
        val art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
        return NowPlaying(
            title = title,
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
            art = art?.let { small(it, title, meta.getString(MediaMetadata.METADATA_KEY_ARTIST)) },
            artUri = meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: meta.getString(MediaMetadata.METADATA_KEY_ART_URI),
            playing = state.state == PlaybackState.STATE_PLAYING,
            position = state.position,
            positionAt = state.lastPositionUpdateTime,
            speed = state.playbackSpeed.takeIf { it > 0f } ?: 1f,
            duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
            controls = c.transportControls,
        )
    }

    /**
     * Album art arrives at full size (often 640 px or more) and shows at 84 dp
     * at most, so it's shrunk once per song. Every playback update hands over
     * a fresh copy of the same picture, hence the key instead of the bitmap.
     */
    private var lastArt: Pair<String, ImageBitmap>? = null

    private fun small(art: Bitmap, title: String, artist: String?): ImageBitmap {
        val key = "$title|$artist|${art.width}x${art.height}"
        lastArt?.takeIf { it.first == key }?.let { return it.second }
        val scale = ART_PX.toFloat() / maxOf(art.width, art.height)
        val shrunk = if (scale >= 1f) art
        else Bitmap.createScaledBitmap(art, (art.width * scale).toInt().coerceAtLeast(1), (art.height * scale).toInt().coerceAtLeast(1), true)
        return shrunk.asImageBitmap().also { lastArt = key to it }
    }

    private const val ART_PX = 256
}
