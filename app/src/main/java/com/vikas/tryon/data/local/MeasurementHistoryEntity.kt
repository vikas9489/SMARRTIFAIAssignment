package com.vikas.tryon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vikas.tryon.data.model.BodyMeasurement

@Entity(tableName = "measurement_history")
data class MeasurementHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val chestCm: Float,
    val waistCm: Float,
    val hipsCm: Float,
    val shoulderWidthCm: Float,
    val inseamCm: Float,
    val confidence: Float,
    val suggestedSize: String
) {
    fun toMeasurement() = BodyMeasurement(
        chestCm = chestCm,
        waistCm = waistCm,
        hipsCm = hipsCm,
        shoulderWidthCm = shoulderWidthCm,
        inseamCm = inseamCm,
        confidence = confidence
    )

    companion object {
        fun from(m: BodyMeasurement) = MeasurementHistoryEntity(
            timestampMs = System.currentTimeMillis(),
            chestCm = m.chestCm,
            waistCm = m.waistCm,
            hipsCm = m.hipsCm,
            shoulderWidthCm = m.shoulderWidthCm,
            inseamCm = m.inseamCm,
            confidence = m.confidence,
            suggestedSize = m.suggestedSize()
        )
    }
}
