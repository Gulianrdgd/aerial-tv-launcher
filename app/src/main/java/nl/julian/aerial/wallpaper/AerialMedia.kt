package nl.julian.aerial.wallpaper

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.net.Socket
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedTrustManager
import java.io.IOException

/** ~5 Apple clips (≈200 MB each). TV storage is tight, so stay modest. */
private const val CACHE_BYTES = 1L * 1024 * 1024 * 1024

/**
 * Network + disk-cache plumbing for aerials. Every read goes through one
 * LRU SimpleCache, so a clip streamed once (or prefetched) replays from disk:
 * instant start and offline playback.
 */
@OptIn(UnstableApi::class)
object AerialMedia {

    @Volatile
    private var cache: SimpleCache? = null

    fun cache(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: SimpleCache(
            File(context.applicationContext.cacheDir, "aerials"),
            LeastRecentlyUsedCacheEvictor(CACHE_BYTES),
            StandaloneDatabaseProvider(context.applicationContext),
        ).also { cache = it }
    }

    /**
     * Apple's sylvan.apple.com serves a chain many TV trust stores reject.
     * Certificate checks are skipped for that one host only (it serves nothing
     * but public decorative video); every other host is verified normally.
     */
    private val http: OkHttpClient by lazy {
        val tm = SylvanTrustManager(platformTrustManager())
        val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(tm), null) }
        OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, tm)
            .build()
    }

    private fun platformTrustManager(): X509ExtendedTrustManager {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        return tmf.trustManagers.filterIsInstance<X509ExtendedTrustManager>().first()
    }

    /**
     * Trusts sylvan.apple.com without validation and defers everything else to
     * the platform. Implements every check variant, because Android's TLS
     * stack picks different ones on different devices and builds.
     */
    private class SylvanTrustManager(private val platform: X509ExtendedTrustManager) : X509ExtendedTrustManager() {
        private fun insecure(host: String?) = host.equals(INSECURE_HOST, ignoreCase = true)

        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket?) {
            val host = (socket as? SSLSocket)?.handshakeSession?.peerHost
            if (!insecure(host)) platform.checkServerTrusted(chain, authType, socket)
        }
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine?) {
            if (!insecure(engine?.peerHost)) platform.checkServerTrusted(chain, authType, engine)
        }
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) =
            platform.checkServerTrusted(chain, authType)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket?) =
            platform.checkClientTrusted(chain, authType, socket)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine?) =
            platform.checkClientTrusted(chain, authType, engine)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
            platform.checkClientTrusted(chain, authType)
        override fun getAcceptedIssuers(): Array<X509Certificate> = platform.acceptedIssuers
    }

    private const val INSECURE_HOST = "sylvan.apple.com"

    fun dataSourceFactory(context: Context): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(cache(context))
            .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(http))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    fun isFullyCached(context: Context, uri: String): Boolean {
        val c = cache(context)
        val length = ContentMetadata.getContentLength(c.getContentMetadata(uri))
        return length > 0 && c.isCached(uri, 0, length)
    }

    /** Downloads [uri] into the cache. Cancelling the calling coroutine stops it. */
    suspend fun prefetch(context: Context, uri: String) = withContext(Dispatchers.IO) {
        if (isFullyCached(context, uri)) return@withContext
        val writer = CacheWriter(
            dataSourceFactory(context).createDataSource(),
            DataSpec(Uri.parse(uri)),
            null,
            null,
        )
        val handle = coroutineContext[Job]?.invokeOnCompletion { writer.cancel() }
        try {
            writer.cache()
        } catch (e: IOException) {
            // Cancelled, offline, or the CDN hiccuped; the clip will just stream.
            Log.d("Aerial", "prefetch stopped: ${e.message}")
        } finally {
            handle?.dispose()
        }
    }
}
