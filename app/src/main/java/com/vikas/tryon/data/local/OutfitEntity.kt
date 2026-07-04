package com.vikas.tryon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_outfits")
data class OutfitEntity(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    val garmentName: String,
    val imagePath: String,
    val savedAtMs: Long = System.currentTimeMillis()
)
