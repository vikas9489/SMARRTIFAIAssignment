package com.vikas.tryon.presentation.measurement

import androidx.lifecycle.ViewModel
import com.vikas.tryon.data.model.Avatar
import com.vikas.tryon.data.model.BodyMeasurement
import com.vikas.tryon.data.repository.AvatarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val avatarRepository: AvatarRepository
) : ViewModel() {

    val bodyMeasurement: Flow<BodyMeasurement> = avatarRepository.bodyMeasurement
    val avatar: Flow<Avatar> = avatarRepository.avatar
}
