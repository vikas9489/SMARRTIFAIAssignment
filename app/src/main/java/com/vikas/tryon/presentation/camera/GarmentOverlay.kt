package com.vikas.tryon.presentation.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.vikas.tryon.data.model.Garment
import com.vikas.tryon.data.model.GarmentCategory

@Composable
fun GarmentOverlay(
    result: PoseLandmarkerResult?,
    garment: Garment?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (result == null || garment == null || result.landmarks().isEmpty()) return@Canvas

        val landmarks = result.landmarks()[0]
        if (landmarks.size <= 28) return@Canvas

        val w = size.width
        val h = size.height

        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]

        val lsVis = leftShoulder.visibility().orElse(0f)
        val rsVis = rightShoulder.visibility().orElse(0f)
        if (lsVis < 0.4f || rsVis < 0.4f) return@Canvas

        val lsX = leftShoulder.x() * w
        val lsY = leftShoulder.y() * h
        val rsX = rightShoulder.x() * w
        val rsY = rightShoulder.y() * h
        val lhX = leftHip.x() * w
        val lhY = leftHip.y() * h
        val rhX = rightHip.x() * w
        val rhY = rightHip.y() * h

        when (garment.category) {
            GarmentCategory.TOP, GarmentCategory.OUTERWEAR ->
                drawTopGarment(garment.color, lsX, lsY, rsX, rsY, lhX, lhY, rhX, rhY, garment.category)
            GarmentCategory.BOTTOM ->
                drawBottomGarment(garment.color, lhX, lhY, rhX, rhY,
                    landmarks[27].x() * w, landmarks[27].y() * h,
                    landmarks[28].x() * w, landmarks[28].y() * h)
            GarmentCategory.DRESS ->
                drawDressGarment(garment.color, lsX, lsY, rsX, rsY, lhX, lhY, rhX, rhY,
                    landmarks[27].x() * w, landmarks[27].y() * h,
                    landmarks[28].x() * w, landmarks[28].y() * h)
        }
    }
}

private fun DrawScope.drawTopGarment(
    color: Color,
    lsX: Float, lsY: Float, rsX: Float, rsY: Float,
    lhX: Float, lhY: Float, rhX: Float, rhY: Float,
    category: GarmentCategory
) {
    val padding = (rsX - lsX) * 0.08f
    val neckWidth = (rsX - lsX) * 0.25f
    val neckCenterX = (lsX + rsX) / 2f
    val shoulderY = (lsY + rsY) / 2f

    // Collar extra height for outerwear
    val sleeveExtra = if (category == GarmentCategory.OUTERWEAR) 1.3f else 1.0f

    val path = Path().apply {
        moveTo(lsX - padding, lsY)
        // Left sleeve
        lineTo(lsX - padding * 4f * sleeveExtra, lsY + (lhY - lsY) * 0.35f)
        lineTo(lsX - padding * 2f, lsY + (lhY - lsY) * 0.45f)
        // Left body down to hip
        lineTo(lhX - padding, lhY)
        // Hem
        lineTo(rhX + padding, rhY)
        // Right body up
        lineTo(rsX + padding * 2f, rsY + (rhY - rsY) * 0.45f)
        // Right sleeve
        lineTo(rsX + padding * 4f * sleeveExtra, rsY + (rhY - rsY) * 0.35f)
        lineTo(rsX + padding, rsY)
        // Neckline
        lineTo(neckCenterX + neckWidth, shoulderY - (lhY - lsY) * 0.05f)
        quadraticTo(neckCenterX, shoulderY - (lhY - lsY) * 0.12f, neckCenterX - neckWidth, shoulderY - (lhY - lsY) * 0.05f)
        close()
    }

    drawPath(path, color = color.copy(alpha = 0.72f), style = Fill)
    drawPath(path, color = color.copy(alpha = 0.9f), style = Stroke(width = 2.5f))
}

private fun DrawScope.drawBottomGarment(
    color: Color,
    lhX: Float, lhY: Float, rhX: Float, rhY: Float,
    laX: Float, laY: Float, raX: Float, raY: Float
) {
    val padding = (rhX - lhX) * 0.1f
    val crotchX = (lhX + rhX) / 2f
    val crotchY = (lhY + rhY) / 2f + (laY - lhY) * 0.3f

    val path = Path().apply {
        moveTo(lhX - padding, lhY)
        lineTo(rhX + padding, rhY)
        // Right leg
        lineTo(raX + padding, raY)
        lineTo(crotchX, crotchY)
        // Left leg
        lineTo(laX - padding, laY)
        close()
    }

    drawPath(path, color = color.copy(alpha = 0.72f), style = Fill)
    drawPath(path, color = color.copy(alpha = 0.9f), style = Stroke(width = 2.5f))
}

private fun DrawScope.drawDressGarment(
    color: Color,
    lsX: Float, lsY: Float, rsX: Float, rsY: Float,
    lhX: Float, lhY: Float, rhX: Float, rhY: Float,
    laX: Float, laY: Float, raX: Float, raY: Float
) {
    val padding = (rsX - lsX) * 0.06f
    val neckWidth = (rsX - lsX) * 0.22f
    val neckCenterX = (lsX + rsX) / 2f
    val shoulderY = (lsY + rsY) / 2f
    val hemY = (laY + raY) / 2f

    val path = Path().apply {
        moveTo(lsX - padding, lsY)
        lineTo(lhX - padding * 2.5f, hemY)
        lineTo(rhX + padding * 2.5f, hemY)
        lineTo(rsX + padding, rsY)
        // Neckline
        lineTo(neckCenterX + neckWidth, shoulderY - (lhY - lsY) * 0.04f)
        quadraticTo(neckCenterX, shoulderY - (lhY - lsY) * 0.14f, neckCenterX - neckWidth, shoulderY - (lhY - lsY) * 0.04f)
        close()
    }

    drawPath(path, color = color.copy(alpha = 0.72f), style = Fill)
    drawPath(path, color = color.copy(alpha = 0.9f), style = Stroke(width = 2.5f))
}
