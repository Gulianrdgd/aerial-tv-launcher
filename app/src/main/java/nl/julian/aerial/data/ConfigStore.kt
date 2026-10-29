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

/** Where the colours come from: the aerial on screen, one colour you pick, or black and white. */
enum class ThemeSource(val label: String) { Aerial("From the aerial"), Fixed("One colour"), Mono("Black and white") }

/** The colours "One colour" cycles through. */
val THEME_COLORS = listOf(
    0xFF4A5BA8.toInt() to "Blue", 0xFF2E7D6B.toInt() to "Green", 0xFFB3541E.toInt() to "Orange",
    0xFFB0305C.toInt() to "Pink", 0xFF6B4FA8.toInt() to "Purple", 0xFF8A7A2E.toInt() to "Gold",
)

/** The home screen's sections below the top bar, in the order the user chose. */
enum class HomeSection(val label: String) {
    Dock("Dock"), WatchNext("Continue watching"), Jellyfin("Jellyfin"), Plex("Plex"), Home("Home Assistant"), Recent("Recent"), Apps("All apps"), Quick("Quick settings"),
}

/** The quick settings tiles. */
enum class QuickTileKind(val label: String) {
    Network("Network"), Bluetooth("Bluetooth"), Display("Display"), Scenery("Scenery"),
    Screensaver("Screensaver"), Sleep("Sleep timer"), PictureOff("Picture off"),
}

/** Remote buttons a shortcut can be put on (through the Remote buttons accessibility service). */
enum class RemoteKey(val label: String, val keyCode: Int) {
    HoldHome("Hold Home", android.view.KeyEvent.KEYCODE_HOME),
    Red("Red button", android.view.KeyEvent.KEYCODE_PROG_RED),
    Green("Green button", android.view.KeyEvent.KEYCODE_PROG_GREEN),
    Yellow("Yellow button", android.view.KeyEvent.KEYCODE_PROG_YELLOW),
    Blue("Blue button", android.view.KeyEvent.KEYCODE_PROG_BLUE),
}

/** The parts of the top bar, left to right by default. */
enum class TopTile(val label: String) { Clock("Clock"), Middle("Aerial name or Now playing"), Weather("Weather"), Buttons("Buttons") }

/** When apps may be opened in kids mode. */
enum class KidsHours(val label: String, val from: Int, val until: Int) {
    Any("Any time", 0, 24), Day("07:00 to 19:00", 7, 19), Long("07:00 to 20:00", 7, 20),
}

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

/** A folder tile on the home screen: [apps] (packages) are shown inside it instead of in All apps. */
@Serializable
data class Folder(val id: String, val name: String, val color: Int = THEME_COLORS[0].first, val apps: List<String> = emptyList())

@Immutable
@Serializable
data class LauncherConfig(
    /** Dock packages in order; null until the user changes it (defaults apply). */
    val dock: List<String>? = null,
    /** Manual order of the app grid; apps not listed follow alphabetically. */
    val order: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    /** Names the user gave apps, by package. */
    val names: Map<String, String> = emptyMap(),
    /** Apps shown with their icon and name instead of their banner. */
    val iconOnly: Set<String> = emptySet(),
    /** Package of the icon pack to use, or null for the apps' own icons. */
    val iconPack: String? = null,
    val folders: List<Folder> = emptyList(),
    /** Apps left out of Continue Watching. */
    val watchNextHidden: Set<String> = emptySet(),
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
    /** Web links to videos, live streams or photos, played with your own media. */
    val links: List<String> = emptyList(),
    val nightMode: NightMode = NightMode.Off,
    val appOrder: AppOrder = AppOrder.Manual,
    /** Section order; sections missing here (added in a later version) follow in their default order. */
    val sectionOrder: List<HomeSection> = HomeSection.entries,
    /** The dock can't be hidden: Home brings focus back to it. */
    val hiddenSections: Set<HomeSection> = setOf(HomeSection.Recent),
    /** A section to put first in the morning (06–12) and in the evening (18–24); null = the usual order. */
    val morningFirst: HomeSection? = null,
    val eveningFirst: HomeSection? = null,
    /** Quick settings tile order, and the ones turned off. */
    val quickOrder: List<QuickTileKind> = QuickTileKind.entries,
    val quickHidden: Set<QuickTileKind> = emptySet(),
    val topBarOrder: List<TopTile> = TopTile.entries,
    /** Times each app was opened from Aerial, and the last few opened (newest first). */
    val launches: Map<String, Int> = emptyMap(),
    val recent: List<String> = emptyList(),
    /** Sleep timer: when the TV goes to standby (System.currentTimeMillis), 0 = off. */
    val sleepAt: Long = 0,
    /** Settings asks GitHub for a newer release (sideloaded builds only). */
    val updateCheck: Boolean = true,
    /** Warn on the home screen when Aerial is no longer the default Home app. */
    val homeWarning: Boolean = true,
    /** Keep Aerial out of the recent apps screen. */
    val hideFromRecents: Boolean = false,
    /** Font size multiplier, in percent. */
    val textSize: Int = 100,
    val highContrast: Boolean = false,
    /** How much night mode darkens the screen, in percent. */
    val nightDim: Int = 40,
    val remoteButtons: RemoteButtons = RemoteButtons.Off,
    /** What each remote key does: "inputs", "screensaver" or "app:<package>". */
    val shortcuts: Map<RemoteKey, String> = emptyMap(),
    /** When the TV turns on: null = the home screen, "app:<package>" or "input:<id>". */
    val startup: String? = null,
    /** Signed-in Jellyfin server for the Jellyfin row; null = not connected. */
    val jellyfin: JellyfinLogin? = null,
    /** Signed-in Plex server for the Plex row; null = not connected. */
    val plex: PlexLogin? = null,
    /** Kids mode: only [kidsApps], only during [kidsHours]; the PIN unlocks the top bar buttons. */
    /** Home Assistant: buttons in a row, and the doorbell camera; null = not connected. */
    val homeAssistant: HaConfig? = null,
    val kidsMode: Boolean = false,
    val kidsPin: String = "",
    val kidsApps: List<String> = emptyList(),
    val kidsHours: KidsHours = KidsHours.Any,
    /** Minutes without input before the screensaver; 0 = never. */
    val idleMinutes: Int = 2,
    /** App tiles in the theme colour with the app's icon, instead of each app's own banner. */
    val tintedTiles: Boolean = false,
    /** Tiles per row in the dock and the app rows: fewer means bigger tiles. */
    val tilesPerRow: Int = COLUMNS,
    val glass: Glass = Glass.Tinted,
    val themeSource: ThemeSource = ThemeSource.Aerial,
    /** With [ThemeSource.Fixed]: one of [THEME_COLORS]. */
    val themeColor: Int = THEME_COLORS[0].first,
    /** Panel opacity, in percent. */
    val panelOpacity: Int = 62,
    /** Corner radius, in percent of the default. */
    val roundness: Int = 100,
    /** Added to every font weight: -100 lighter, 0, or 100 bolder. */
    val fontWeight: Int = 0,
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

/** The sections to show, in order. */
val LauncherConfig.sections: List<HomeSection>
    get() = (sectionOrder + HomeSection.entries).distinct().filter { it == HomeSection.Dock || it !in hiddenSections }

/** [sections] at [hour]: the morning or evening section moves to the top (and shows even if hidden). */
fun LauncherConfig.sectionsAt(hour: Int): List<HomeSection> {
    val first = when (hour) {
        in 6..11 -> morningFirst
        in 18..23 -> eveningFirst
        else -> null
    } ?: return sections
    return (listOf(first) + sections).distinct()
}

/** In kids mode, whether apps may be opened at [hour]. */
fun LauncherConfig.kidsAllowedAt(hour: Int) = !kidsMode || hour in kidsHours.from until kidsHours.until

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

    /** All settings as JSON, for a backup; without the Jellyfin and Home Assistant tokens, as Downloads is readable by other apps. */
    suspend fun export(): String = json.encodeToString(LauncherConfig.serializer(), config.first().copy(jellyfin = null, plex = null, homeAssistant = null))

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
