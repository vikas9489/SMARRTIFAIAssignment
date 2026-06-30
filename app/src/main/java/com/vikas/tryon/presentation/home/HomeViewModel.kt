package com.vikas.tryon.presentation.home

import androidx.lifecycle.ViewModel
import com.vikas.tryon.data.model.Avatar
import com.vikas.tryon.data.model.Garment
import com.vikas.tryon.data.repository.AvatarRepository
import com.vikas.tryon.domain.usecase.GetGarmentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getGarmentsUseCase: GetGarmentsUseCase,
    avatarRepository: AvatarRepository
) : ViewModel() {

    val avatar: kotlinx.coroutines.flow.Flow<Avatar> = avatarRepository.avatar

    val featuredGarments: List<Garment>
        get() = getGarmentsUseCase.getAllGarments().take(4)
}
