package com.vikas.tryon.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.vikas.tryon.data.local.OutfitDao
import com.vikas.tryon.data.local.OutfitEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class SavedOutfit(
    val id: Long,
    val garmentName: String,
    val bitmap: Bitmap?,
    val savedAtMs: Long
)

@Singleton
class OutfitRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val outfitDao: OutfitDao
) {
    val outfits: Flow<List<SavedOutfit>> = outfitDao.observeAll().map { entities ->
        entities.map { entity ->
            SavedOutfit(
                id = entity.id,
                garmentName = entity.garmentName,
                bitmap = BitmapFactory.decodeFile(entity.imagePath),
                savedAtMs = entity.savedAtMs
            )
        }
    }

    suspend fun saveOutfit(bitmap: Bitmap, garmentName: String) = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "outfits").also { it.mkdirs() }
        val id = System.currentTimeMillis()
        val file = File(dir, "$id.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        outfitDao.insert(OutfitEntity(id = id, garmentName = garmentName, imagePath = file.absolutePath))
    }

    suspend fun deleteOutfit(id: Long) = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "outfits")
        File(dir, "$id.png").delete()
        outfitDao.delete(id)
    }
}
