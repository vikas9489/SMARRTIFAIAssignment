package com.vikas.tryon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MeasurementHistoryEntity)

    @Query("SELECT * FROM measurement_history ORDER BY timestampMs DESC LIMIT 1")
    fun observeLatest(): Flow<MeasurementHistoryEntity?>

    @Query("SELECT * FROM measurement_history ORDER BY timestampMs DESC LIMIT 10")
    fun observeHistory(): Flow<List<MeasurementHistoryEntity>>

    @Query("DELETE FROM measurement_history")
    suspend fun clearAll()
}
