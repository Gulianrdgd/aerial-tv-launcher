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
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Signed-in Jellyfin server: address, user id and access token. */
@Serializable
data class JellyfinLogin(val server: String, val userId: String, val token: String)

/** "Continue watching" and "Next up" from a Jellyfin server, as Watch Next style items. */
object JellyfinRepository {
    const val APP = "org.jellyfin.androidtv"

    private val http by lazy { OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build() }
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable private data class AuthUser(val Id: String)
    @Serializable private data class AuthResult(val AccessToken: String, val User: AuthUser)
    @Serializable private data class UserData(val PlayedPercentage: Double? = null)
    @Serializable private data class Item(
        val Id: String,
        val Name: String = "",
        val SeriesName: String? = null,
        val ParentIndexNumber: Int? = null,
        val IndexNumber: Int? = null,
        val BackdropImageTags: List<String> = emptyList(),
        val ParentBackdropItemId: String? = null,
        val UserData: UserData? = null,
    )
    @Serializable private data class Items(val Items: List<Item> = emptyList())

    private fun authHeader(context: Context, token: String? = null): String {
        val device = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "tv"
        return "MediaBrowser Client=\"Aerial\", Device=\"Android TV\", DeviceId=\"aerial-$device\", Version=\"1\"" +
            (token?.let { ", Token=\"$it\"" } ?: "")
    }

    /** Signs in; a failure carries the reason as its message. */
    suspend fun login(context: Context, server: String, user: String, password: String): Result<JellyfinLogin> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base = server.trim().trimEnd('/').let { if (it.startsWith("http")) it else "http://$it" }
                val body = """{"Username":${Json.encodeToString(String.serializer(), user)},"Pw":${Json.encodeToString(String.serializer(), password)}}"""
                val request = Request.Builder().url("$base/Users/AuthenticateByName")
                    .header("Authorization", authHeader(context))
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                val result = http.newCall(request).execute().use { r ->
                    check(r.isSuccessful) { if (r.code == 401) "Wrong username or password" else "The server answered ${r.code}" }
                    json.decodeFromString<AuthResult>(r.body.string())
                }
                JellyfinLogin(base, result.User.Id, result.AccessToken)
            }
        }

    /** Resume items first, then next episodes; refreshed every 5 minutes. Empty when offline. */
    fun items(context: Context, login: JellyfinLogin): Flow<List<WatchNextItem>> = flow {
        while (true) {
            emit(runCatching { fetch(context, login) }.onFailure { Log.d("Aerial", "jellyfin: ${it.message}") }.getOrDefault(emptyList()))
            delay(5 * 60_000L)
        }
    }.flowOn(Dispatchers.IO)

    private fun fetch(context: Context, login: JellyfinLogin): List<WatchNextItem> {
        fun get(path: String): List<Item> = http.newCall(
            Request.Builder().url(login.server + path).header("Authorization", authHeader(context, login.token)).build()
        ).execute().use { r ->
            check(r.isSuccessful) { "HTTP ${r.code}" }
            json.decodeFromString<Items>(r.body.string()).Items
        }
        val resume = get("/Users/${login.userId}/Items/Resume?Limit=10&MediaTypes=Video")
        val nextUp = get("/Shows/NextUp?UserId=${login.userId}&Limit=10")
        return (resume + nextUp).distinctBy { it.Id }.mapIndexed { i, item ->
            val backdrop = when {
                item.BackdropImageTags.isNotEmpty() -> item.Id
                item.ParentBackdropItemId != null -> item.ParentBackdropItemId
                else -> null
            }
            val image = backdrop?.let { "${login.server}/Items/$it/Images/Backdrop?maxWidth=640&api_key=${login.token}" }
                ?: "${login.server}/Items/${item.Id}/Images/Primary?maxWidth=640&api_key=${login.token}"
            val episode = if (item.ParentIndexNumber != null && item.IndexNumber != null)
                "S${item.ParentIndexNumber} E${item.IndexNumber} · ${item.Name}" else null
            WatchNextItem(
                id = i.toLong(), pkg = APP,
                title = item.SeriesName ?: item.Name, subtitle = episode, imageUri = image, intentUri = null,
                progress = item.UserData?.PlayedPercentage?.let { (it / 100).toFloat() },
            )
        }
    }
}
