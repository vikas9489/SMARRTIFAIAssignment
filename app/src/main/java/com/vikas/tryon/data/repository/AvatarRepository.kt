package com.vikas.tryon.data.repository

import com.vikas.tryon.data.model.Avatar
import com.vikas.tryon.data.model.BodyMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarRepository @Inject constructor() {

    private val _avatar = MutableStateFlow(Avatar())
    val avatar: Flow<Avatar> = _avatar.asStateFlow()

    private val _bodyMeasurement = MutableStateFlow(BodyMeasurement())
    val bodyMeasurement: Flow<BodyMeasurement> = _bodyMeasurement.asStateFlow()

    fun updateAvatar(avatar: Avatar) {
        _avatar.value = avatar
    }

    fun updateMeasurement(measurement: BodyMeasurement) {
        _bodyMeasurement.value = measurement
    }

    fun getCurrentAvatar(): Avatar = _avatar.value

    fun getCurrentMeasurement(): BodyMeasurement = _bodyMeasurement.value
}
