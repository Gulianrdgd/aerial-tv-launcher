package nl.julian.aerial.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalTime

/** What the aerials show, matching the `k` tag in aerials.json. */
enum class Scenery(val label: String, val kind: String?) {
    All("Everything", null),
    City("Cities", "city"),
    Nature("Nature", "nature"),
    Underwater("Underwater", "water"),
    Space("From space", "space"),
}

/** When day and night clips play, matching the `d` tag in aerials.json. */
enum class AerialTime(val label: String) { Any("Any time"), Sun("Follow the sun"), Day("Day only"), Night("Night only") }

/** Which version of Apple's clips plays. [Auto] picks the best one the TV can show. */
enum class VideoQuality(val label: String) { HD("1080p"), UHD("4K"), Hdr("4K HDR"), Auto("Best this TV can show") }

/** How favourite clips are played. */
enum class FavouriteMode(val label: String) { Normal("Like any other clip"), More("More often"), Only("Only favourites") }

/** Which remote buttons the accessibility service sends to Aerial. */
enum class RemoteButtons(val label: String) { Off("Off"), Input("Input button"), InputHome("Input and Home buttons") }

/** What the screensaver shows over the aerial. */
enum class AmbientInfo(val label: String) { All("Clock, weather, clip and music"), Clock("Clock only"), None("Nothing") }

enum class ClockFormat(val label: String) { System("Same as the TV"), H24("24-hour"), H12("12-hour") }

enum class TempUnit(val label: String) { Auto("Automatic"), Celsius("Celsius (°C)"), Fahrenheit("Fahrenheit (°F)") }

@Immutable
@Serializable
data class LauncherConfig(
    /** Dock packages in order; null until the user changes it (defaults apply). */
    val dock: List<String>? = null,
    /** Manual order of the app grid; apps not listed follow alphabetically. */
    val order: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val scenery: Scenery = Scenery.All,
    val aerialTime: AerialTime = AerialTime.Any,
    val ambientInfo: AmbientInfo = AmbientInfo.All,
    /** Clip URIs marked as favourite, and clips never to play again. */
    val favourites: Set<String> = emptySet(),
    val skipped: Set<String> = emptySet(),
    val favouriteMode: FavouriteMode = FavouriteMode.Normal,
    val videoQuality: VideoQuality = VideoQuality.HD,
    /** Screensaver: Apple's caption of where the camera is, in place of the scenery. */
    val showPlaces: Boolean = false,
    val remoteButtons: RemoteButtons = RemoteButtons.Off,
    /** Minutes without input before the screensaver; 0 = never. */
    val idleMinutes: Int = 2,
    /** App tiles in the theme colour with the app's icon, instead of each app's own banner. */
    val tintedTiles: Boolean = false,
    /** Top bar: show the weather tile. */
    val barShowsWeather: Boolean = true,
    /** Top bar: show the aerial's name. */
    val barShowsAerial: Boolean = false,
    /** Top bar: show what's playing, with controls, while music plays. */
    val barShowsMusic: Boolean = false,
    /** Top bar: a search button that opens the Assistant. */
    val barShowsSearch: Boolean = false,
    /** Top bar: inputs, alerts and settings as a narrow column of icons. */
    val compactButtons: Boolean = false,
    val clockFormat: ClockFormat = ClockFormat.System,
    val tempUnit: TempUnit = TempUnit.Auto,
    /** City for the weather; empty = derived from the TV's timezone. */
    val weatherCity: String = "",
)

/**
 * "day" or "night" for [AerialTime], or null for any time. [isDay] comes
 * from the weather (it knows sunrise and sunset); until it loads, the
 * clock decides.
 */
fun LauncherConfig.aerialTimeTag(isDay: Boolean?): String? = when (aerialTime) {
    AerialTime.Any -> null
    AerialTime.Day -> "day"
    AerialTime.Night -> "night"
    AerialTime.Sun -> if (isDay ?: (LocalTime.now().hour in 7..18)) "day" else "night"
}

private val Context.dataStore by preferencesDataStore("launcher")

/** All launcher settings, stored as one JSON document. */
class ConfigStore(context: Context) {
    private val store = context.applicationContext.dataStore
    private val key = stringPreferencesKey("config")
    private val json = Json { ignoreUnknownKeys = true }

    val config: Flow<LauncherConfig> = store.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString<LauncherConfig>(it) }.getOrNull() } ?: LauncherConfig()
    }

    suspend fun update(change: (LauncherConfig) -> LauncherConfig) {
        store.edit { prefs ->
            val current = prefs[key]?.let { runCatching { json.decodeFromString<LauncherConfig>(it) }.getOrNull() }
                ?: LauncherConfig()
            prefs[key] = json.encodeToString(LauncherConfig.serializer(), change(current))
        }
    }

    companion object {
        @Volatile
        private var instance: ConfigStore? = null

        fun get(context: Context): ConfigStore =
            instance ?: synchronized(this) { instance ?: ConfigStore(context).also { instance = it } }
    }
}
