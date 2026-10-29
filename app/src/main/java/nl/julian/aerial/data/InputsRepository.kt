package nl.julian.aerial.data

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** What's plugged into an input, from the TV's own device-type tag. */
enum class InputDevice { Console, MediaPlayer, Disc, Computer, Audio, Unknown }

@Immutable
data class TvInput(
    val id: String,
    val label: String,
    /** TvInputInfo.TYPE_* (HDMI, component, tuner, …). */
    val type: Int,
    val device: InputDevice = InputDevice.Unknown,
    val passthrough: Boolean,
    /** False when the TV reports nothing plugged in (not every TV reports this). */
    val connected: Boolean,
)

/** The TV's own inputs (HDMI ports, antenna, …), for the input switcher row. */
object InputsRepository {

    fun inputs(context: Context): Flow<List<TvInput>> = callbackFlow {
        val manager = context.getSystemService(TvInputManager::class.java)
        if (manager == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        fun list() = runCatching {
            manager.tvInputList
                // Hardware-backed inputs only (ids end in HW<n>): HDMI ports,
                // component, tuners. Apps such as VLC also register "inputs".
                .filter { !it.isHidden(context) && (it.isPassthroughInput || PORT.containsMatchIn(it.id) && "/HW" in it.id) }
                .map { info ->
                    // Philips stores the literal text "null" for ports without a custom name.
                    val custom = info.loadCustomLabel(context)?.toString()?.takeUnless { it.isBlank() || it.startsWith("null") }
                    val (label, device) = parseLabel(custom ?: friendlyName(info.loadLabel(context).toString()))
                    TvInput(
                        id = info.id,
                        label = label,
                        type = info.type,
                        device = device,
                        passthrough = info.isPassthroughInput,
                        connected = manager.getInputState(info.id) != TvInputManager.INPUT_STATE_DISCONNECTED,
                    )
                }
                .let(::ordered)
        }.getOrDefault(emptyList())

        val callback = object : TvInputManager.TvInputCallback() {
            override fun onInputAdded(inputId: String) { trySend(list()) }
            override fun onInputRemoved(inputId: String) { trySend(list()) }
            override fun onInputUpdated(inputId: String) { trySend(list()) }
            override fun onInputStateChanged(inputId: String, state: Int) { trySend(list()) }
        }
        trySend(list())
        manager.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { manager.unregisterCallback(callback) }
    }.distinctUntilChanged()

    /**
     * HDMI ports first, then other hardware (component, …), tuners last;
     * within each group the TV's own port order (the number that ends the
     * input id). Name only breaks ties.
     */
    internal fun ordered(inputs: List<TvInput>): List<TvInput> = inputs.sortedWith(
        compareBy<TvInput>(
            {
                when (it.type) {
                    TvInputInfo.TYPE_HDMI -> 0
                    TvInputInfo.TYPE_TUNER -> 2
                    else -> 1
                }
            },
            { PORT.find(it.id)?.value?.toIntOrNull() ?: Int.MAX_VALUE },
            { it.label.lowercase() },
        )
    )

    private val PORT = Regex("\\d+$")

    /**
     * Some TVs (Philips) append the device type to the name the user gave an
     * input: "Xbox&GAME_CONSOLE". Split that into a clean name and a type.
     */
    internal fun parseLabel(raw: String): Pair<String, InputDevice> {
        val name = raw.substringBefore('&').trim().ifEmpty { raw }
        val tag = raw.substringAfter('&', "").uppercase()
        val device = when {
            "GAME" in tag || "CONSOLE" in tag -> InputDevice.Console
            "STREAM" in tag || "MEDIA" in tag || "SET_TOP" in tag || "STB" in tag -> InputDevice.MediaPlayer
            "BLU" in tag || "DVD" in tag || "DISC" in tag -> InputDevice.Disc
            "PC" in tag || "COMPUTER" in tag -> InputDevice.Computer
            "AUDIO" in tag || "SOUND" in tag || "AMP" in tag -> InputDevice.Audio
            else -> InputDevice.Unknown
        }
        return name to device
    }

    /** TV vendors' internal input names, in words people use. */
    internal fun friendlyName(raw: String): String {
        val key = raw.replace(" ", "").uppercase()
        return when {
            "DTV" in key || "DVB" in key -> "Antenna (digital)"
            "ATV" in key -> "Antenna (analog)"
            "YPBPR" in key || key == "COMPONENT" -> "Component"
            key == "CVBS" || key == "AV" -> "Composite"
            else -> raw
        }
    }

    /** Dock entries for inputs use this in place of a package name. */
    const val DOCK_PREFIX = "input:"

    /** [switchIntent] for a pinned input, by id; null if the TV no longer has it. */
    fun switchIntent(context: Context, id: String): Intent? {
        val info = context.getSystemService(TvInputManager::class.java)?.getTvInputInfo(id) ?: return null
        return switchIntent(TvInput(id, "", info.type, passthrough = info.isPassthroughInput, connected = true))
    }

    /** Switches the TV to [input] through the system's Live TV / input handler. */
    fun switchIntent(input: TvInput): Intent {
        val uri = if (input.passthrough) TvContract.buildChannelUriForPassthroughInput(input.id)
        else TvContract.buildChannelsUriForInput(input.id)
        return Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
