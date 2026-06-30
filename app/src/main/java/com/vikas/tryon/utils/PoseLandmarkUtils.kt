package com.vikas.tryon.utils

import androidx.compose.ui.geometry.Offset
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

object PoseLandmarkUtils {

    // Skeleton connections (pairs of landmark indices to draw lines between)
    val POSE_CONNECTIONS = listOf(
        // Torso
        11 to 12, // shoulders
        11 to 23, // left shoulder to hip
        12 to 24, // right shoulder to hip
        23 to 24, // hips
        // Left arm
        11 to 13, 13 to 15,
        // Right arm
        12 to 14, 14 to 16,
        // Left leg
        23 to 25, 25 to 27,
        // Right leg
        24 to 26, 26 to 28,
        // Face
        0 to 1, 1 to 2, 2 to 3, 3 to 7,
        0 to 4, 4 to 5, 5 to 6, 6 to 8,
        9 to 10 // mouth
    )

    fun getShoulderCenter(result: PoseLandmarkerResult, canvasWidth: Float, canvasHeight: Float): Offset? {
        if (result.landmarks().isEmpty()) return null
        val landmarks = result.landmarks()[0]
        if (landmarks.size <= 12) return null
        val ls = landmarks[11]
        val rs = landmarks[12]
        return Offset(
            ((ls.x() + rs.x()) / 2f) * canvasWidth,
            ((ls.y() + rs.y()) / 2f) * canvasHeight
        )
    }

    fun getShoulderWidth(result: PoseLandmarkerResult, canvasWidth: Float): Float? {
        if (result.landmarks().isEmpty()) return null
        val landmarks = result.landmarks()[0]
        if (landmarks.size <= 12) return null
        val ls = landmarks[11]
        val rs = landmarks[12]
        return Math.abs(rs.x() - ls.x()) * canvasWidth
    }

    fun getTorsoHeight(result: PoseLandmarkerResult, canvasHeight: Float): Float? {
        if (result.landmarks().isEmpty()) return null
        val landmarks = result.landmarks()[0]
        if (landmarks.size <= 24) return null
        val shoulderY = (landmarks[11].y() + landmarks[12].y()) / 2f
        val hipY = (landmarks[23].y() + landmarks[24].y()) / 2f
        return Math.abs(hipY - shoulderY) * canvasHeight
    }
}
