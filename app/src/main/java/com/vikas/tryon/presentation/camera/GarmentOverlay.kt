package com.vikas.tryon.presentation.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.vikas.tryon.data.model.Garment
import com.vikas.tryon.data.model.GarmentCategory
import com.vikas.tryon.utils.SmoothedLandmarks

@Composable
fun GarmentOverlay(
    landmarks: SmoothedLandmarks?,
    garment: Garment?,
    garmentBitmap: Bitmap? = null,
    isFrontCamera: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Cache ImageBitmap conversion — never call asImageBitmap() inside DrawScope
    val imageBitmap = remember(garmentBitmap) { garmentBitmap?.asImageBitmap() }

    Canvas(modifier = modifier) {
        if (landmarks == null || garment == null || landmarks.size <= 28) return@Canvas

        val w = size.width
        val h = size.height

        val lsVis = landmarks.getVisibility(11)
        val rsVis = landmarks.getVisibility(12)
        if (lsVis < 0.2f || rsVis < 0.2f) return@Canvas

        val avgConfidence = ((lsVis + rsVis +
            landmarks.getVisibility(23) +
            landmarks.getVisibility(24)) / 4f).coerceIn(0f, 1f)
        val garmentAlpha = (avgConfidence * 1.0f).coerceIn(0.6f, 0.92f)

        // Mirror X coords for front camera: the PreviewView is horizontally flipped
        // but ImageAnalysis gives the raw sensor image, so landmarks need mirroring.
        val getX = { idx: Int ->
            val raw = landmarks.getX(idx) * w
            if (isFrontCamera) w - raw else raw
        }
        val getY = { idx: Int -> landmarks.getY(idx) * h }

        val lsX = getX(11);  val lsY = getY(11)
        val rsX = getX(12);  val rsY = getY(12)
        val lhX = getX(23);  val lhY = getY(23)
        val rhX = getX(24);  val rhY = getY(24)
        val leX = getX(13);  val leY = getY(13)
        val reX = getX(14);  val reY = getY(14)
        val laX = getX(27);  val laY = getY(27)
        val raX = getX(28);  val raY = getY(28)

        // Use pre-rendered bitmap (from drawable or scanned) if available
        if (imageBitmap != null) {
            drawScannedGarment(
                image = imageBitmap,
                lsX = lsX, lsY = lsY, rsX = rsX, rsY = rsY,
                lhX = lhX, lhY = lhY, rhX = rhX, rhY = rhY,
                leX = leX, reX = reX,
                alpha = garmentAlpha,
                category = garment.category,
                screenW = w, screenH = h,
                lhVis = landmarks.getVisibility(23),
                rhVis = landmarks.getVisibility(24)
            )
        } else {
            when (garment.category) {
                GarmentCategory.TOP, GarmentCategory.OUTERWEAR ->
                    drawTopWithEffects(
                        garment.color, lsX, lsY, rsX, rsY,
                        lhX, lhY, rhX, rhY, leX, leY, reX, reY,
                        garmentAlpha, garment.category
                    )
                GarmentCategory.BOTTOM ->
                    drawBottomWithEffects(
                        garment.color, lhX, lhY, rhX, rhY,
                        laX, laY, raX, raY, garmentAlpha
                    )
                GarmentCategory.DRESS, GarmentCategory.SCANNED ->
                    drawDressWithEffects(
                        garment.color, lsX, lsY, rsX, rsY,
                        lhX, lhY, rhX, rhY, laX, laY, raX, raY, garmentAlpha
                    )
            }
        }
    }
}

private fun DrawScope.drawScannedGarment(
    image: ImageBitmap,
    lsX: Float, lsY: Float, rsX: Float, rsY: Float,
    lhX: Float, lhY: Float, rhX: Float, rhY: Float,
    leX: Float, reX: Float,
    alpha: Float,
    category: GarmentCategory,
    screenW: Float, screenH: Float,
    lhVis: Float, rhVis: Float
) {
    // ── 1. Key measurements from landmarks ───────────────────────────────
    val shoulderSpread = kotlin.math.abs(rsX - lsX).coerceAtLeast(1f)
    val shoulderTopY   = minOf(lsY, rsY)
    val shoulderCx     = (lsX + rsX) / 2f

    val hipsVisible = lhVis > 0.35f && rhVis > 0.35f
    val hipMidY     = if (hipsVisible) (lhY + rhY) / 2f
                      else shoulderTopY + shoulderSpread * 2.6f
    val torsoH      = (hipMidY - shoulderTopY).coerceAtLeast(shoulderSpread)

    // ── 2. Width — shoulder outer edges + sleeve overhang ────────────────
    // The bitmap is trimmed to the garment's visible pixels, so bitmap width
    // maps directly to the shirt's flat-lay width (sleeve tip to sleeve tip).
    // Shoulder JOINTS are inner points; a tee's full width ≈ 2.1× that spread.
    val garmentW = shoulderSpread * 2.1f
    val centerX  = shoulderCx  // center on shoulders — most reliable mid-X

    // ── 3. Height — scale both axes together to maintain shape ───────────
    // Start from the bitmap's natural aspect ratio at the computed width.
    // If the natural height is shorter than the torso, scale the WHOLE
    // garment up (width and height) so it covers shoulder-to-hip.
    val imgAspect  = image.height.toFloat() / image.width.toFloat().coerceAtLeast(1f)
    val naturalH   = garmentW * imgAspect
    // We want garment to cover 105% of torso height
    val targetH    = torsoH * 1.05f
    val scaleUp    = if (naturalH < targetH) targetH / naturalH else 1f
    val finalW     = garmentW * scaleUp
    val finalH     = naturalH * scaleUp

    // ── 4. Position ───────────────────────────────────────────────────────
    // With a trimmed bitmap the top row IS the collar. The shirt's shoulder
    // seam sits slightly above the shoulder JOINT landmark, so lift the
    // garment up by 30% of shoulder spread to align collar with neck base.
    val collarY = shoulderTopY - shoulderSpread * 0.60f
    val top    = collarY.toInt().coerceAtLeast(0)
    val left   = (centerX - finalW / 2f).toInt().coerceAtLeast(0)
    val right  = (centerX + finalW / 2f).toInt().coerceAtMost(screenW.toInt())
    val bottom = (top + finalH).toInt().coerceAtMost(screenH.toInt())

    val dstW = (right - left).coerceAtLeast(1)
    val dstH = (bottom - top).coerceAtLeast(1)

    drawImage(
        image = image,
        dstOffset = IntOffset(left, top),
        dstSize = IntSize(dstW, dstH),
        alpha = alpha,
        filterQuality = FilterQuality.Medium
    )
}

private fun DrawScope.drawTopWithEffects(
    color: Color,
    lsX: Float, lsY: Float, rsX: Float, rsY: Float,
    lhX: Float, lhY: Float, rhX: Float, rhY: Float,
    leX: Float, leY: Float, reX: Float, reY: Float,
    alpha: Float, category: GarmentCategory
) {
    val padding = (rsX - lsX) * 0.08f
    val sleeveExtra = if (category == GarmentCategory.OUTERWEAR) 1.35f else 1.05f
    val neckWidth = (rsX - lsX) * 0.25f
    val neckCenterX = (lsX + rsX) / 2f
    val shoulderY = (lsY + rsY) / 2f

    val path = Path().apply {
        moveTo(lsX - padding, lsY)
        cubicTo(
            lsX - padding * 3f * sleeveExtra, lsY + (leY - lsY) * 0.3f,
            leX - padding * 2f, leY,
            lsX - padding * 2f, lsY + (lhY - lsY) * 0.45f
        )
        lineTo(lhX - padding, lhY)
        quadraticTo((lhX + rhX) / 2f, lhY + padding * 0.5f, rhX + padding, rhY)
        lineTo(rsX + padding * 2f, rsY + (rhY - rsY) * 0.45f)
        cubicTo(
            reX + padding * 2f, reY,
            rsX + padding * 3f * sleeveExtra, rsY + (reY - rsY) * 0.3f,
            rsX + padding, rsY
        )
        lineTo(neckCenterX + neckWidth, shoulderY - (lhY - lsY) * 0.05f)
        quadraticTo(
            neckCenterX, shoulderY - (lhY - lsY) * 0.14f,
            neckCenterX - neckWidth, shoulderY - (lhY - lsY) * 0.05f
        )
        close()
    }
    drawGarmentWithEffects(path, color, alpha)
}

private fun DrawScope.drawBottomWithEffects(
    color: Color,
    lhX: Float, lhY: Float, rhX: Float, rhY: Float,
    laX: Float, laY: Float, raX: Float, raY: Float,
    alpha: Float
) {
    val padding = (rhX - lhX) * 0.1f
    val crotchX = (lhX + rhX) / 2f
    val crotchY = (lhY + rhY) / 2f + (laY - lhY) * 0.3f

    val path = Path().apply {
        moveTo(lhX - padding, lhY)
        lineTo(rhX + padding, rhY)
        cubicTo(rhX + padding, rhY + (raY - rhY) * 0.3f, raX + padding * 0.5f, raY - (raY - rhY) * 0.2f, raX + padding * 0.5f, raY)
        lineTo(crotchX, crotchY)
        cubicTo(laX - padding * 0.5f, laY - (laY - lhY) * 0.2f, lhX - padding, lhY + (laY - lhY) * 0.3f, laX - padding * 0.5f, laY)
        close()
    }
    drawGarmentWithEffects(path, color, alpha)
}

private fun DrawScope.drawDressWithEffects(
    color: Color,
    lsX: Float, lsY: Float, rsX: Float, rsY: Float,
    lhX: Float, lhY: Float, rhX: Float, rhY: Float,
    laX: Float, laY: Float, raX: Float, raY: Float,
    alpha: Float
) {
    val padding = (rsX - lsX) * 0.06f
    val neckWidth = (rsX - lsX) * 0.22f
    val neckCenterX = (lsX + rsX) / 2f
    val shoulderY = (lsY + rsY) / 2f
    val hemY = (laY + raY) / 2f
    val hemFlare = (rhX - lhX) * 0.4f

    val path = Path().apply {
        moveTo(lsX - padding, lsY)
        cubicTo(lhX - padding * 2f, lhY, lhX - hemFlare, (lhY + hemY) / 2f, lhX - hemFlare, hemY)
        lineTo(rhX + hemFlare, hemY)
        cubicTo(rhX + hemFlare, (rhY + hemY) / 2f, rhX + padding * 2f, rhY, rsX + padding, rsY)
        lineTo(neckCenterX + neckWidth, shoulderY - (lhY - lsY) * 0.04f)
        quadraticTo(neckCenterX, shoulderY - (lhY - lsY) * 0.16f, neckCenterX - neckWidth, shoulderY - (lhY - lsY) * 0.04f)
        close()
    }
    drawGarmentWithEffects(path, color, alpha)
}

private fun DrawScope.drawGarmentWithEffects(path: Path, color: Color, alpha: Float) {
    // 1. Drop shadow
    withTransform({ translate(6f, 8f) }) {
        drawPath(path = path, color = Color.Black.copy(alpha = alpha * 0.25f), style = Fill)
    }

    // 2. Gradient fill — simulates fabric lighting
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = alpha * 0.95f),
                color.copy(alpha = alpha * 0.75f),
                color.copy(alpha = alpha * 0.85f)
            )
        ),
        style = Fill
    )

    // 3. Fabric texture — subtle dashed lines
    drawPath(
        path = path,
        color = Color.White.copy(alpha = alpha * 0.06f),
        style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
    )

    // 4. Outline
    drawPath(path = path, color = color.copy(alpha = (alpha * 1.1f).coerceAtMost(1f)), style = Stroke(width = 2f))

    // 5. Light reflection sheen
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = alpha * 0.18f), Color.Transparent),
            start = Offset.Zero,
            end = Offset(size.width * 0.4f, size.height * 0.4f)
        ),
        style = Fill
    )
}
