package com.vikas.tryon.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Removes the background from a garment photo.
 *
 * Strategy — "inside-out" (centre → edges):
 * 1. Sample the garment colour from the centre 20% of the image
 * 2. Sample background colour from the outer edge strip
 * 3. Classify every pixel: closer to garment or to background?
 * 4. Keep only the connected component touching the image centre
 *    → eliminates detached background islands inside the silhouette
 * 5. Feathered alpha at the silhouette boundary
 *
 * Works on complex / patterned backgrounds because it identifies
 * the garment positively from the centre rather than trying to
 * subtract every background colour variant from the edges.
 */
@Singleton
class BackgroundRemover @Inject constructor() {

    suspend fun removeBackground(source: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        // ------------------------------------------------------------------
        // 1. Build garment colour palette from centre 20% of image
        // ------------------------------------------------------------------
        val garmentSamples = sampleRegion(
            pixels, w, h,
            xFrom = (w * 0.40f).toInt(), xTo = (w * 0.60f).toInt(),
            yFrom = (h * 0.40f).toInt(), yTo = (h * 0.60f).toInt(),
            stride = 4
        )

        // ------------------------------------------------------------------
        // 2. Build background colour palette from outer 12% border strip
        // ------------------------------------------------------------------
        val edgeStrip = (min(w, h) * 0.12f).toInt().coerceAtLeast(8)
        val bgSamples = sampleBorderStrip(pixels, w, h, edgeStrip, stride = 6)

        // ------------------------------------------------------------------
        // 3. Classify every pixel: foreground (closer to garment) vs background
        // ------------------------------------------------------------------
        val isForeground = BooleanArray(w * h)
        for (i in pixels.indices) {
            val pix = pixels[i]
            val dGarment = minColorDist(pix, garmentSamples)
            val dBg = minColorDist(pix, bgSamples)
            // Give garment a slight bias so border-ambiguous pixels stay in
            isForeground[i] = dGarment * 1.10f < dBg
        }

        // ------------------------------------------------------------------
        // 4. Connected-component flood fill from the image centre
        //    → only the garment body survives; detached patches are removed
        // ------------------------------------------------------------------
        val centerX = w / 2
        val centerY = h / 2
        val connected = BooleanArray(w * h)

        // If the absolute centre is classified as background (very dark / very
        // similar to bg), try a small spiral outward to find a foreground seed.
        var seedIdx = centerY * w + centerX
        if (!isForeground[seedIdx]) {
            outer@ for (r in 1..30) {
                for (dy in -r..r) for (dx in -r..r) {
                    val sx = centerX + dx; val sy = centerY + dy
                    if (sx in 0 until w && sy in 0 until h) {
                        val si = sy * w + sx
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
            val x = idx % w
            val y = idx / w
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until w && ny in 0 until h) {
                    val ni = ny * w + nx
                    if (isForeground[ni] && !connected[ni]) {
                        connected[ni] = true
                        queue.add(ni)
                    }
                }
            }
        }

        // ------------------------------------------------------------------
        // 5. Feathered alpha at silhouette boundary
        // ------------------------------------------------------------------
        val featherRadius = 5
        val result = IntArray(w * h)
        for (i in pixels.indices) {
            if (!connected[i]) {
                result[i] = 0 // fully transparent
            } else {
                val x = i % w
                val y = i / w
                val alpha = featheredAlpha(connected, x, y, w, h, featherRadius)
                result[i] = (pixels[i] and 0x00FFFFFF) or (alpha shl 24)
            }
        }

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        output.setPixels(result, 0, w, 0, 0, w, h)
        output
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun sampleRegion(
        pixels: IntArray, w: Int, h: Int,
        xFrom: Int, xTo: Int, yFrom: Int, yTo: Int,
        stride: Int
    ): List<Int> {
        val samples = mutableListOf<Int>()
        for (y in yFrom until yTo step stride)
            for (x in xFrom until xTo step stride)
                samples.add(pixels[y * w + x])
        return samples
    }

    private fun sampleBorderStrip(
        pixels: IntArray, w: Int, h: Int, strip: Int, stride: Int
    ): List<Int> {
        val samples = mutableListOf<Int>()
        for (y in 0 until h step stride) {
            for (x in 0 until strip) samples.add(pixels[y * w + x])
            for (x in (w - strip) until w) samples.add(pixels[y * w + x])
        }
        for (x in 0 until w step stride) {
            for (y in 0 until strip) samples.add(pixels[y * w + x])
            for (y in (h - strip) until h) samples.add(pixels[y * w + x])
        }
        return samples
    }

    /** Minimum Euclidean distance (RGB) from [pixel] to any colour in [palette]. */
    private fun minColorDist(pixel: Int, palette: List<Int>): Float {
        if (palette.isEmpty()) return Float.MAX_VALUE
        val pr = Color.red(pixel); val pg = Color.green(pixel); val pb = Color.blue(pixel)
        var minD = Float.MAX_VALUE
        for (c in palette) {
            val dr = (pr - Color.red(c)).toFloat()
            val dg = (pg - Color.green(c)).toFloat()
            val db = (pb - Color.blue(c)).toFloat()
            val d = dr * dr + dg * dg + db * db  // squared — no sqrt needed for comparison
            if (d < minD) minD = d
        }
        return minD
    }

    /** Alpha that fades from 0 near the boundary to 255 fully inside. */
    private fun featheredAlpha(
        connected: BooleanArray,
        x: Int, y: Int, w: Int, h: Int, radius: Int
    ): Int {
        var minDist = radius.toFloat()
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until w && ny in 0 until h && !connected[ny * w + nx]) {
                    val d = sqrt((dx * dx + dy * dy).toFloat())
                    if (d < minDist) minDist = d
                }
            }
        }
        return ((minDist / radius) * 255).toInt().coerceIn(0, 255)
    }
}
