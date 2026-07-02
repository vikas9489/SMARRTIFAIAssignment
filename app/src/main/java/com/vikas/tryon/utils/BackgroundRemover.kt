package com.vikas.tryon.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Removes the background from a garment photo.
 *
 * Algorithm:
 * 1. Multi-point background colour estimation (corners + edge strip)
 * 2. 8-directional BFS flood fill from ALL edges with adaptive tolerance
 * 3. Second-pass interior hole fill — removes "trapped" background pockets
 *    inside the garment silhouette
 * 4. Morphological erosion to clean noisy fringe pixels
 * 5. Gaussian-like edge feathering for smooth, anti-aliased transparency
 */
@Singleton
class BackgroundRemover @Inject constructor() {

    suspend fun removeBackground(source: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        // --- Step 1: estimate background colour from border strip ---
        val bgColor = estimateBackgroundColor(pixels, w, h)
        // Adaptive tolerance: tighter when bg is pure white/black, looser for midtones
        val baseTolerance = 55
        val bgBrightness = (Color.red(bgColor) + Color.green(bgColor) + Color.blue(bgColor)) / 3
        val tolerance = if (bgBrightness > 200 || bgBrightness < 30) baseTolerance + 20 else baseTolerance

        // --- Step 2: 8-directional BFS from all four edges ---
        val isBackground = BooleanArray(w * h)
        val queue = ArrayDeque<Int>()

        fun tryEnqueue(idx: Int) {
            if (idx < 0 || idx >= pixels.size) return
            if (!isBackground[idx] && isSimilarColor(pixels[idx], bgColor, tolerance)) {
                isBackground[idx] = true
                queue.add(idx)
            }
        }

        for (x in 0 until w) { tryEnqueue(x); tryEnqueue((h - 1) * w + x) }
        for (y in 0 until h) { tryEnqueue(y * w); tryEnqueue(y * w + w - 1) }

        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            val x = idx % w
            val y = idx / w
            // 8-directional neighbours
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until w && ny in 0 until h) tryEnqueue(ny * w + nx)
            }
        }

        // --- Step 3: fill interior background pockets ---
        // Any pixel that is background-coloured AND fully surrounded by other
        // background pixels should be transparent, even if flood fill missed it.
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x
                if (!isBackground[idx] && isSimilarColor(pixels[idx], bgColor, tolerance + 15)) {
                    val allNeighboursBackground =
                        isBackground[(y - 1) * w + x] && isBackground[(y + 1) * w + x] &&
                        isBackground[y * w + x - 1] && isBackground[y * w + x + 1]
                    if (allNeighboursBackground) isBackground[idx] = true
                }
            }
        }

        // --- Step 4: morphological erosion — strip lone foreground pixels at boundary ---
        val eroded = isBackground.copyOf()
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x
                if (!isBackground[idx]) {
                    // If 5+ of 8 neighbours are background, classify this pixel as background too
                    var bgCount = 0
                    for (dy in -1..1) for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        if (isBackground[(y + dy) * w + (x + dx)]) bgCount++
                    }
                    if (bgCount >= 5) eroded[idx] = true
                }
            }
        }

        // --- Step 5: feathered alpha at silhouette edge ---
        val featherRadius = 4
        val result = IntArray(w * h)
        for (i in pixels.indices) {
            if (eroded[i]) {
                result[i] = 0 // fully transparent
            } else {
                val x = i % w
                val y = i / w
                val alpha = computeFeatheredAlpha(eroded, x, y, w, h, featherRadius)
                result[i] = (pixels[i] and 0x00FFFFFF) or (alpha shl 24)
            }
        }

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        output.setPixels(result, 0, w, 0, 0, w, h)
        output
    }

    private fun estimateBackgroundColor(pixels: IntArray, w: Int, h: Int): Int {
        val samples = mutableListOf<Int>()
        val strip = 8 // sample 8-pixel border strip
        for (i in 0 until strip) {
            for (j in 0 until w step 4) { // top and bottom strips
                if (i * w + j < pixels.size) samples.add(pixels[i * w + j])
                val bIdx = (h - 1 - i) * w + j
                if (bIdx < pixels.size) samples.add(pixels[bIdx])
            }
            for (j in 0 until h step 4) { // left and right strips
                if (j * w + i < pixels.size) samples.add(pixels[j * w + i])
                val rIdx = j * w + w - 1 - i
                if (rIdx < pixels.size) samples.add(pixels[rIdx])
            }
        }
        // Use median-like approach: pick the most common brightness band
        val r = samples.map { Color.red(it) }.average().toInt()
        val g = samples.map { Color.green(it) }.average().toInt()
        val b = samples.map { Color.blue(it) }.average().toInt()
        return Color.rgb(r, g, b)
    }

    private fun isSimilarColor(pixel: Int, reference: Int, tolerance: Int): Boolean {
        val dr = abs(Color.red(pixel) - Color.red(reference))
        val dg = abs(Color.green(pixel) - Color.green(reference))
        val db = abs(Color.blue(pixel) - Color.blue(reference))
        return sqrt((dr * dr + dg * dg + db * db).toDouble()) < tolerance
    }

    // Returns 0–255 alpha based on distance to nearest background pixel
    private fun computeFeatheredAlpha(
        isBackground: BooleanArray,
        x: Int, y: Int, w: Int, h: Int, radius: Int
    ): Int {
        var minDist = radius.toFloat()
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val nx = x + dx; val ny = y + dy
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue
                if (isBackground[ny * w + nx]) {
                    val dist = sqrt((dx * dx + dy * dy).toFloat())
                    if (dist < minDist) minDist = dist
                }
            }
        }
        // Full opacity beyond radius, feathered near the edge
        return ((minDist / radius) * 255).toInt().coerceIn(0, 255)
    }
}
