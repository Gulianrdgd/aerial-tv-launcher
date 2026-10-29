package nl.julian.aerial.data

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Home Assistant connection and what to show: [buttons] in a row, [doorbell] that pops up [camera]. */
@Serializable
data class HaConfig(
    val server: String,
    val token: String,
    val buttons: List<String> = emptyList(),
    val doorbell: String? = null,
    val camera: String? = null,
)

/** One entity's state, as Home Assistant reports it. */
data class HaEntity(val id: String, val name: String, val state: String) {
    val domain get() = id.substringBefore('.')
    val on get() = state == "on" || state == "open" || state == "playing"
}

/** Home Assistant's REST API with a long-lived access token. */
object HomeAssistant {
    /** Entities that can be buttons: switched or run with one press. */
    val BUTTON_DOMAINS = setOf("light", "switch", "scene", "script", "fan", "input_boolean")

    private val http by lazy { OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build() }
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable private data class State(val entity_id: String, val state: String, val attributes: JsonObject = JsonObject(emptyMap()))

    private fun request(ha: HaConfig, path: String) =
        Request.Builder().url(ha.server.trimEnd('/') + path).header("Authorization", "Bearer ${ha.token}")

    /** Every entity, or a failure with the reason. */
    suspend fun states(ha: HaConfig): Result<List<HaEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(request(ha, "/api/states").build()).execute().use { r ->
                check(r.isSuccessful) { if (r.code == 401) "The token was refused" else "The server answered ${r.code}" }
                json.decodeFromString<List<State>>(r.body.string()).map {
                    HaEntity(it.entity_id, it.attributes["friendly_name"]?.jsonPrimitive?.content ?: it.entity_id, it.state)
                }
            }
        }
    }

    /** Presses a button: toggles lights and switches, runs scenes and scripts. */
    suspend fun press(ha: HaConfig, id: String) = withContext(Dispatchers.IO) {
        val domain = id.substringBefore('.')
        val service = if (domain == "scene" || domain == "script") "turn_on" else "toggle"
        runCatching {
            http.newCall(
                request(ha, "/api/services/$domain/$service")
                    .post("""{"entity_id":"$id"}""".toRequestBody("application/json".toMediaType())).build()
            ).execute().close()
        }
    }

    /** One entity's state, or null when unreachable. */
    suspend fun state(ha: HaConfig, id: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(request(ha, "/api/states/$id").build()).execute().use { r ->
                if (r.isSuccessful) json.decodeFromString<State>(r.body.string()).state else null
            }
        }.getOrNull()
    }

    /** A still from [camera], or null. */
    suspend fun camera(ha: HaConfig, camera: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(request(ha, "/api/camera_proxy/$camera").build()).execute().use { r ->
                if (r.isSuccessful) BitmapFactory.decodeStream(r.body.byteStream()) else null
            }
        }.getOrNull()
    }
}
