package com.vikas.tryon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AvatarEntity::class,
        MeasurementHistoryEntity::class,
        GarmentFavouriteEntity::class,
        ScannedGarmentEntity::class,
        OutfitEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TryOnDatabase : RoomDatabase() {
    abstract fun avatarDao(): AvatarDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun garmentFavouriteDao(): GarmentFavouriteDao
    abstract fun scannedGarmentDao(): ScannedGarmentDao
    abstract fun outfitDao(): OutfitDao
}
