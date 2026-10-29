package nl.julian.aerial.data

import nl.julian.aerial.theme.Reveal
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

/** Photos and videos from a USB stick or SD card, besides or instead of the aerials. */
enum class OwnMedia(val label: String) { Off("Off"), Mix("Mixed with the aerials"), Only("Instead of the aerials") }

/** What selecting the weather tile shows. [Off] leaves the tile display-only. */
enum class WeatherDetails(val label: String) { Both("Next hours and days"), Hourly("Next hours"), Daily("Next days"), Off("Off") }

/** When the screen dims for the evening. */
enum class NightMode(val label: String) { Off("Off"), Sun("After sunset"), Late("22:00 to 07:00") }

/** How All apps is ordered. */
enum class AppOrder(val label: String) { Manual("Your order"), MostUsed("Most used first") }

/** The panels: tinted (a dark see-through fill) or frosted (the video blurred behind them). */
enum class Glass(val label: String, val sampleWidth: Int) { Tinted("Tinted", 0), Frosted("Frosted", 64), Heavy("Frosted, more blur", 32) }

/** Opening an app: the tile grows to fill the screen first, taking [ms]. */
enum class LaunchAnimation(val label: String, val ms: Int) { Off("Off", 0), Fast("Fast", 220), Slow("Slow", 420) }

/** A focused tile tilts toward the direction focus moved by [degrees], then settles. */
enum class TileDepth(val label: String, val degrees: Float) { Off("Off", 0f), Subtle("Subtle", 5f), Strong("Strong", 10f) }

/** The ring around a focused app banner. */
enum class FocusColor(val label: String) { White("White"), App("The app's colour"), Aerial("The aerial's colour") }

/** Quiet clicks when moving focus or opening something, at [volume]. */
enum class FocusSounds(val label: String, val volume: Float) { Off("Off", 0f), Soft("Soft", 0.25f), Normal("Normal", 0.6f) }

/** How favourite clips are played. */
enum class FavouriteMode(val label: String) { Normal("Like any other clip"), More("More often"), Only("Only favourites") }

/** Which remote buttons the accessibility service sends to Aerial. */
enum class RemoteButtons(val label: String) { Off("Off"), Input("Input button"), InputHome("Input and Home buttons") }

/** What the screensaver shows over the aerial. */
enum class AmbientInfo(val label: String) { All("Clock, weather, clip and music"), Clock("Clock only"), None("Nothing") }

/** How the time is drawn: [Standard] is the colour pills in the top bar and plain digits on the screensaver. */
enum class ClockFace(val label: String) { Standard("Standard"), Thin("Large and thin"), Analog("Analog"), Words("In words") }

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
    /** Screensaver: move the text a little every few minutes, so it can't burn into an OLED panel. */
    val shiftText: Boolean = false,
    val ownMedia: OwnMedia = OwnMedia.Off,
    /** How long each of your own photos stays on screen. */
    val photoSeconds: Int = 15,
    val nightMode: NightMode = NightMode.Off,
    val appOrder: AppOrder = AppOrder.Manual,
    /** A row with the last apps opened, above All apps. */
    val recentRow: Boolean = false,
    /** Times each app was opened from Aerial, and the last few opened (newest first). */
    val launches: Map<String, Int> = emptyMap(),
    val recent: List<String> = emptyList(),
    /** Sleep timer: when the TV goes to standby (System.currentTimeMillis), 0 = off. */
    val sleepAt: Long = 0,
    /** Settings asks GitHub for a newer release (sideloaded builds only). */
    val updateCheck: Boolean = true,
    /** Font size multiplier, in percent. */
    val textSize: Int = 100,
    val highContrast: Boolean = false,
    /** How much night mode darkens the screen, in percent. */
    val nightDim: Int = 40,
    val remoteButtons: RemoteButtons = RemoteButtons.Off,
    /** Minutes without input before the screensaver; 0 = never. */
    val idleMinutes: Int = 2,
    /** App tiles in the theme colour with the app's icon, instead of each app's own banner. */
    val tintedTiles: Boolean = false,
    val glass: Glass = Glass.Tinted,
    val launchAnimation: LaunchAnimation = LaunchAnimation.Off,
    val tileDepth: TileDepth = TileDepth.Off,
    val reveal: Reveal = Reveal.Rise,
    /** Light snow over the aerial in December. */
    val snow: Boolean = false,
    val focusSounds: FocusSounds = FocusSounds.Off,
    /** A gold frame around the screen on 31 December and 1 January. */
    val newYearFrame: Boolean = false,
    val focusColor: FocusColor = FocusColor.White,
    /** Top bar: show the weather tile. */
    val barShowsWeather: Boolean = true,
    val weatherDetails: WeatherDetails = WeatherDetails.Both,
    /** Top bar: show the aerial's name. */
    val barShowsAerial: Boolean = false,
    /** Top bar: show what's playing, with controls, while music plays. */
    val barShowsMusic: Boolean = false,
    /** Top bar: a search button that opens the Assistant. */
    val barShowsSearch: Boolean = false,
    /** Top bar: small status icons next to the date. */
    val statusNetwork: Boolean = false,
    val statusBluetooth: Boolean = false,
    /** Only shows when the TV is running low on storage. */
    val statusStorage: Boolean = false,
    /** Top bar: inputs, alerts and settings as a narrow column of icons. */
    val compactButtons: Boolean = false,
    val clockFormat: ClockFormat = ClockFormat.System,
    val clockFace: ClockFace = ClockFace.Standard,
    val ambientClockFace: ClockFace = ClockFace.Standard,
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

/** True while [NightMode] should dim the screen; [isDay] is from the weather, null until it loads. */
fun LauncherConfig.isNight(isDay: Boolean?): Boolean {
    val hour = LocalTime.now().hour
    return when (nightMode) {
        NightMode.Off -> false
        NightMode.Sun -> !(isDay ?: (hour in 7..18))
        NightMode.Late -> hour >= 22 || hour < 7
    }
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

    /** All settings as JSON, for a backup. */
    suspend fun export(): String = json.encodeToString(LauncherConfig.serializer(), config.first())

    /** Replaces all settings with a backup; false if [text] isn't one. */
    suspend fun import(text: String): Boolean {
        val restored = runCatching { json.decodeFromString<LauncherConfig>(text) }.getOrNull() ?: return false
        update { restored.copy(sleepAt = 0) }
        return true
    }

    companion object {
        @Volatile
        private var instance: ConfigStore? = null

        fun get(context: Context): ConfigStore =
            instance ?: synchronized(this) { instance ?: ConfigStore(context).also { instance = it } }
    }
}
