package nl.julian.aerial.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nl.julian.aerial.theme.Reveal

/** The look of the launcher, without apps, places or anything personal: what "Share this look" saves. */
@Serializable
data class Look(
    val themeSource: ThemeSource = ThemeSource.Aerial,
    val themeColor: Int = THEME_COLORS[0].first,
    val panelOpacity: Int = 62,
    val roundness: Int = 100,
    val fontWeight: Int = 0,
    val glass: Glass = Glass.Tinted,
    val reveal: Reveal = Reveal.Rise,
    val tileDepth: TileDepth = TileDepth.Off,
    val focusColor: FocusColor = FocusColor.White,
    val launchAnimation: LaunchAnimation = LaunchAnimation.Off,
    val clockFace: ClockFace = ClockFace.Standard,
    val ambientClockFace: ClockFace = ClockFace.Standard,
    val tintedTiles: Boolean = false,
    val textSize: Int = 100,
    val highContrast: Boolean = false,
    val focusSounds: FocusSounds = FocusSounds.Off,
    val snow: Boolean = false,
    val newYearFrame: Boolean = false,
)

fun LauncherConfig.look() = Look(
    themeSource, themeColor, panelOpacity, roundness, fontWeight, glass, reveal, tileDepth, focusColor, launchAnimation,
    clockFace, ambientClockFace, tintedTiles, textSize, highContrast, focusSounds, snow, newYearFrame,
)

fun LauncherConfig.withLook(l: Look) = copy(
    themeSource = l.themeSource, themeColor = l.themeColor, panelOpacity = l.panelOpacity,
    roundness = l.roundness, fontWeight = l.fontWeight, glass = l.glass,
    reveal = l.reveal, tileDepth = l.tileDepth, focusColor = l.focusColor, launchAnimation = l.launchAnimation,
    clockFace = l.clockFace, ambientClockFace = l.ambientClockFace, tintedTiles = l.tintedTiles,
    textSize = l.textSize, highContrast = l.highContrast, focusSounds = l.focusSounds, snow = l.snow,
    newYearFrame = l.newYearFrame,
)

private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

fun Look.encode(): String = json.encodeToString(Look.serializer(), this)

fun decodeLook(text: String): Look? = runCatching { json.decodeFromString(Look.serializer(), text) }.getOrNull()
