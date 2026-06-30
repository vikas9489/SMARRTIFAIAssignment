package com.vikas.tryon.presentation.avatar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vikas.tryon.data.model.Avatar
import com.vikas.tryon.data.model.BodyType
import com.vikas.tryon.data.model.SkinTone
import com.vikas.tryon.data.repository.AvatarRepository
import com.vikas.tryon.domain.usecase.SaveAvatarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AvatarViewModel @Inject constructor(
    private val avatarRepository: AvatarRepository,
    private val saveAvatarUseCase: SaveAvatarUseCase
) : ViewModel() {

    private val _avatar = MutableStateFlow(avatarRepository.getCurrentAvatar())
    val avatar: StateFlow<Avatar> = _avatar.asStateFlow()

    fun updateName(name: String) {
        _avatar.value = _avatar.value.copy(name = name)
    }

    fun updateHeight(heightCm: Int) {
        _avatar.value = _avatar.value.copy(heightCm = heightCm)
    }

    fun updateWeight(weightKg: Int) {
        _avatar.value = _avatar.value.copy(weightKg = weightKg)
    }

    fun updateBodyType(bodyType: BodyType) {
        _avatar.value = _avatar.value.copy(bodyType = bodyType)
    }

    fun updateSkinTone(skinTone: SkinTone) {
        _avatar.value = _avatar.value.copy(skinTone = skinTone)
    }

    fun saveAvatar() {
        viewModelScope.launch {
            saveAvatarUseCase(_avatar.value)
        }
    }
}
