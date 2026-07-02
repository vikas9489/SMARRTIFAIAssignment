package com.vikas.tryon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AvatarDao {
    @Query("SELECT * FROM avatar WHERE id = 1")
    fun observeAvatar(): Flow<AvatarEntity?>

    @Query("SELECT * FROM avatar WHERE id = 1")
    suspend fun getAvatar(): AvatarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AvatarEntity)
}
