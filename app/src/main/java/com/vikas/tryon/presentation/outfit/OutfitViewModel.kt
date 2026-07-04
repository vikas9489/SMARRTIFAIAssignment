package com.vikas.tryon.presentation.outfit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vikas.tryon.data.repository.OutfitRepository
import com.vikas.tryon.data.repository.SavedOutfit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OutfitViewModel @Inject constructor(
    private val outfitRepository: OutfitRepository
) : ViewModel() {

    val outfits: StateFlow<List<SavedOutfit>> = outfitRepository.outfits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteOutfit(id: Long) {
        viewModelScope.launch { outfitRepository.deleteOutfit(id) }
    }
}
