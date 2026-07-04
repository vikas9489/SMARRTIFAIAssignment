package com.vikas.tryon.presentation.garment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vikas.tryon.data.model.Garment
import com.vikas.tryon.data.model.GarmentCategory
import com.vikas.tryon.data.repository.GarmentRepository
import com.vikas.tryon.domain.usecase.GetGarmentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class GarmentViewModel @Inject constructor(
    private val getGarmentsUseCase: GetGarmentsUseCase,
    private val garmentRepository: GarmentRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<GarmentCategory?>(null)
    val selectedCategory: StateFlow<GarmentCategory?> = _selectedCategory.asStateFlow()

    val selectedGarmentId: Flow<Int?> = garmentRepository.selectedGarmentId
    val favouriteIds: Flow<List<Int>> = garmentRepository.favouriteIds

    // Scanned garments loaded from Room — persists across restarts
    val scannedGarments: StateFlow<List<Garment>> = garmentRepository.scannedGarments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All garments = sample library + persisted scanned garments
    val garments: StateFlow<List<Garment>> = combine(
        _selectedCategory,
        scannedGarments
    ) { category, scanned ->
        val sample = if (category == null) getGarmentsUseCase.getAllGarments()
                     else getGarmentsUseCase.getByCategory(category)
        val filteredScanned = if (category == null || category == GarmentCategory.SCANNED) scanned
                              else emptyList()
        sample + filteredScanned
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), getGarmentsUseCase.getAllGarments())

    fun selectCategory(category: GarmentCategory?) {
        _selectedCategory.value = category
    }

    fun selectGarment(id: Int?) {
        garmentRepository.selectGarment(id)
    }

    fun toggleFavourite(garmentId: Int) {
        garmentRepository.toggleFavourite(garmentId)
    }
}
