package com.vikas.tryon.presentation.scan

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vikas.tryon.data.model.GarmentCategory
import com.vikas.tryon.data.repository.GarmentRepository
import com.vikas.tryon.utils.BackgroundRemover
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ScanState {
    data object Idle : ScanState()
    data object Capturing : ScanState()
    data object Processing : ScanState()
    data class Preview(val bitmap: Bitmap) : ScanState()
    data class Error(val message: String) : ScanState()
    data object Saved : ScanState()
}

@HiltViewModel
class ScanGarmentViewModel @Inject constructor(
    private val backgroundRemover: BackgroundRemover,
    private val garmentRepository: GarmentRepository
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _selectedCategory = MutableStateFlow(GarmentCategory.TOP)
    val selectedCategory: StateFlow<GarmentCategory> = _selectedCategory.asStateFlow()

    fun onImageCaptured(bitmap: Bitmap) {
        _scanState.value = ScanState.Processing
        viewModelScope.launch {
            try {
                val cleaned = backgroundRemover.removeBackground(bitmap)
                _scanState.value = ScanState.Preview(cleaned)
            } catch (e: Exception) {
                Log.e("ScanVM", "Background removal failed: ${e.message}")
                // Fall back to original bitmap if segmentation fails
                _scanState.value = ScanState.Preview(bitmap)
            }
        }
    }

    fun selectCategory(category: GarmentCategory) {
        _selectedCategory.value = category
    }

    fun saveGarment(name: String) {
        val state = _scanState.value as? ScanState.Preview ?: return
        garmentRepository.addScannedGarment(
            name = name.ifBlank { "Scanned Garment" },
            bitmap = state.bitmap,
            category = _selectedCategory.value
        )
        _scanState.value = ScanState.Saved
    }

    fun retake() {
        _scanState.value = ScanState.Idle
    }
}
