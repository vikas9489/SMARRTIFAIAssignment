package com.vikas.tryon.domain.usecase

import com.vikas.tryon.data.model.BodyMeasurement
import com.vikas.tryon.data.repository.AvatarRepository
import com.vikas.tryon.utils.BodyMeasurementEstimator
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import javax.inject.Inject

class EstimateMeasurementsUseCase @Inject constructor(
    private val avatarRepository: AvatarRepository,
    private val estimator: BodyMeasurementEstimator
) {
    operator fun invoke(result: PoseLandmarkerResult): BodyMeasurement {
        val referenceHeightCm = avatarRepository.getCurrentAvatar().heightCm.toFloat()
        val measurement = estimator.estimate(result, referenceHeightCm)
        avatarRepository.updateMeasurement(measurement)
        return measurement
    }
}
