package com.vikas.tryon.di

import android.content.Context
import androidx.room.Room
import com.vikas.tryon.data.local.AvatarDao
import com.vikas.tryon.data.local.GarmentFavouriteDao
import com.vikas.tryon.data.local.MeasurementDao
import com.vikas.tryon.data.local.TryOnDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TryOnDatabase =
        Room.databaseBuilder(context, TryOnDatabase::class.java, "tryon.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAvatarDao(db: TryOnDatabase): AvatarDao = db.avatarDao()

    @Provides
    fun provideMeasurementDao(db: TryOnDatabase): MeasurementDao = db.measurementDao()

    @Provides
    fun provideGarmentFavouriteDao(db: TryOnDatabase): GarmentFavouriteDao = db.garmentFavouriteDao()
}
