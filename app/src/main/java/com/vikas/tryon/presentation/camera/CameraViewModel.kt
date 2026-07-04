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
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.vikas.tryon.data.model.Garment
import com.vikas.tryon.data.repository.AvatarRepository
import com.vikas.tryon.data.repository.GarmentRepository
import com.vikas.tryon.data.repository.OutfitRepository
import com.vikas.tryon.domain.usecase.EstimateMeasurementsUseCase
import com.vikas.tryon.utils.GarmentBitmapLoader
import com.vikas.tryon.utils.LandmarkSmoother
import com.vikas.tryon.utils.SmoothedLandmarks
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val garmentRepository: GarmentRepository,
    private val avatarRepository: AvatarRepository,
    private val outfitRepository: OutfitRepository,
    private val estimateMeasurementsUseCase: EstimateMeasurementsUseCase,
    private val landmarkSmoother: LandmarkSmoother,
    private val garmentBitmapLoader: GarmentBitmapLoader
) : ViewModel() {

    // Raw result still used for measurement estimation (needs full result)
    private val _poseResult = MutableStateFlow<PoseLandmarkerResult?>(null)
    val poseResult: StateFlow<PoseLandmarkerResult?> = _poseResult.asStateFlow()

    // Smoothed landmarks used for rendering overlays
    private val _smoothedLandmarks = MutableStateFlow<SmoothedLandmarks?>(null)
    val smoothedLandmarks: StateFlow<SmoothedLandmarks?> = _smoothedLandmarks.asStateFlow()

    private val _selectedGarment = MutableStateFlow<Garment?>(null)
    val selectedGarment: StateFlow<Garment?> = _selectedGarment.asStateFlow()

    // Pre-rendered bitmap for the camera canvas (scanned or drawable-based)
    private val _garmentBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val garmentBitmap: StateFlow<android.graphics.Bitmap?> = _garmentBitmap.asStateFlow()

    private val _isLandmarkerReady = MutableStateFlow(false)
    val isLandmarkerReady: StateFlow<Boolean> = _isLandmarkerReady.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _showGarment = MutableStateFlow(true)
    val showGarment: StateFlow<Boolean> = _showGarment.asStateFlow()

    private val _outfitSaved = MutableStateFlow(false)
    val outfitSaved: StateFlow<Boolean> = _outfitSaved.asStateFlow()

    private var poseLandmarker: PoseLandmarker? = null

    // Local cache of scanned garments so we can look them up by ID after restart
    private var cachedScannedGarments: List<Garment> = emptyList()

    init {
        initializeLandmarker()

        // Keep the scanned garment cache up to date
        viewModelScope.launch {
            garmentRepository.scannedGarments.collect { list ->
                cachedScannedGarments = list
                // If the currently selected garment is a scanned one whose bitmap was
                // loaded from DB (not from the transient addScannedGarment call), refresh it
                val currentId = _selectedGarment.value?.id
                if (currentId != null && list.any { it.id == currentId }) {
                    val refreshed = list.find { it.id == currentId }
                    if (refreshed != null && _garmentBitmap.value == null) {
                        _selectedGarment.value = refreshed
                        _garmentBitmap.value = refreshed.scannedBitmap
                    }
                }
            }
        }

        viewModelScope.launch {
            garmentRepository.selectedGarmentId.collect { id ->
                // Look up in sample library first, then fall back to persisted scanned garments
                val garment = id?.let {
                    garmentRepository.getGarmentById(it)
                        ?: cachedScannedGarments.find { g -> g.id == it }
                }
                _selectedGarment.value = garment
                _garmentBitmap.value = withContext(Dispatchers.Default) {
                    try {
                        when {
                            garment == null -> null
                            garment.scannedBitmap != null -> garment.scannedBitmap
                            garment.imageRes != 0 -> garmentBitmapLoader.load(garment.imageRes, garment.color)
                            else -> null
                        }
                    } catch (e: Exception) {
                        Log.e("CameraViewModel", "Garment bitmap load failed: ${e.message}")
                        null
                    }
                }
            }
        }
    }

    private fun initializeLandmarker() {
        viewModelScope.launch {
            try {
                val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(
                        BaseOptions.builder()
                            .setModelAssetPath("pose_landmarker_lite.task")
                            .build()
                    )
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumPoses(1)
                    .setMinPoseDetectionConfidence(0.3f)
                    .setMinPosePresenceConfidence(0.3f)
                    .setMinTrackingConfidence(0.3f)
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
            _smoothedLandmarks.value = landmarkSmoother.smooth(result)

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
        landmarkSmoother.reset()
    }

    fun toggleGarmentOverlay() {
        _showGarment.value = !_showGarment.value
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Float): Bitmap {
        if (rotationDegrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun saveOutfit(screenshot: Bitmap) {
        val garment = _selectedGarment.value ?: return
        viewModelScope.launch {
            try {
                outfitRepository.saveOutfit(screenshot, garment.name)
                _outfitSaved.value = true
            } catch (e: Exception) {
                Log.e("CameraViewModel", "saveOutfit failed: ${e.message}")
            }
        }
    }

    fun consumeOutfitSaved() {
        _outfitSaved.value = false
    }

    override fun onCleared() {
        super.onCleared()
        poseLandmarker?.close()
    }
}
