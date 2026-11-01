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
    private const val FROST_W = 160
    private const val FROST_H = 90
    private val main = Handler(Looper.getMainLooper())

    /**
     * A blurred copy of the picture for frosted glass, or null. Blurred about as
     * much as a [detail]-pixel-wide copy would look stretched across the screen,
     * but smooth: a tiny copy stretched that far shows its pixels as blocks.
     */
    suspend fun frost(view: SurfaceView, detail: Int): Bitmap? {
        if (!view.holder.surface.isValid) return null
        val bmp = Bitmap.createBitmap(FROST_W, FROST_H, Bitmap.Config.ARGB_8888)
        val ok = suspendCancellableCoroutine { cont ->
            runCatching {
                PixelCopy.request(view, bmp, { result -> cont.resume(result == PixelCopy.SUCCESS) }, main)
            }.onFailure { cont.resume(false) }
        }
        if (!ok) return null.also { bmp.recycle() }
        return withContext(Dispatchers.Default) {
            val pixels = IntArray(FROST_W * FROST_H)
            bmp.getPixels(pixels, 0, FROST_W, 0, 0, FROST_W, FROST_H)
            blur(pixels, FROST_W, FROST_H, radius = 2 * FROST_W / detail)
            bmp.setPixels(pixels, 0, FROST_W, 0, 0, FROST_W, FROST_H)
            bmp
        }
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

    /** Three box blurs in each direction: close to a Gaussian, and cheap at this size. */
    private fun blur(px: IntArray, w: Int, h: Int, radius: Int) {
        val tmp = IntArray(px.size)
        repeat(3) {
            for (y in 0 until h) blurLine(px, tmp, y * w, 1, w, radius)
            for (x in 0 until w) blurLine(tmp, px, x, w, h, radius)
        }
    }

    /** Running-sum box blur of one row or column ([n] pixels, [step] apart); edges clamp. The video is opaque. */
    private fun blurLine(src: IntArray, dst: IntArray, start: Int, step: Int, n: Int, r: Int) {
        val window = 2 * r + 1
        fun at(i: Int) = src[start + step * i.coerceIn(0, n - 1)]
        var red = 0
        var green = 0
        var blue = 0
        for (i in -r..r) {
            val c = at(i)
            red += c shr 16 and 0xFF; green += c shr 8 and 0xFF; blue += c and 0xFF
        }
        for (i in 0 until n) {
            dst[start + step * i] = (0xFF shl 24) or (red / window shl 16) or (green / window shl 8) or (blue / window)
            val out = at(i - r)
            val inn = at(i + r + 1)
            red += (inn shr 16 and 0xFF) - (out shr 16 and 0xFF)
            green += (inn shr 8 and 0xFF) - (out shr 8 and 0xFF)
            blue += (inn and 0xFF) - (out and 0xFF)
        }
    }
}
