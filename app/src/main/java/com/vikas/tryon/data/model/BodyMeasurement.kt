package com.vikas.tryon.data.model

data class BodyMeasurement(
    val chestCm: Float = 0f,
    val waistCm: Float = 0f,
    val hipsCm: Float = 0f,
    val shoulderWidthCm: Float = 0f,
    val inseamCm: Float = 0f,
    val confidence: Float = 0f // 0.0 - 1.0
) {
    val isValid: Boolean get() = chestCm > 0f && confidence > 0.5f

    fun suggestedSize(): String {
        if (!isValid) return "Unknown"
        return when {
            chestCm < 88f -> "XS"
            chestCm < 96f -> "S"
            chestCm < 104f -> "M"
            chestCm < 112f -> "L"
            chestCm < 120f -> "XL"
            else -> "XXL"
        }
    }
}
