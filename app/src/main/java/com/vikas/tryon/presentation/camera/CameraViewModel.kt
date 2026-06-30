package com.vikas.tryon.presentation.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerOptions
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.vikas.tryon.data.model.Garment
import com.vikas.tryon.data.repository.AvatarRepository
import com.vikas.tryon.data.repository.GarmentRepository
import com.vikas.tryon.domain.usecase.EstimateMeasurementsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val garmentRepository: GarmentRepository,
    private val avatarRepository: AvatarRepository,
    private val estimateMeasurementsUseCase: EstimateMeasurementsUseCase
) : ViewModel() {

    private val _poseResult = MutableStateFlow<PoseLandmarkerResult?>(null)
    val poseResult: StateFlow<PoseLandmarkerResult?> = _poseResult.asStateFlow()

    private val _selectedGarment = MutableStateFlow<Garment?>(null)
    val selectedGarment: StateFlow<Garment?> = _selectedGarment.asStateFlow()

    private val _isLandmarkerReady = MutableStateFlow(false)
    val isLandmarkerReady: StateFlow<Boolean> = _isLandmarkerReady.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _showGarment = MutableStateFlow(true)
    val showGarment: StateFlow<Boolean> = _showGarment.asStateFlow()

    private var poseLandmarker: PoseLandmarker? = null

    init {
        initializeLandmarker()
        viewModelScope.launch {
            garmentRepository.selectedGarmentId.collect { id ->
                _selectedGarment.value = id?.let { garmentRepository.getGarmentById(it) }
            }
        }
    }

    private fun initializeLandmarker() {
        viewModelScope.launch {
            try {
                val options = PoseLandmarkerOptions.builder()
                    .setBaseOptions(
                        BaseOptions.builder()
                            .setModelAssetPath("pose_landmarker_lite.task")
                            .build()
                    )
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumPoses(1)
                    .setMinPoseDetectionConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .build()

                poseLandmarker = PoseLandmarker.createFromOptions(context, options)
                _isLandmarkerReady.value = true
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to initialize PoseLandmarker: ${e.message}")
                _isLandmarkerReady.value = false
            }
        }
    }

    fun processImageProxy(imageProxy: ImageProxy) {
        val landmarker = poseLandmarker ?: run { imageProxy.close(); return }

        try {
            val bitmap = imageProxy.toBitmap()
            val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            val result = landmarker.detect(mpImage)
            _poseResult.value = result

            if (result.landmarks().isNotEmpty()) {
                estimateMeasurementsUseCase(result)
            }
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Pose detection error: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    fun toggleCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun toggleGarmentOverlay() {
        _showGarment.value = !_showGarment.value
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Float): Bitmap {
        if (rotationDegrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onCleared() {
        super.onCleared()
        poseLandmarker?.close()
    }
}
