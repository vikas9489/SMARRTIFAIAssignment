package com.vikas.tryon.utils

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

data class SmoothedLandmarks(
    val x: FloatArray,
    val y: FloatArray,
    val z: FloatArray,
    val visibility: FloatArray,
    val size: Int
) {
    fun getX(i: Int) = if (i < size) x[i] else 0f
    fun getY(i: Int) = if (i < size) y[i] else 0f
    fun getVisibility(i: Int) = if (i < size) visibility[i] else 0f
}

/**
 * Applies exponential moving average to landmark positions to reduce jitter
 * between frames, producing smoother garment overlay animation.
 */
@Singleton
class LandmarkSmoother @Inject constructor() {

    private val alpha = 0.4f // smoothing factor: lower = smoother but more lag
    private var smoothed: SmoothedLandmarks? = null

    fun smooth(result: PoseLandmarkerResult): SmoothedLandmarks? {
        if (result.landmarks().isEmpty()) {
            smoothed = null
            return null
        }
        val landmarks = result.landmarks()[0]
        val n = landmarks.size

        val current = smoothed
        if (current == null) {
            val s = SmoothedLandmarks(
                x = FloatArray(n) { landmarks[it].x() },
                y = FloatArray(n) { landmarks[it].y() },
                z = FloatArray(n) { landmarks[it].z() },
                visibility = FloatArray(n) { landmarks[it].visibility().orElse(0f) },
                size = n
            )
            smoothed = s
            return s
        }

        // EMA update
        for (i in 0 until n.coerceAtMost(current.size)) {
            current.x[i] += alpha * (landmarks[i].x() - current.x[i])
            current.y[i] += alpha * (landmarks[i].y() - current.y[i])
            current.z[i] += alpha * (landmarks[i].z() - current.z[i])
            current.visibility[i] += alpha * (landmarks[i].visibility().orElse(0f) - current.visibility[i])
        }
        return current
    }

    fun reset() { smoothed = null }
}
