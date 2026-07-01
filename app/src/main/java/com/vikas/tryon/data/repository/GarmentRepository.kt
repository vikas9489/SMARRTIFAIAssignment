package com.vikas.tryon.data.repository

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.vikas.tryon.data.model.Garment
import com.vikas.tryon.data.model.GarmentCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarmentRepository @Inject constructor() {

    private val _selectedGarmentId = MutableStateFlow<Int?>(null)
    val selectedGarmentId: Flow<Int?> = _selectedGarmentId.asStateFlow()

    private val _scannedGarments = MutableStateFlow<List<Garment>>(emptyList())
    val scannedGarments: Flow<List<Garment>> = _scannedGarments.asStateFlow()

    private val sampleGarments = listOf(
        Garment(1, "Classic White Tee", GarmentCategory.TOP, Color(0xFFF5F5F5), "Minimalist cotton crew neck"),
        Garment(2, "Navy Polo", GarmentCategory.TOP, Color(0xFF1A237E), "Slim fit polo shirt"),
        Garment(3, "Red Flannel", GarmentCategory.TOP, Color(0xFFC62828), "Casual plaid flannel shirt"),
        Garment(4, "Black Hoodie", GarmentCategory.TOP, Color(0xFF212121), "Oversized fleece hoodie"),
        Garment(5, "Olive Bomber", GarmentCategory.OUTERWEAR, Color(0xFF558B2F), "Lightweight bomber jacket"),
        Garment(6, "Denim Jacket", GarmentCategory.OUTERWEAR, Color(0xFF1565C0), "Classic blue denim jacket"),
        Garment(7, "Camel Blazer", GarmentCategory.OUTERWEAR, Color(0xFFC8A876), "Slim fit casual blazer"),
        Garment(8, "Slim Jeans", GarmentCategory.BOTTOM, Color(0xFF37474F), "Dark wash slim fit jeans"),
        Garment(9, "Khaki Chinos", GarmentCategory.BOTTOM, Color(0xFFD4A76A), "Straight cut chino pants"),
        Garment(10, "Floral Dress", GarmentCategory.DRESS, Color(0xFFE91E63), "Midi length floral print"),
        Garment(11, "Black Maxi", GarmentCategory.DRESS, Color(0xFF263238), "Elegant black maxi dress"),
        Garment(12, "Striped Tee", GarmentCategory.TOP, Color(0xFF607D8B), "Breton stripe sailor top")
    )

    private var nextScannedId = 100

    fun getAllGarments(): List<Garment> = sampleGarments + _scannedGarments.value

    fun getGarmentsByCategory(category: GarmentCategory): List<Garment> =
        if (category == GarmentCategory.SCANNED) _scannedGarments.value
        else sampleGarments.filter { it.category == category }

    fun getGarmentById(id: Int): Garment? = getAllGarments().find { it.id == id }

    fun addScannedGarment(name: String, bitmap: Bitmap, category: GarmentCategory): Garment {
        val garment = Garment(
            id = nextScannedId++,
            name = name,
            category = category,
            color = Color(0xFF9E9E9E),
            description = "Scanned garment",
            scannedBitmap = bitmap
        )
        _scannedGarments.value = _scannedGarments.value + garment
        _selectedGarmentId.value = garment.id
        return garment
    }

    fun selectGarment(id: Int?) {
        _selectedGarmentId.value = id
    }

    fun getSelectedGarment(): Garment? =
        _selectedGarmentId.value?.let { id -> getGarmentById(id) }
}
