package com.vikas.tryon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_garments")
data class ScannedGarmentEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val category: String,       // GarmentCategory.name()
    val bitmapPath: String,     // absolute path to PNG file in filesDir
    val savedAtMs: Long = System.currentTimeMillis()
)
