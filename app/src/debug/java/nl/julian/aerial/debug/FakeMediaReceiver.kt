package nl.julian.aerial.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock

/**
 * Debug builds only: a fake playing track, so Now Playing can be tested on an
 * emulator without a music app. Actions: play, pause, stop.
 */
class FakeMediaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "play" -> session(context).apply { isActive = true; setState(this, playing = true) }
            "pause" -> session?.let { setState(it, playing = false) }
            "stop" -> { session?.release(); session = null }
        }
    }

    companion object {
        private var session: MediaSession? = null
        private var position = 64_000L
        private var track = 0
        private val TRACKS = listOf("Off The Knob" to "Marlon Craft", "Night Drive" to "Aerial Test")

        private fun session(context: Context): MediaSession = session ?: MediaSession(context, "fake").also { s ->
            s.setCallback(object : MediaSession.Callback() {
                override fun onPlay() = setState(s, playing = true)
                override fun onPause() = setState(s, playing = false)
                override fun onSkipToNext() { track = (track + 1) % TRACKS.size; position = 0; setMeta(s); setState(s, true) }
                override fun onSkipToPrevious() { position = 0; setState(s, true) }
            })
            setMeta(s)
            session = s
        }

        private fun setMeta(s: MediaSession) {
            val (title, artist) = TRACKS[track]
            s.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, 214_000L)
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art(track))
                    .build()
            )
        }

        private fun setState(s: MediaSession, playing: Boolean) {
            s.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(
                        PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                    )
                    .setState(
                        if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                        position, 1f, SystemClock.elapsedRealtime(),
                    )
                    .build()
            )
        }

        private fun art(seed: Int): Bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).also {
            val colors = if (seed == 0) intArrayOf(0xFFE8503A.toInt(), 0xFF7A1F4A.toInt())
            else intArrayOf(0xFF2A6BE8.toInt(), 0xFF1B2448.toInt())
            Canvas(it).drawRect(0f, 0f, 256f, 256f, Paint().apply {
                shader = LinearGradient(0f, 0f, 256f, 256f, colors[0], colors[1], Shader.TileMode.CLAMP)
            })
        }
    }
}
