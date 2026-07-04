package com.vikas.tryon.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedGarmentDao {

    @Query("SELECT * FROM scanned_garments ORDER BY savedAtMs DESC")
    fun observeAll(): Flow<List<ScannedGarmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScannedGarmentEntity)

    @Query("DELETE FROM scanned_garments WHERE id = :id")
    suspend fun delete(id: Int)
}
