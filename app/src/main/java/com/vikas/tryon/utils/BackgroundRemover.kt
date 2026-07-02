package com.vikas.tryon.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Removes the background from a garment photo.
 *
 * Performance strategy:
 *   - Downscale to MAX_DIM before all heavy computation
 *   - Upscale the boolean mask back to original resolution
 *   - Apply feathered alpha on original-resolution bitmap
 *
 * Detection strategy ("inside-out"):
 *   1. Average garment colour from centre 20 % of the working image
 *   2. Average background colour from outer 12 % border strip
 *   3. Classify each pixel by which average it is closer to
 *   4. Flood-fill from centre keeping only the connected foreground blob
 *   5. Feather the silhouette boundary
 */
@Singleton
class BackgroundRemover @Inject constructor() {

    companion object {
        private const val MAX_DIM = 600   // working-copy max dimension (fast BFS)
        private const val FEATHER_RADIUS = 4
    }

    suspend fun removeBackground(source: Bitmap): Bitmap = withContext(Dispatchers.Default) {

        // ------------------------------------------------------------------
        // 0. Downscale to a fast working copy
        // ------------------------------------------------------------------
        val scale = min(MAX_DIM.toFloat() / source.width, MAX_DIM.toFloat() / source.height)
            .coerceAtMost(1f)
        val wSmall = (source.width  * scale).toInt().coerceAtLeast(1)
        val hSmall = (source.height * scale).toInt().coerceAtLeast(1)

        val small = Bitmap.createScaledBitmap(source, wSmall, hSmall, true)
        val pixels = IntArray(wSmall * hSmall)
        small.getPixels(pixels, 0, wSmall, 0, 0, wSmall, hSmall)

        // ------------------------------------------------------------------
        // 1. Average garment colour from centre 20 % region
        // ------------------------------------------------------------------
        val garmentColor = averageColor(
            pixels, wSmall, hSmall,
            xFrom = (wSmall * 0.40f).toInt(), xTo = (wSmall * 0.60f).toInt(),
            yFrom = (hSmall * 0.40f).toInt(), yTo = (hSmall * 0.60f).toInt()
        )

        // ------------------------------------------------------------------
        // 2. Average background colour from outer 12 % border strip
        // ------------------------------------------------------------------
        val strip = (min(wSmall, hSmall) * 0.12f).toInt().coerceAtLeast(4)
        val bgColor = averageBorderColor(pixels, wSmall, hSmall, strip)

        // ------------------------------------------------------------------
        // 3. Classify every pixel: closer to garment or to background?
        // ------------------------------------------------------------------
        val isForeground = BooleanArray(wSmall * hSmall)
        for (i in pixels.indices) {
            val dG = colorDist(pixels[i], garmentColor)
            val dB = colorDist(pixels[i], bgColor)
            isForeground[i] = dG * 1.1f < dB   // slight bias toward keeping garment
        }

        // ------------------------------------------------------------------
        // 4. Connected-component from centre (removes background islands)
        // ------------------------------------------------------------------
        val connected = BooleanArray(wSmall * hSmall)
        var seedIdx = (hSmall / 2) * wSmall + (wSmall / 2)

        // If dead-centre is classified background, spiral outward for a seed
        if (!isForeground[seedIdx]) {
            outer@ for (r in 1..40) {
                for (dy in -r..r) for (dx in -r..r) {
                    val sx = wSmall / 2 + dx
                    val sy = hSmall / 2 + dy
                    if (sx in 0 until wSmall && sy in 0 until hSmall) {
                        val si = sy * wSmall + sx
                        if (isForeground[si]) { seedIdx = si; break@outer }
                    }
                }
            }
        }

        connected[seedIdx] = true
        val queue = ArrayDeque<Int>()
        queue.add(seedIdx)

        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            val x = idx % wSmall
            val y = idx / wSmall
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until wSmall && ny in 0 until hSmall) {
                    val ni = ny * wSmall + nx
                    if (isForeground[ni] && !connected[ni]) {
                        connected[ni] = true
                        queue.add(ni)
                    }
                }
            }
        }

        // ------------------------------------------------------------------
        // 5. Upscale connected mask to original resolution and apply alpha
        // ------------------------------------------------------------------
        val origW = source.width
        val origH = source.height
        val origPixels = IntArray(origW * origH)
        source.getPixels(origPixels, 0, origW, 0, 0, origW, origH)

        val result = IntArray(origW * origH)
        for (oy in 0 until origH) {
            for (ox in 0 until origW) {
                // Map original pixel to small-image pixel
                val sx = (ox * scale).toInt().coerceIn(0, wSmall - 1)
                val sy = (oy * scale).toInt().coerceIn(0, hSmall - 1)
                val si = sy * wSmall + sx

                val oi = oy * origW + ox
                if (!connected[si]) {
                    result[oi] = 0 // transparent
                } else {
                    // Feather based on small-image neighbourhood
                    val alpha = featheredAlpha(connected, sx, sy, wSmall, hSmall, FEATHER_RADIUS)
                    result[oi] = (origPixels[oi] and 0x00FFFFFF) or (alpha shl 24)
                }
            }
        }

        val output = Bitmap.createBitmap(origW, origH, Bitmap.Config.ARGB_8888)
        output.setPixels(result, 0, origW, 0, 0, origW, origH)
        output
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun averageColor(
        pixels: IntArray, w: Int, h: Int,
        xFrom: Int, xTo: Int, yFrom: Int, yTo: Int
    ): Int {
        var r = 0L; var g = 0L; var b = 0L; var count = 0
        for (y in yFrom until yTo) for (x in xFrom until xTo) {
            val p = pixels[y * w + x]
            r += Color.red(p); g += Color.green(p); b += Color.blue(p)
            count++
        }
        if (count == 0) return Color.GRAY
        return Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
    }

    private fun averageBorderColor(pixels: IntArray, w: Int, h: Int, strip: Int): Int {
        var r = 0L; var g = 0L; var b = 0L; var count = 0
        fun add(p: Int) { r += Color.red(p); g += Color.green(p); b += Color.blue(p); count++ }
        for (y in 0 until h) {
            for (x in 0 until strip) add(pixels[y * w + x])
            for (x in (w - strip) until w) add(pixels[y * w + x])
        }
        for (x in 0 until w) {
            for (y in 0 until strip) add(pixels[y * w + x])
            for (y in (h - strip) until h) add(pixels[y * w + x])
        }
        if (count == 0) return Color.WHITE
        return Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
    }

    private fun colorDist(a: Int, b: Int): Float {
        val dr = (Color.red(a)   - Color.red(b)).toFloat()
        val dg = (Color.green(a) - Color.green(b)).toFloat()
        val db = (Color.blue(a)  - Color.blue(b)).toFloat()
        return sqrt(dr * dr + dg * dg + db * db)
    }

    private fun featheredAlpha(
        connected: BooleanArray,
        x: Int, y: Int, w: Int, h: Int, radius: Int
    ): Int {
        var minDist = radius.toFloat()
        for (dy in -radius..radius) for (dx in -radius..radius) {
            val nx = x + dx; val ny = y + dy
            if (nx in 0 until w && ny in 0 until h && !connected[ny * w + nx]) {
                val d = sqrt((dx * dx + dy * dy).toFloat())
                if (d < minDist) minDist = d
            }
        }
        return ((minDist / radius) * 255).toInt().coerceIn(0, 255)
    }
}
