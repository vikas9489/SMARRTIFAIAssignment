package com.vikas.tryon.data.model

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

data class Garment(
    val id: Int,
    val name: String,
    val category: GarmentCategory,
    val color: Color,
    val description: String,
    @DrawableRes val imageRes: Int = 0,
    val scannedBitmap: Bitmap? = null
) {
    val isScanned: Boolean get() = scannedBitmap != null
}

enum class GarmentCategory(val displayName: String) {
    TOP("Tops"),
    BOTTOM("Bottoms"),
    DRESS("Dresses"),
    OUTERWEAR("Outerwear"),
    SCANNED("My Scans")
}
