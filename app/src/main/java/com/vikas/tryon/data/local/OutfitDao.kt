package com.vikas.tryon.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitDao {
    @Query("SELECT * FROM saved_outfits ORDER BY savedAtMs DESC")
    fun observeAll(): Flow<List<OutfitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OutfitEntity)

    @Query("DELETE FROM saved_outfits WHERE id = :id")
    suspend fun delete(id: Long)
}
