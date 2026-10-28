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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import nl.julian.aerial.media.MediaListenerService

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

    private fun listener(context: Context) = ComponentName(context, MediaListenerService::class.java)

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
            art = art?.asImageBitmap(),
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
}
