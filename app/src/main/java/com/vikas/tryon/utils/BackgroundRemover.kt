package com.vikas.tryon.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.sqrt

@Singleton
class BackgroundRemover @Inject constructor() {

    companion object {
        private const val MAX_DIM = 800
        private const val FEATHER_RADIUS = 2
        private const val DILATE_RADIUS = 2
        // Max squared RGB distance a pixel can be from garment palette and still be foreground.
        // Keeps dark-shirt classifiers from pulling in unrelated dark objects.
        private const val MAX_GARMENT_DIST_SQ = 12000f   // ≈ 110 per channel
    }

    suspend fun removeBackground(source: Bitmap): Bitmap = withContext(Dispatchers.Default) {

        // ── 0. Downscale ────────────────────────────────────────────────────
        val scale = min(MAX_DIM.toFloat() / source.width, MAX_DIM.toFloat() / source.height)
            .coerceAtMost(1f)
        val sw = (source.width  * scale).toInt().coerceAtLeast(1)
        val sh = (source.height * scale).toInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(source, sw, sh, true)
        val px = IntArray(sw * sh)
        small.getPixels(px, 0, sw, 0, 0, sw, sh)

        // ── 1. Garment palette — 3×3 grid from centre 40 % ──────────────────
        val cx0 = (sw * 0.30f).toInt(); val cx1 = (sw * 0.70f).toInt()
        val cy0 = (sh * 0.30f).toInt(); val cy1 = (sh * 0.70f).toInt()
        val cw  = ((cx1 - cx0) / 3).coerceAtLeast(1)
        val ch  = ((cy1 - cy0) / 3).coerceAtLeast(1)
        val garmentColors = Array(9) { k ->
            val gx0 = cx0 + (k % 3) * cw; val gx1 = gx0 + cw
            val gy0 = cy0 + (k / 3) * ch; val gy1 = gy0 + ch
            averageColor(px, sw, gx0, gx1, gy0, gy1)
        }

        // ── 2. Background palette — brightest pixels from each corner ────────
        // Using brightness-biased sampling means dark objects in corners
        // (laptop, clothes, phone) don't corrupt the background reference.
        // The actual background (table/bedsheet) is always lighter than a shirt.
        val cs = (min(sw, sh) * 0.20f).toInt().coerceAtLeast(8)
        val bgColors = arrayOf(
            brightestColor(px, sw, 0,       cs,      0,       cs),   // TL
            brightestColor(px, sw, sw - cs, sw,      0,       cs),   // TR
            brightestColor(px, sw, 0,       cs,      sh - cs, sh),   // BL
            brightestColor(px, sw, sw - cs, sw,      sh - cs, sh),   // BR
            // Also sample the mid-edges to catch more background area
            brightestColor(px, sw, 0,       cs,      sh/2-cs/2, sh/2+cs/2),  // Left mid
            brightestColor(px, sw, sw - cs, sw,      sh/2-cs/2, sh/2+cs/2)   // Right mid
        )

        // ── 3. Classify every pixel ──────────────────────────────────────────
        val fg = BooleanArray(sw * sh)
        for (i in px.indices) {
            val dG = minDist(px[i], garmentColors)
            val dB = minDist(px[i], bgColors)
            // Foreground: closer to garment than background (with 10% bias)
            // AND within absolute garment threshold (prevents leaking to
            // unrelated objects of similar hue, e.g. dark laptop near black shirt)
            fg[i] = dG < dB * 0.90f && dG < MAX_GARMENT_DIST_SQ
        }

        // ── 4. Connected component BFS from image centre ────────────────────
        val connected = BooleanArray(sw * sh)
        var seed = (sh / 2) * sw + (sw / 2)
        if (!fg[seed]) {
            outer@ for (r in 1..80) {
                for (dy in -r..r) for (dx in -r..r) {
                    val sx = sw / 2 + dx; val sy = sh / 2 + dy
                    if (sx in 0 until sw && sy in 0 until sh) {
                        val si = sy * sw + sx
                        if (fg[si]) { seed = si; break@outer }
                    }
                }
            }
        }
        connected[seed] = true
        val q = ArrayDeque<Int>()
        q.add(seed)
        while (q.isNotEmpty()) {
            val idx = q.removeFirst()
            val x = idx % sw; val y = idx / sw
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until sw && ny in 0 until sh) {
                    val ni = ny * sw + nx
                    if (fg[ni] && !connected[ni]) { connected[ni] = true; q.add(ni) }
                }
            }
        }

        // ── 5. Hole fill — recover buttons/logos/print inside garment ───────
        val reachableFromEdge = BooleanArray(sw * sh)
        for (x in 0 until sw) {
            enqueueIfTransparent(0,      x, sw, sh, connected, reachableFromEdge, q)
            enqueueIfTransparent(sh - 1, x, sw, sh, connected, reachableFromEdge, q)
        }
        for (y in 0 until sh) {
            enqueueIfTransparent(y, 0,      sw, sh, connected, reachableFromEdge, q)
            enqueueIfTransparent(y, sw - 1, sw, sh, connected, reachableFromEdge, q)
        }
        while (q.isNotEmpty()) {
            val idx = q.removeFirst()
            val x = idx % sw; val y = idx / sw
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until sw && ny in 0 until sh) {
                    val ni = ny * sw + nx
                    if (!connected[ni] && !reachableFromEdge[ni]) {
                        reachableFromEdge[ni] = true; q.add(ni)
                    }
                }
            }
        }
        for (i in connected.indices) {
            if (!connected[i] && !reachableFromEdge[i]) connected[i] = true
        }

        // ── 6. Edge dilation ─────────────────────────────────────────────────
        val dilated = connected.copyOf()
        for (y in DILATE_RADIUS until sh - DILATE_RADIUS) {
            for (x in DILATE_RADIUS until sw - DILATE_RADIUS) {
                if (!connected[y * sw + x]) {
                    check@ for (dy in -DILATE_RADIUS..DILATE_RADIUS)
                        for (dx in -DILATE_RADIUS..DILATE_RADIUS) {
                            if (connected[(y + dy) * sw + (x + dx)]) {
                                dilated[y * sw + x] = true; break@check
                            }
                        }
                }
            }
        }

        // ── 7. Apply mask to full-res source ─────────────────────────────────
        val ow = source.width; val oh = source.height
        val origPx = IntArray(ow * oh)
        source.getPixels(origPx, 0, ow, 0, 0, ow, oh)
        val result = IntArray(ow * oh)
        for (oy in 0 until oh) {
            for (ox in 0 until ow) {
                val sx = (ox * scale).toInt().coerceIn(0, sw - 1)
                val sy = (oy * scale).toInt().coerceIn(0, sh - 1)
                val oi = oy * ow + ox
                if (!dilated[sy * sw + sx]) {
                    result[oi] = 0
                } else {
                    val alpha = featheredAlpha(dilated, sx, sy, sw, sh, FEATHER_RADIUS)
                    result[oi] = (origPx[oi] and 0x00FFFFFF) or (alpha shl 24)
                }
            }
        }
        val out = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
        out.setPixels(result, 0, ow, 0, 0, ow, oh)
        // Trim transparent margins so the bitmap bounds = the garment bounds.
        // Overlay placement sizes the whole bitmap, so margins would make the
        // garment render smaller and lower than intended.
        BitmapUtils.trimTransparent(out)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns the average colour of the BRIGHTEST 30 % of pixels in the region.
     * Ignores dark objects in corners so the true background (table/bedsheet)
     * is sampled even when a laptop or dark clothes sit in the corner.
     */
    private fun brightestColor(px: IntArray, w: Int, x0: Int, x1: Int, y0: Int, y1: Int): Int {
        val xs = x0.coerceAtLeast(0); val xe = x1.coerceAtMost(w)
        val ys = y0.coerceAtLeast(0); val ye = y1.coerceAtMost(px.size / w)
        val samples = mutableListOf<Int>()
        for (y in ys until ye) for (x in xs until xe) {
            val idx = y * w + x
            if (idx < px.size) samples.add(px[idx])
        }
        if (samples.isEmpty()) return Color.LTGRAY
        // Sort by brightness descending, keep top 30 %
        val sorted = samples.sortedByDescending {
            Color.red(it) + Color.green(it) + Color.blue(it)
        }
        val topK = sorted.take((sorted.size * 0.30f).toInt().coerceAtLeast(1))
        var r = 0L; var g = 0L; var b = 0L
        for (c in topK) { r += Color.red(c); g += Color.green(c); b += Color.blue(c) }
        return Color.rgb((r / topK.size).toInt(), (g / topK.size).toInt(), (b / topK.size).toInt())
    }

    private fun averageColor(px: IntArray, w: Int, x0: Int, x1: Int, y0: Int, y1: Int): Int {
        var r = 0L; var g = 0L; var b = 0L; var n = 0
        val xs = x0.coerceAtLeast(0); val xe = x1.coerceAtMost(w)
        val ys = y0.coerceAtLeast(0); val ye = y1.coerceAtMost(px.size / w)
        for (y in ys until ye) for (x in xs until xe) {
            val idx = y * w + x
            if (idx < px.size) {
                r += Color.red(px[idx]); g += Color.green(px[idx]); b += Color.blue(px[idx]); n++
            }
        }
        if (n == 0) return Color.GRAY
        return Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }

    private fun minDist(pixel: Int, palette: Array<Int>): Float {
        val pr = Color.red(pixel); val pg = Color.green(pixel); val pb = Color.blue(pixel)
        var min = Float.MAX_VALUE
        for (c in palette) {
            val dr = (pr - Color.red(c)).toFloat()
            val dg = (pg - Color.green(c)).toFloat()
            val db = (pb - Color.blue(c)).toFloat()
            val d = dr * dr + dg * dg + db * db
            if (d < min) min = d
        }
        return min
    }

    private fun enqueueIfTransparent(
        y: Int, x: Int, sw: Int, sh: Int,
        connected: BooleanArray, visited: BooleanArray, q: ArrayDeque<Int>
    ) {
        if (x < 0 || x >= sw || y < 0 || y >= sh) return
        val i = y * sw + x
        if (!connected[i] && !visited[i]) { visited[i] = true; q.add(i) }
    }

    private fun featheredAlpha(mask: BooleanArray, x: Int, y: Int, w: Int, h: Int, r: Int): Int {
        var minD = r.toFloat()
        for (dy in -r..r) for (dx in -r..r) {
            val nx = x + dx; val ny = y + dy
            if (nx in 0 until w && ny in 0 until h && !mask[ny * w + nx]) {
                val d = sqrt((dx * dx + dy * dy).toFloat())
                if (d < minD) minD = d
            }
        }
        return ((minD / r) * 255).toInt().coerceIn(0, 255)
    }
}
