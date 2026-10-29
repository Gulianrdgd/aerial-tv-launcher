package nl.julian.aerial.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * For sideloaded builds: is there a newer release on GitHub? F-Droid
 * installs are skipped, F-Droid updates those itself.
 */
object UpdateCheck {
    const val PAGE = "https://github.com/Gulianrdgd/aerial-tv-launcher/releases/latest"
    private const val API = "https://api.github.com/repos/Gulianrdgd/aerial-tv-launcher/releases/latest"

    @Serializable private data class Release(val tag_name: String)

    /** The newer version's name ("0.3.0"), or null if up to date, from F-Droid, or offline. */
    suspend fun newer(context: Context): String? = withContext(Dispatchers.IO) {
        if (fromFdroid(context)) return@withContext null
        val mine = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: return@withContext null
        runCatching {
            val conn = URL(API).openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            val body = conn.inputStream.use { it.readBytes().decodeToString() }
            val latest = Json { ignoreUnknownKeys = true }.decodeFromString<Release>(body).tag_name.removePrefix("v")
            latest.takeIf { isNewer(it, mine) }
        }.getOrNull()
    }

    internal fun isNewer(a: String, b: String): Boolean {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    private fun fromFdroid(context: Context): Boolean {
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= 30) context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            else @Suppress("DEPRECATION") context.packageManager.getInstallerPackageName(context.packageName)
        }.getOrNull()
        return installer != null && ("fdroid" in installer || installer == "com.looker.droidify")
    }
}
