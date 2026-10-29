package nl.julian.aerial.wallpaper

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Grabs a tiny frame of the playing aerial and returns its most "theme-worthy"
 * colour (Material's quantize + score), or null if the copy failed.
 */
object FrameSampler {
    private const val W = 112
    private const val H = 63
    private val main = Handler(Looper.getMainLooper())

    /** A [w]×[h] copy of the picture, for frosted glass (stretched, it's already a blur), or null. */
    suspend fun frame(view: SurfaceView, w: Int, h: Int): Bitmap? {
        if (!view.holder.surface.isValid) return null
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val ok = suspendCancellableCoroutine { cont ->
            runCatching {
                PixelCopy.request(view, bmp, { result -> cont.resume(result == PixelCopy.SUCCESS) }, main)
            }.onFailure { cont.resume(false) }
        }
        return if (ok) bmp else null.also { bmp.recycle() }
    }

    suspend fun seed(view: SurfaceView): Int? {
        if (!view.holder.surface.isValid) return null
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val ok = suspendCancellableCoroutine { cont ->
            runCatching {
                PixelCopy.request(view, bmp, { result -> cont.resume(result == PixelCopy.SUCCESS) }, main)
            }.onFailure { cont.resume(false) }
        }
        if (!ok) return null.also { bmp.recycle() }
        return withContext(Dispatchers.Default) {
            val pixels = IntArray(W * H)
            bmp.getPixels(pixels, 0, W, 0, 0, W, H)
            bmp.recycle()
            Score.score(QuantizerCelebi.quantize(pixels, 64)).firstOrNull()
        }
    }
}
