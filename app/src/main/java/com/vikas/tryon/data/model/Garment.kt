package com.vikas.tryon.data.model

import androidx.compose.ui.graphics.Color

data class Garment(
    val id: Int,
    val name: String,
    val category: GarmentCategory,
    val color: Color,
    val description: String
)

enum class GarmentCategory(val displayName: String) {
    TOP("Tops"),
    BOTTOM("Bottoms"),
    DRESS("Dresses"),
    OUTERWEAR("Outerwear")
}
