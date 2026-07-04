package com.vikas.tryon.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import com.vikas.tryon.R
import com.vikas.tryon.data.local.GarmentFavouriteDao
import com.vikas.tryon.data.local.GarmentFavouriteEntity
import com.vikas.tryon.data.local.ScannedGarmentDao
import com.vikas.tryon.data.local.ScannedGarmentEntity
import com.vikas.tryon.data.model.Garment
import com.vikas.tryon.data.model.GarmentCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarmentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val favouriteDao: GarmentFavouriteDao,
    private val scannedGarmentDao: ScannedGarmentDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _selectedGarmentId = MutableStateFlow<Int?>(null)
    val selectedGarmentId: Flow<Int?> = _selectedGarmentId.asStateFlow()

    // Scanned garments loaded from Room + disk on every change
    val scannedGarments: Flow<List<Garment>> = scannedGarmentDao.observeAll()
        .map { entities -> entities.mapNotNull { it.toGarment() } }
        .flowOn(Dispatchers.IO)

    val favouriteIds: Flow<List<Int>> = favouriteDao.observeFavouriteIds()

    private val sampleGarments = listOf(
        Garment(1,  "Classic White Tee",  GarmentCategory.TOP,       Color(0xFFF5F5F5), "Minimalist cotton crew neck",    R.drawable.ic_garment_tshirt),
        Garment(2,  "Navy Polo",           GarmentCategory.TOP,       Color(0xFF1A237E), "Slim fit polo shirt",             R.drawable.ic_garment_polo),
        Garment(3,  "Red Flannel",         GarmentCategory.TOP,       Color(0xFFC62828), "Casual plaid flannel shirt",      R.drawable.ic_garment_tshirt),
        Garment(4,  "Black Hoodie",        GarmentCategory.TOP,       Color(0xFF212121), "Oversized fleece hoodie",         R.drawable.ic_garment_hoodie),
        Garment(5,  "Olive Bomber",        GarmentCategory.OUTERWEAR, Color(0xFF558B2F), "Lightweight bomber jacket",       R.drawable.ic_garment_jacket),
        Garment(6,  "Denim Jacket",        GarmentCategory.OUTERWEAR, Color(0xFF1565C0), "Classic blue denim jacket",       R.drawable.ic_garment_jacket),
        Garment(7,  "Camel Blazer",        GarmentCategory.OUTERWEAR, Color(0xFFC8A876), "Slim fit casual blazer",          R.drawable.ic_garment_jacket),
        Garment(8,  "Slim Jeans",          GarmentCategory.BOTTOM,    Color(0xFF37474F), "Dark wash slim fit jeans",        R.drawable.ic_garment_pants),
        Garment(9,  "Khaki Chinos",        GarmentCategory.BOTTOM,    Color(0xFFD4A76A), "Straight cut chino pants",        R.drawable.ic_garment_pants),
        Garment(10, "Floral Dress",        GarmentCategory.DRESS,     Color(0xFFE91E63), "Midi length floral print",        R.drawable.ic_garment_dress),
        Garment(11, "Black Maxi",          GarmentCategory.DRESS,     Color(0xFF263238), "Elegant black maxi dress",        R.drawable.ic_garment_dress),
        Garment(12, "Striped Tee",         GarmentCategory.TOP,       Color(0xFF607D8B), "Breton stripe sailor top",        R.drawable.ic_garment_tshirt)
    )

    fun getAllGarments(): List<Garment> = sampleGarments

    fun getGarmentsByCategory(category: GarmentCategory): List<Garment> =
        sampleGarments.filter { it.category == category }

    fun getGarmentById(id: Int): Garment? = sampleGarments.find { it.id == id }

    fun selectGarment(id: Int?) {
        _selectedGarmentId.value = id
    }

    fun getSelectedGarment(): Garment? =
        _selectedGarmentId.value?.let { id -> getGarmentById(id) }

    fun addScannedGarment(name: String, bitmap: Bitmap, category: GarmentCategory): Garment {
        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        scope.launch {
            val file = saveBitmapToFile(bitmap, id)
            scannedGarmentDao.insert(
                ScannedGarmentEntity(
                    id = id,
                    name = name.ifBlank { "Scanned Garment" },
                    category = category.name,
                    bitmapPath = file.absolutePath
                )
            )
        }
        // Return immediately so the UI can navigate — Flow will update shortly
        val garment = Garment(
            id = id,
            name = name.ifBlank { "Scanned Garment" },
            category = category,
            color = Color(0xFF9E9E9E),
            description = "Scanned garment",
            scannedBitmap = bitmap
        )
        _selectedGarmentId.value = garment.id
        return garment
    }

    fun toggleFavourite(garmentId: Int) {
        scope.launch {
            val count = favouriteDao.isFavourite(garmentId)
            if (count > 0) favouriteDao.removeFavourite(garmentId)
            else favouriteDao.addFavourite(GarmentFavouriteEntity(garmentId))
        }
    }

    private suspend fun saveBitmapToFile(bitmap: Bitmap, id: Int): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "garments").also { it.mkdirs() }
            val file = File(dir, "$id.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file
        }

    private fun ScannedGarmentEntity.toGarment(): Garment? {
        val file = File(bitmapPath)
        if (!file.exists()) return null
        // Trim transparent margins — older scans were saved with padding around
        // the garment, which breaks overlay sizing.
        val bitmap = BitmapFactory.decodeFile(bitmapPath)
            ?.let { com.vikas.tryon.utils.BitmapUtils.trimTransparent(it) }
            ?: return null
        return Garment(
            id = id,
            name = name,
            category = runCatching { GarmentCategory.valueOf(category) }.getOrDefault(GarmentCategory.SCANNED),
            color = Color(0xFF9E9E9E),
            description = "Scanned garment",
            scannedBitmap = bitmap
        )
    }
}
