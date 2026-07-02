package com.vikas.tryon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GarmentFavouriteDao {
    @Query("SELECT garmentId FROM garment_favourites")
    fun observeFavouriteIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavourite(entity: GarmentFavouriteEntity)

    @Query("DELETE FROM garment_favourites WHERE garmentId = :id")
    suspend fun removeFavourite(id: Int)

    @Query("SELECT COUNT(*) FROM garment_favourites WHERE garmentId = :id")
    suspend fun isFavourite(id: Int): Int
}
