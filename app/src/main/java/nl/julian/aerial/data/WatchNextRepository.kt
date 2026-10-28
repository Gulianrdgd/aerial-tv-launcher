package nl.julian.aerial.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.media.tv.TvContract.WatchNextPrograms
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn

@Immutable
data class WatchNextItem(
    val id: Long,
    val pkg: String?,
    val title: String,
    val subtitle: String?,
    /** Landscape artwork when the app provides it. */
    val imageUri: String?,
    val intentUri: String?,
    /** 0..1 watched, or null for "next episode"/unknown. */
    val progress: Float?,
)

/** "Continue watching": the system Watch Next list that streaming apps publish to. */
object WatchNextRepository {
    const val PERMISSION = "android.permission.READ_TV_LISTINGS"
    private const val LEGACY_PERMISSION = "com.android.providers.tv.permission.READ_WRITE_WATCH_NEXT_PROGRAMS"
    private const val MAX_ITEMS = 20

    fun hasAccess(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(LEGACY_PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** Live list, newest engagement first. Re-queries whenever any app updates it. */
    fun items(context: Context): Flow<List<WatchNextItem>> = callbackFlow {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(query(context)) }
        }
        trySend(query(context))
        runCatching { resolver.registerContentObserver(WatchNextPrograms.CONTENT_URI, true, observer) }
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.conflate().flowOn(Dispatchers.IO)

    fun launchIntent(context: Context, item: WatchNextItem): Intent? {
        val fromUri = item.intentUri?.let { runCatching { Intent.parseUri(it, Intent.URI_INTENT_SCHEME) }.getOrNull() }
        val intent = fromUri ?: item.pkg?.let { AppRepository.launchIntent(context, it) } ?: return null
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private val PROJECTION = arrayOf(
        WatchNextPrograms._ID,
        WatchNextPrograms.COLUMN_PACKAGE_NAME,
        WatchNextPrograms.COLUMN_TITLE,
        WatchNextPrograms.COLUMN_EPISODE_TITLE,
        WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER,
        WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER,
        WatchNextPrograms.COLUMN_SHORT_DESCRIPTION,
        WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE,
        WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS,
        WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
        WatchNextPrograms.COLUMN_DURATION_MILLIS,
        WatchNextPrograms.COLUMN_INTENT_URI,
        WatchNextPrograms.COLUMN_POSTER_ART_URI,
        WatchNextPrograms.COLUMN_POSTER_ART_ASPECT_RATIO,
        WatchNextPrograms.COLUMN_THUMBNAIL_URI,
        WatchNextPrograms.COLUMN_BROWSABLE,
    )

    private fun query(context: Context): List<WatchNextItem> {
        if (!hasAccess(context)) return emptyList()
        val rows = mutableListOf<Pair<Long, WatchNextItem>>()
        try {
            context.contentResolver.query(WatchNextPrograms.CONTENT_URI, PROJECTION, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    if (c.int(WatchNextPrograms.COLUMN_BROWSABLE) == 0) continue
                    val title = c.str(WatchNextPrograms.COLUMN_TITLE) ?: continue
                    // Some apps write seconds instead of millis.
                    val engaged = (c.long(WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS) ?: 0L)
                        .let { if (it in 1 until 10_000_000_000L) it * 1000 else it }
                    rows += engaged to WatchNextItem(
                        id = c.long(WatchNextPrograms._ID) ?: continue,
                        pkg = c.str(WatchNextPrograms.COLUMN_PACKAGE_NAME),
                        title = title,
                        subtitle = subtitle(c),
                        imageUri = image(c),
                        intentUri = c.str(WatchNextPrograms.COLUMN_INTENT_URI),
                        progress = progress(c),
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.w("Aerial", "watch next not readable: ${e.message}")
        }
        return rows.sortedByDescending { it.first }.take(MAX_ITEMS).map { it.second }
    }

    private fun subtitle(c: Cursor): String? {
        val season = c.str(WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER)
        val episode = c.str(WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER)
        val name = c.str(WatchNextPrograms.COLUMN_EPISODE_TITLE)
        val number = listOfNotNull(season?.let { "S$it" }, episode?.let { "E$it" }).joinToString(" ")
        return listOf(number, name.orEmpty()).filter { it.isNotBlank() }.joinToString(" · ").ifBlank {
            c.str(WatchNextPrograms.COLUMN_SHORT_DESCRIPTION)
        }
    }

    private fun image(c: Cursor): String? {
        val poster = c.str(WatchNextPrograms.COLUMN_POSTER_ART_URI)
        val landscapePoster = c.int(WatchNextPrograms.COLUMN_POSTER_ART_ASPECT_RATIO) ==
            WatchNextPrograms.ASPECT_RATIO_16_9
        return if (landscapePoster) poster else c.str(WatchNextPrograms.COLUMN_THUMBNAIL_URI) ?: poster
    }

    private fun progress(c: Cursor): Float? {
        if (c.int(WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE) == WatchNextPrograms.WATCH_NEXT_TYPE_NEXT) return null
        val pos = c.long(WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS) ?: return null
        val dur = c.long(WatchNextPrograms.COLUMN_DURATION_MILLIS) ?: return null
        return if (pos > 0 && dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else null
    }

    private fun Cursor.idx(col: String) = getColumnIndex(col).takeIf { it >= 0 && !isNull(it) }
    private fun Cursor.str(col: String) = idx(col)?.let { getString(it) }?.takeIf { it.isNotBlank() }
    private fun Cursor.long(col: String) = idx(col)?.let { getLong(it) }
    private fun Cursor.int(col: String) = idx(col)?.let { getInt(it) }
}
