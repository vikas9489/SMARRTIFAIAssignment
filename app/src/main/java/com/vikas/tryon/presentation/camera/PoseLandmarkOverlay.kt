package com.vikas.tryon.presentation.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.vikas.tryon.utils.PoseLandmarkUtils

@Composable
fun PoseLandmarkOverlay(
    result: PoseLandmarkerResult?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (result == null || result.landmarks().isEmpty()) return@Canvas

        val landmarks = result.landmarks()[0]
        val w = size.width
        val h = size.height

        // Draw skeleton connections
        for ((startIdx, endIdx) in PoseLandmarkUtils.POSE_CONNECTIONS) {
            if (startIdx >= landmarks.size || endIdx >= landmarks.size) continue
            val start = landmarks[startIdx]
            val end = landmarks[endIdx]
            val startVis = start.visibility().orElse(0f)
            val endVis = end.visibility().orElse(0f)
            if (startVis < 0.5f || endVis < 0.5f) continue

            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(start.x() * w, start.y() * h),
                end = Offset(end.x() * w, end.y() * h),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        // Draw landmark points
        for (landmark in landmarks) {
            val vis = landmark.visibility().orElse(0f)
            if (vis < 0.5f) continue
            drawCircle(
                color = Color(0xFF76FF03),
                radius = 6f,
                center = Offset(landmark.x() * w, landmark.y() * h)
            )
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = Offset(landmark.x() * w, landmark.y() * h)
            )
        }
    }
}
