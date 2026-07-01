package com.vikas.tryon.presentation.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.vikas.tryon.utils.PoseLandmarkUtils
import com.vikas.tryon.utils.SmoothedLandmarks

@Composable
fun PoseLandmarkOverlay(
    landmarks: SmoothedLandmarks?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (landmarks == null) return@Canvas

        val w = size.width
        val h = size.height

        // Draw skeleton connections
        for ((startIdx, endIdx) in PoseLandmarkUtils.POSE_CONNECTIONS) {
            if (startIdx >= landmarks.size || endIdx >= landmarks.size) continue
            if (landmarks.getVisibility(startIdx) < 0.5f || landmarks.getVisibility(endIdx) < 0.5f) continue

            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(landmarks.getX(startIdx) * w, landmarks.getY(startIdx) * h),
                end = Offset(landmarks.getX(endIdx) * w, landmarks.getY(endIdx) * h),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        // Draw landmark points
        for (i in 0 until landmarks.size) {
            if (landmarks.getVisibility(i) < 0.5f) continue
            val cx = landmarks.getX(i) * w
            val cy = landmarks.getY(i) * h
            drawCircle(color = Color(0xFF76FF03), radius = 6f, center = Offset(cx, cy))
            drawCircle(color = Color.White, radius = 3f, center = Offset(cx, cy))
        }
    }
}
