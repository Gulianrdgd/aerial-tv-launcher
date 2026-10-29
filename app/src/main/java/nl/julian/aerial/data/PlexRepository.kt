package nl.julian.aerial.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** A Plex server Aerial signed in to: its address and the token for it. */
@Serializable
data class PlexLogin(val server: String, val token: String)

/** Plex: sign in with a code at plex.tv/link, then the server's On Deck as Watch Next style items. */
object PlexRepository {
    const val APP = "com.plexapp.android"

    private val http by lazy { OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build() }
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable data class Pin(val id: Long, val code: String, val authToken: String? = null)
    @Serializable private data class Connection(val uri: String, val local: Boolean = false, val relay: Boolean = false)
    @Serializable private data class Resource(
        val provides: String = "", val accessToken: String? = null, val connections: List<Connection> = emptyList(),
    )
    @Serializable private data class Video(
        val ratingKey: String,
        val title: String = "",
        val grandparentTitle: String? = null,
        val parentIndex: Int? = null,
        val index: Int? = null,
        val art: String? = null,
        val grandparentArt: String? = null,
        val thumb: String? = null,
        val viewOffset: Long? = null,
        val duration: Long? = null,
    )
    @Serializable private data class Container(val Metadata: List<Video> = emptyList())
    @Serializable private data class Deck(val MediaContainer: Container)

    private fun Request.Builder.plex(context: Context) = this
        .header("Accept", "application/json")
        .header("X-Plex-Product", "Aerial")
        .header("X-Plex-Client-Identifier", "aerial-" + (Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "tv"))

    /** A new code to enter at plex.tv/link. */
    suspend fun newPin(context: Context): Result<Pin> = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(Request.Builder().url("https://plex.tv/api/v2/pins").plex(context).post(FormBody.Builder().build()).build())
                .execute().use { r -> check(r.isSuccessful) { "Plex answered ${r.code}" }; json.decodeFromString<Pin>(r.body.string()) }
        }
    }

    /** Waits until the code is entered, then picks a server (local connections first). */
    suspend fun awaitLogin(context: Context, pin: Pin): Result<PlexLogin> = withContext(Dispatchers.IO) {
        runCatching {
            var token: String? = null
            repeat(150) { // 5 minutes
                token = http.newCall(Request.Builder().url("https://plex.tv/api/v2/pins/${pin.id}").plex(context).build())
                    .execute().use { r -> json.decodeFromString<Pin>(r.body.string()).authToken }
                if (token != null) return@repeat
                delay(2_000)
            }
            val user = checkNotNull(token) { "The code wasn't entered in time" }
            val resources = http.newCall(
                Request.Builder().url("https://plex.tv/api/v2/resources?includeHttps=1").plex(context).header("X-Plex-Token", user).build()
            ).execute().use { r -> json.decodeFromString<List<Resource>>(r.body.string()) }
            val server = checkNotNull(resources.firstOrNull { "server" in it.provides }) { "No Plex server found on this account" }
            val conn = server.connections.sortedWith(compareBy({ !it.local }, { it.relay })).first()
            PlexLogin(conn.uri, server.accessToken ?: user)
        }
    }

    /** On Deck, refreshed every 5 minutes; empty when the server can't be reached. */
    fun items(context: Context, login: PlexLogin): Flow<List<WatchNextItem>> = flow {
        while (true) {
            emit(runCatching { fetch(context, login) }.onFailure { Log.d("Aerial", "plex: ${it.message}") }.getOrDefault(emptyList()))
            delay(5 * 60_000L)
        }
    }.flowOn(Dispatchers.IO)

    private fun fetch(context: Context, login: PlexLogin): List<WatchNextItem> {
        val deck = http.newCall(
            Request.Builder().url("${login.server}/library/onDeck").plex(context).header("X-Plex-Token", login.token).build()
        ).execute().use { r -> check(r.isSuccessful) { "HTTP ${r.code}" }; json.decodeFromString<Deck>(r.body.string()) }
        return deck.MediaContainer.Metadata.take(12).mapIndexed { i, v ->
            val art = v.grandparentArt ?: v.art ?: v.thumb
            WatchNextItem(
                id = i.toLong(), pkg = APP,
                title = v.grandparentTitle ?: v.title,
                subtitle = if (v.parentIndex != null && v.index != null) "S${v.parentIndex} E${v.index} · ${v.title}" else null,
                imageUri = art?.let { "${login.server}/photo/:/transcode?width=640&height=360&url=${android.net.Uri.encode(it)}&X-Plex-Token=${login.token}" },
                intentUri = null,
                progress = if (v.viewOffset != null && v.duration != null && v.duration > 0) v.viewOffset.toFloat() / v.duration else null,
            )
        }
    }
}
