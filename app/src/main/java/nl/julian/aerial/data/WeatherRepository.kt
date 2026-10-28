package nl.julian.aerial.data

import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

enum class Sky { Clear, PartlyCloudy, Cloudy, Fog, Rain, Snow, Storm }

@Immutable
data class Weather(
    val place: String,
    val temperature: Int,
    val sky: Sky,
    val isDay: Boolean,
    /** Precipitation (mm) per 15 minutes for the next 2 hours, 8 values. */
    val rain: List<Float> = emptyList(),
) {
    /** True when any meaningful rain (≥ 0.1 mm per quarter) is expected in the next 2 hours. */
    val rainExpected: Boolean get() = rain.any { it >= 0.1f }
}

/**
 * Current conditions from Open-Meteo (no API key, no account). The place is
 * derived from the TV's timezone, e.g. Europe/Amsterdam → Amsterdam, so no
 * location permission or IP lookup is needed.
 */
object WeatherRepository {
    private const val REFRESH_MS = 30 * 60_000L
    private const val RETRY_MS = 2 * 60_000L

    private val http by lazy {
        OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
    }
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable private data class GeoResult(val name: String, val latitude: Double, val longitude: Double)
    @Serializable private data class GeoResponse(val results: List<GeoResult> = emptyList())
    @Serializable private data class Current(
        val temperature_2m: Double,
        val weather_code: Int,
        val is_day: Int = 1,
    )
    @Serializable private data class Minutely(val precipitation: List<Double?> = emptyList())
    @Serializable private data class ForecastResponse(val current: Current, val minutely_15: Minutely? = null)

    /** Geocoded places by the name that was looked up. */
    private val places = java.util.concurrent.ConcurrentHashMap<String, GeoResult>()
    /** Last result per (place name, unit), with when it was fetched. */
    private val last = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Weather>>()

    /**
     * Emits conditions for [city] (empty = from the TV's timezone) in [unit],
     * refreshed every 30 min; null while unknown (offline, unknown place).
     * Re-collecting reuses a still-fresh result, so returning to the home
     * screen doesn't hit the network.
     */
    fun weather(city: String = "", unit: TempUnit = TempUnit.Auto): Flow<Weather?> = flow {
        val name = city.ifBlank { cityFromTimeZone() }
        if (name == null) {
            emit(null)
            return@flow
        }
        val fahrenheit = when (unit) {
            TempUnit.Celsius -> false
            TempUnit.Fahrenheit -> true
            TempUnit.Auto -> Locale.getDefault().country in setOf("US", "LR", "MM")
        }
        val key = "$name|$fahrenheit"
        last[key]?.let { (at, w) ->
            val age = System.currentTimeMillis() - at
            if (age < REFRESH_MS) {
                emit(w)
                delay(REFRESH_MS - age)
            }
        }
        while (true) {
            val w = runCatching { fetch(name, fahrenheit) }
                .onFailure { Log.d("Aerial", "weather failed: ${it.message}") }
                .getOrNull()
            if (w != null) last[key] = System.currentTimeMillis() to w
            emit(w)
            delay(if (w == null) RETRY_MS else REFRESH_MS)
        }
    }.flowOn(Dispatchers.IO)

    /** Looks up [name]; returns the place's proper name, or null if it isn't found. */
    suspend fun findPlace(name: String): String? = withContext(Dispatchers.IO) {
        runCatching { geocode(name.trim())?.also { places[name.trim()] = it }?.name }.getOrNull()
    }

    private fun fetch(name: String, fahrenheit: Boolean): Weather? {
        val geo = places[name] ?: geocode(name)?.also { places[name] = it } ?: return null
        val url = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", geo.latitude.toString())
            .addQueryParameter("longitude", geo.longitude.toString())
            .addQueryParameter("current", "temperature_2m,weather_code,is_day")
            .addQueryParameter("minutely_15", "precipitation")
            .addQueryParameter("forecast_minutely_15", "8")
            .apply { if (fahrenheit) addQueryParameter("temperature_unit", "fahrenheit") }
            .build()
        val forecast = json.decodeFromString<ForecastResponse>(get(url.toString()))
        val current = forecast.current
        return Weather(
            place = geo.name,
            temperature = Math.round(current.temperature_2m).toInt(),
            sky = skyFor(current.weather_code),
            isDay = current.is_day == 1,
            rain = forecast.minutely_15?.precipitation.orEmpty().map { (it ?: 0.0).toFloat() },
        )
    }

    private fun geocode(city: String): GeoResult? {
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", city)
            .addQueryParameter("count", "1")
            .build()
        return json.decodeFromString<GeoResponse>(get(url.toString())).results.firstOrNull()
    }

    private fun get(url: String): String =
        http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            check(r.isSuccessful) { "HTTP ${r.code}" }
            r.body.string()
        }

    /** "America/Argentina/Buenos_Aires" → "Buenos Aires"; null for Etc/UTC-style zones. */
    internal fun cityFromTimeZone(id: String = TimeZone.getDefault().id): String? {
        if (!id.contains('/') || id.startsWith("Etc/")) return null
        return id.substringAfterLast('/').replace('_', ' ')
    }

    /** WMO weather interpretation codes → our seven skies. */
    internal fun skyFor(code: Int): Sky = when (code) {
        0 -> Sky.Clear
        1, 2 -> Sky.PartlyCloudy
        3 -> Sky.Cloudy
        45, 48 -> Sky.Fog
        in 51..67, in 80..82 -> Sky.Rain
        in 71..77, 85, 86 -> Sky.Snow
        in 95..99 -> Sky.Storm
        else -> Sky.Cloudy
    }
}
