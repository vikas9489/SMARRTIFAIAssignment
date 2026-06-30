package com.vikas.tryon.utils

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.vikas.tryon.data.model.BodyMeasurement
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class BodyMeasurementEstimator @Inject constructor() {

    // MediaPipe BlazePose landmark indices
    private object Landmark {
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_KNEE = 25
        const val RIGHT_KNEE = 26
        const val LEFT_ANKLE = 27
        const val RIGHT_ANKLE = 28
    }

    fun estimate(result: PoseLandmarkerResult, referenceHeightCm: Float): BodyMeasurement {
        if (result.landmarks().isEmpty()) return BodyMeasurement()

        val landmarks = result.landmarks()[0]
        if (landmarks.size <= Landmark.RIGHT_ANKLE) return BodyMeasurement()

        val leftShoulder = landmarks[Landmark.LEFT_SHOULDER]
        val rightShoulder = landmarks[Landmark.RIGHT_SHOULDER]
        val leftHip = landmarks[Landmark.LEFT_HIP]
        val rightHip = landmarks[Landmark.RIGHT_HIP]
        val leftKnee = landmarks[Landmark.LEFT_KNEE]
        val leftAnkle = landmarks[Landmark.LEFT_ANKLE]

        val avgConfidence = listOf(
            leftShoulder, rightShoulder, leftHip, rightHip
        ).map { it.visibility().orElse(0f) }.average().toFloat()

        if (avgConfidence < 0.5f) return BodyMeasurement(confidence = avgConfidence)

        // Pixel distances in normalized coordinates (0.0 - 1.0)
        val shoulderWidthNorm = dist(leftShoulder.x(), leftShoulder.y(),
            rightShoulder.x(), rightShoulder.y())
        val hipWidthNorm = dist(leftHip.x(), leftHip.y(),
            rightHip.x(), rightHip.y())
        val torsoHeightNorm = dist(
            (leftShoulder.x() + rightShoulder.x()) / 2f,
            (leftShoulder.y() + rightShoulder.y()) / 2f,
            (leftHip.x() + rightHip.x()) / 2f,
            (leftHip.y() + rightHip.y()) / 2f
        )
        val legLengthNorm = dist(leftHip.x(), leftHip.y(), leftAnkle.x(), leftAnkle.y())

        // Total body height in normalized units (approximate)
        val bodyHeightNorm = torsoHeightNorm + legLengthNorm + 0.15f // head approx

        // Scale factor: cm per normalized unit
        val scaleFactor = if (bodyHeightNorm > 0f) referenceHeightCm / bodyHeightNorm else 0f

        val shoulderWidthCm = shoulderWidthNorm * scaleFactor
        val hipWidthCm = hipWidthNorm * scaleFactor

        // Body circumferences estimated from width (front-view approximation)
        // Chest ≈ shoulder width * π / 1.25 (rough ellipse perimeter approximation)
        val chestCm = shoulderWidthCm * Math.PI.toFloat() / 1.25f
        val waistCm = hipWidthCm * Math.PI.toFloat() / 1.4f
        val hipsCm = hipWidthCm * Math.PI.toFloat() / 1.2f
        val inseamCm = legLengthNorm * scaleFactor * 0.9f // adjust for footwear

        return BodyMeasurement(
            chestCm = chestCm.coerceIn(60f, 160f),
            waistCm = waistCm.coerceIn(55f, 150f),
            hipsCm = hipsCm.coerceIn(60f, 160f),
            shoulderWidthCm = shoulderWidthCm.coerceIn(30f, 70f),
            inseamCm = inseamCm.coerceIn(50f, 100f),
            confidence = avgConfidence
        )
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}
