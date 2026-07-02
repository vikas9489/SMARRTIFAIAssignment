package com.vikas.tryon.presentation.garment

import androidx.lifecycle.ViewModel
import com.vikas.tryon.data.model.Garment
import com.vikas.tryon.data.model.GarmentCategory
import com.vikas.tryon.data.repository.GarmentRepository
import com.vikas.tryon.domain.usecase.GetGarmentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class GarmentViewModel @Inject constructor(
    private val getGarmentsUseCase: GetGarmentsUseCase,
    private val garmentRepository: GarmentRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<GarmentCategory?>(null)
    val selectedCategory: StateFlow<GarmentCategory?> = _selectedCategory.asStateFlow()

    val selectedGarmentId: Flow<Int?> = garmentRepository.selectedGarmentId

    // Favourite IDs from Room — updates reactively
    val favouriteIds: Flow<List<Int>> = garmentRepository.favouriteIds

    val garments: List<Garment>
        get() = _selectedCategory.value
            ?.let { getGarmentsUseCase.getByCategory(it) }
            ?: getGarmentsUseCase.getAllGarments()

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
