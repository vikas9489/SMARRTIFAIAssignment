package com.vikas.tryon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "garment_favourites")
data class GarmentFavouriteEntity(
    @PrimaryKey val garmentId: Int,
    val addedAtMs: Long = System.currentTimeMillis()
)
