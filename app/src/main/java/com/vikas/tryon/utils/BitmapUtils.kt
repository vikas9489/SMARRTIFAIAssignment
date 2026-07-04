package com.vikas.tryon.utils

import android.graphics.Bitmap

object BitmapUtils {

    private const val ALPHA_THRESHOLD = 24

    /**
     * Crops a bitmap to the bounding box of its visible (non-transparent) pixels.
     * Scanned garment PNGs carry large transparent margins from background removal;
     * without trimming, overlay placement math sizes the empty margins instead of
     * the garment itself.
     */
    fun trimTransparent(source: Bitmap, marginPx: Int = 2): Bitmap {
        val w = source.width
        val h = source.height
        val px = IntArray(w * h)
        source.getPixels(px, 0, w, 0, 0, w, h)

        var minX = w; var minY = h; var maxX = -1; var maxY = -1
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                if ((px[row + x] ushr 24) > ALPHA_THRESHOLD) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        // Fully transparent or already tight — return as-is
        if (maxX < minX || maxY < minY) return source
        val left = (minX - marginPx).coerceAtLeast(0)
        val top = (minY - marginPx).coerceAtLeast(0)
        val right = (maxX + marginPx).coerceAtMost(w - 1)
        val bottom = (maxY + marginPx).coerceAtMost(h - 1)
        if (left == 0 && top == 0 && right == w - 1 && bottom == h - 1) return source

        return Bitmap.createBitmap(source, left, top, right - left + 1, bottom - top + 1)
    }
}
