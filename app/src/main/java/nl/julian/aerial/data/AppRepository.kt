package nl.julian.aerial.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * @Immutable lets Compose skip tiles whose entry didn't change, so a focus move
 * doesn't recompose (and re-upload) every visible bitmap.
 */
@Immutable
data class AppEntry(
    val pkg: String,
    val label: String,
    /** 16:9 leanback banner if the app ships one, pre-rasterized. */
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
    /** lastUpdateTime — lets a rescan reuse unchanged entries as-is. */
    val stamp: Long,
    /** Preinstalled apps can't be uninstalled, only disabled in Settings. */
    val isSystem: Boolean,
)

/**
 * Installed-app scan with an on-disk banner/icon cache.
 * Adapted from couchy-launcher's AppRepository (GPL-3.0, see NOTICE).
 */
object AppRepository {

    @Volatile
    var memoryCache: List<AppEntry>? = null
        private set

    /** The app list as of the last scan, next to the icon cache. */
    private const val LIST_FILE = "apps.json"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class SavedList(val locale: String, val bannerW: Int, val apps: List<SavedApp>)

    @Serializable
    private data class SavedApp(val pkg: String, val label: String, val stamp: Long, val isSystem: Boolean, val banner: Boolean, val icon: Boolean)

    /**
     * The apps as of the last scan, read from disk: no package manager, only
     * the cached banners. After a cold start (TVs often kill the launcher
     * behind a streaming app) the grid shows from this at once, and [scan]
     * checks it after, reusing every entry that hasn't changed.
     */
    suspend fun saved(context: Context): List<AppEntry>? = memoryCache ?: withContext(scanDispatcher) {
        val list = runCatching {
            json.decodeFromString<SavedList>(File(context.filesDir, LIST_FILE).readText())
        }.getOrNull() ?: return@withContext null
        // App names come in the TV's language; a different one means a fresh scan.
        if (list.locale != systemLocale(context)) return@withContext null
        val dir = File(context.filesDir, "iconcache")
        fun decode(name: String) = runCatching { BitmapFactory.decodeFile(File(dir, name).absolutePath, hardware()) }.getOrNull()
            ?.asImageBitmap()
        coroutineScope {
            list.apps.map { a ->
                async {
                    AppEntry(
                        pkg = a.pkg, label = a.label,
                        banner = if (a.banner) decode("${a.pkg}-${a.stamp}-b${list.bannerW}.webp") else null,
                        icon = if (a.icon) decode("${a.pkg}-${a.stamp}-i128.webp") else null,
                        stamp = a.stamp, isSystem = a.isSystem,
                    )
                }
            }.awaitAll()
        }.also { if (memoryCache == null) memoryCache = it }
    }

    private fun systemLocale(context: Context) = context.resources.configuration.locales[0].toLanguageTag()

    // Leave a core free for the render thread while banners decode.
    private val scanDispatcher = Dispatchers.IO.limitedParallelism(
        (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(2)
    )

    suspend fun scan(context: Context): List<AppEntry> = coroutineScope {
        val pm = context.packageManager
        val cacheDir = File(context.filesDir, "iconcache").apply { mkdirs() }
        // Raster banners at the size they paint at (largest tile ≈ 220dp wide).
        val density = context.resources.displayMetrics.density
        val bannerW = (220 * density).toInt().coerceIn(320, 720)
        val bannerH = bannerW * 9 / 16
        val validCacheNames = java.util.Collections.synchronizedSet(HashSet<String>())

        val candidates = LinkedHashMap<String, android.content.pm.ResolveInfo>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (pkg != context.packageName && pkg !in candidates) candidates[pkg] = ri
            }
        }

        val previous = memoryCache?.associateBy { it.pkg }
        val entries = candidates.map { (pkg, ri) ->
            async(scanDispatcher) {
                val stamp = runCatching { pm.getPackageInfo(pkg, 0).lastUpdateTime }.getOrDefault(0L)
                val bannerName = "$pkg-$stamp-b$bannerW.webp"
                val iconName = "$pkg-$stamp-i128.webp"

                previous?.get(pkg)?.takeIf { it.stamp == stamp }?.let { cached ->
                    if (cached.banner != null) validCacheNames.add(bannerName)
                    if (cached.icon != null) validCacheNames.add(iconName)
                    return@async cached
                }

                val ai = ri.activityInfo
                val bannerDrawable = runCatching { ai.loadBanner(pm) }.getOrNull()
                val banner = bannerDrawable?.let { d ->
                    cachedBitmap(cacheDir, bannerName) {
                        runCatching { d.toBitmap(bannerW, bannerH, Bitmap.Config.ARGB_8888) }.getOrNull()
                    }
                }
                // Always loaded: tinted tiles show the icon even for apps with a banner.
                val icon = cachedBitmap(cacheDir, iconName) {
                    runCatching { ri.loadIcon(pm)?.toBitmap(128, 128) }.getOrNull()
                }
                if (banner != null) validCacheNames.add(bannerName)
                if (icon != null) validCacheNames.add(iconName)

                AppEntry(
                    pkg = pkg,
                    label = runCatching { ri.loadLabel(pm)?.toString() }.getOrNull() ?: pkg,
                    banner = banner?.asImageBitmap(),
                    icon = icon?.asImageBitmap(),
                    stamp = stamp,
                    isSystem = (ai.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
        }.awaitAll()

        cacheDir.listFiles()?.forEach { f ->
            if (f.name !in validCacheNames) runCatching { f.delete() }
        }
        val result = entries.sortedBy { it.label.lowercase() }
        // Same content → same instance, so nothing downstream recomposes.
        val listFile = File(context.filesDir, LIST_FILE)
        if (result == memoryCache && listFile.exists()) return@coroutineScope memoryCache!!
        memoryCache = result
        runCatching {
            val saved = SavedList(systemLocale(context), bannerW, result.map {
                SavedApp(it.pkg, it.label, it.stamp, it.isSystem, it.banner != null, it.icon != null)
            })
            listFile.writeText(json.encodeToString(SavedList.serializer(), saved))
        }
        result
    }

    /** Emits Unit whenever an app is installed, removed or updated. */
    fun packageChanges(context: Context): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { trySend(Unit) }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    fun launchIntent(context: Context, pkg: String): Intent? {
        if (pkg.startsWith(InputsRepository.DOCK_PREFIX)) {
            return InputsRepository.switchIntent(context, pkg.removePrefix(InputsRepository.DOCK_PREFIX))
        }
        val pm = context.packageManager
        return (pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg))
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * Banners and icons as hardware bitmaps: they live in graphics memory, not
     * the app's heap, and need no upload when first drawn. Less heap also makes
     * the launcher less likely to be killed behind other apps.
     */
    private fun hardware() = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.HARDWARE }

    /** Reads [dir]/[name]; on a miss runs [create] and persists it as lossless WebP. */
    private fun cachedBitmap(dir: File, name: String, create: () -> Bitmap?): Bitmap? {
        val f = File(dir, name)
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath, hardware())?.let { return it }
        val bmp = create() ?: return null
        runCatching {
            f.outputStream().use { out ->
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    bmp.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
                } else {
                    @Suppress("DEPRECATION")
                    bmp.compress(Bitmap.CompressFormat.WEBP, 100, out)
                }
            }
        }
        // Not recycled: toBitmap() can hand back the app's own drawable bitmap, shared by Resources.
        return bmp.copy(Bitmap.Config.HARDWARE, false) ?: bmp
    }
}
