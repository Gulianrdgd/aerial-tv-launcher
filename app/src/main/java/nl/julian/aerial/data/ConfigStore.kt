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

/** Aerial collections, matching the `s` field of aerials.json. */
enum class AerialSource(val label: String, val keys: Set<String>) {
    All("All", emptySet()),
    Apple("Apple TV", setOf("A")),
    Amazon("Fire TV", setOf("Z")),
    Community("Community", setOf("C1", "C2")),
}

/** What the aerials show, matching the `k` tag in aerials.json. */
enum class Scenery(val label: String, val kind: String?) {
    All("Everything", null),
    City("Cities", "city"),
    Nature("Nature", "nature"),
    Underwater("Underwater", "water"),
    Space("From space", "space"),
}

@Immutable
@Serializable
data class LauncherConfig(
    /** Dock packages in order; null until the user changes it (defaults apply). */
    val dock: List<String>? = null,
    /** Manual order of the app grid; apps not listed follow alphabetically. */
    val order: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val aerialSource: AerialSource = AerialSource.All,
    val scenery: Scenery = Scenery.All,
    /** Minutes without input before the screensaver; 0 = never. */
    val idleMinutes: Int = 2,
)

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
