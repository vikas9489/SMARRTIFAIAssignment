package com.vikas.tryon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AvatarEntity::class,
        MeasurementHistoryEntity::class,
        GarmentFavouriteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TryOnDatabase : RoomDatabase() {
    abstract fun avatarDao(): AvatarDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun garmentFavouriteDao(): GarmentFavouriteDao
}
