package nl.julian.aerial.wallpaper

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * [kind] is the scenery: city, nature, water (underwater), space, or "" (neither, e.g. a fireplace).
 * [time] is "day", "night", or "" for clips that suit any time (underwater, fireplaces).
 */
data class Aerial(val title: String, val uri: String, val kind: String = "", val time: String = "")

/**
 * The bundled aerial manifest: 289 1080p H.264 clips (Apple, Amazon Fire TV,
 * community). Manifest from couchy-launcher (GPL-3.0, see NOTICE).
 */
object Aerials {
    @Serializable
    private data class Entry(val t: String = "", val u: String, val k: String = "", val d: String = "")

    @Volatile
    private var cache: List<Aerial>? = null

    fun load(context: Context): List<Aerial> = cache ?: runCatching {
        val json = context.assets.open("aerials.json").bufferedReader().use { it.readText() }
        Json { ignoreUnknownKeys = true }.decodeFromString<List<Entry>>(json)
            // Amazon's "… Motion" pieces are abstract motion art, not aerial footage.
            .filterNot { it.t.endsWith(" Motion") }
            .map { Aerial(it.t, it.u, it.k, it.d) }
    }.getOrDefault(emptyList()).also { cache = it }
}

/**
 * Shuffle bag: every clip plays once before any repeats, and a clip never
 * plays twice in a row across refills.
 */
class AerialQueue(private val all: List<Aerial>) {
    private val upcoming = ArrayDeque<Aerial>()
    private var last: Aerial? = null

    private fun refill() {
        val bag = all.shuffled().toMutableList()
        if (bag.size > 1 && bag.first() == last) bag.add(bag.removeAt(0))
        upcoming.addAll(bag)
    }

    /** Moves the first clip matching [prefer] to the front (e.g. one already cached). */
    fun preferFirst(prefer: (Aerial) -> Boolean) {
        if (upcoming.isEmpty()) refill()
        val hit = upcoming.firstOrNull(prefer) ?: return
        upcoming.remove(hit)
        upcoming.addFirst(hit)
    }

    fun next(): Aerial {
        if (upcoming.isEmpty()) refill()
        return upcoming.removeFirst().also { last = it }
    }

    /** The clip [next] will return. */
    fun peek(): Aerial {
        if (upcoming.isEmpty()) refill()
        return upcoming.first()
    }
}
