package com.vikas.tryon.data.repository

import com.vikas.tryon.data.local.AvatarDao
import com.vikas.tryon.data.local.AvatarEntity
import com.vikas.tryon.data.local.MeasurementDao
import com.vikas.tryon.data.local.MeasurementHistoryEntity
import com.vikas.tryon.data.model.Avatar
import com.vikas.tryon.data.model.BodyMeasurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarRepository @Inject constructor(
    private val avatarDao: AvatarDao,
    private val measurementDao: MeasurementDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Avatar — backed by Room, falls back to default if no row yet
    val avatar: Flow<Avatar> = avatarDao.observeAvatar()
        .map { it?.toAvatar() ?: Avatar() }

    // Live measurement (in-memory for current session)
    private val _bodyMeasurement = MutableStateFlow(BodyMeasurement())
    val bodyMeasurement: Flow<BodyMeasurement> = _bodyMeasurement.asStateFlow()

    // Measurement history from Room — latest 10 scans
    val measurementHistory: Flow<List<MeasurementHistoryEntity>> =
        measurementDao.observeHistory()

    fun updateAvatar(avatar: Avatar) {
        scope.launch { avatarDao.upsert(AvatarEntity.from(avatar)) }
    }

    fun updateMeasurement(measurement: BodyMeasurement) {
        _bodyMeasurement.value = measurement
        if (measurement.isValid) {
            scope.launch { measurementDao.insert(MeasurementHistoryEntity.from(measurement)) }
        }
    }

    fun getCurrentAvatar(): Avatar = Avatar() // use the Flow in UI
    fun getCurrentMeasurement(): BodyMeasurement = _bodyMeasurement.value
}
