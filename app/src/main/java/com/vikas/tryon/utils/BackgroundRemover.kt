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
 * Removes the background from a garment photo using:
 * 1. Corner-sampling to estimate background color
 * 2. Tolerance-based flood fill from all edges
 * 3. Edge-smoothing (anti-alias the transparency boundary)
 */
@Singleton
class BackgroundRemover @Inject constructor() {

    suspend fun removeBackground(source: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val w = source.width
        val h = source.height

        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        // Sample average background color from corners and edges
        val bgColor = estimateBackgroundColor(pixels, w, h)
        val tolerance = 60

        // Mark background pixels via flood fill from all four edges
        val isBackground = BooleanArray(w * h)
        val queue = ArrayDeque<Int>()

        fun enqueue(idx: Int) {
            if (!isBackground[idx] && isSimilarColor(pixels[idx], bgColor, tolerance)) {
                isBackground[idx] = true
                queue.add(idx)
            }
        }

        // Seed from edges
        for (x in 0 until w) { enqueue(x); enqueue((h - 1) * w + x) }
        for (y in 0 until h) { enqueue(y * w); enqueue(y * w + w - 1) }

        // BFS flood fill
        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            val x = idx % w
            val y = idx / w
            if (x > 0)     enqueue(idx - 1)
            if (x < w - 1) enqueue(idx + 1)
            if (y > 0)     enqueue(idx - w)
            if (y < h - 1) enqueue(idx + w)
        }

        // Apply transparency with soft edge smoothing
        val result = IntArray(w * h)
        for (i in pixels.indices) {
            if (isBackground[i]) {
                result[i] = 0 // fully transparent
            } else {
                val x = i % w
                val y = i / w
                val edgeDist = computeEdgeDistance(isBackground, x, y, w, h, radius = 3)
                val alpha = (edgeDist * 255).toInt().coerceIn(0, 255)
                result[i] = (pixels[i] and 0x00FFFFFF) or (alpha shl 24)
            }
        }

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        output.setPixels(result, 0, w, 0, 0, w, h)
        output
    }

    private fun estimateBackgroundColor(pixels: IntArray, w: Int, h: Int): Int {
        val samples = mutableListOf<Int>()
        val sampleSize = 5
        // Sample corners and edges
        for (i in 0 until sampleSize) {
            samples.add(pixels[i])                        // top-left
            samples.add(pixels[w - 1 - i])               // top-right
            samples.add(pixels[(h - 1) * w + i])         // bottom-left
            samples.add(pixels[(h - 1) * w + w - 1 - i]) // bottom-right
            samples.add(pixels[i * w])                    // left edge
            samples.add(pixels[i * w + w - 1])            // right edge
        }
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

    private fun computeEdgeDistance(
        isBackground: BooleanArray,
        x: Int, y: Int, w: Int, h: Int, radius: Int
    ): Float {
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
        return (minDist / radius).coerceIn(0f, 1f)
    }
}
