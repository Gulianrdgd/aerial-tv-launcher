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

@Immutable
data class TvInput(
    val id: String,
    val label: String,
    /** TvInputInfo.TYPE_* (HDMI, component, tuner, …). */
    val type: Int,
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
                .filterNot { it.isHidden(context) }
                .map { info ->
                    TvInput(
                        id = info.id,
                        label = (info.loadCustomLabel(context) ?: info.loadLabel(context)).toString(),
                        type = info.type,
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

    /** Connected inputs first; then HDMI before other hardware before tuners; then by name. */
    internal fun ordered(inputs: List<TvInput>): List<TvInput> = inputs.sortedWith(
        compareBy<TvInput>(
            { !it.connected },
            {
                when (it.type) {
                    TvInputInfo.TYPE_HDMI -> 0
                    TvInputInfo.TYPE_TUNER -> 2
                    else -> 1
                }
            },
            { it.label.lowercase() },
        )
    )

    /** Switches the TV to [input] through the system's Live TV / input handler. */
    fun switchIntent(input: TvInput): Intent {
        val uri = if (input.passthrough) TvContract.buildChannelUriForPassthroughInput(input.id)
        else TvContract.buildChannelsUriForInput(input.id)
        return Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
