package com.vikas.tryon.domain.usecase

import com.vikas.tryon.data.model.Garment
import com.vikas.tryon.data.model.GarmentCategory
import com.vikas.tryon.data.repository.GarmentRepository
import javax.inject.Inject

class GetGarmentsUseCase @Inject constructor(
    private val garmentRepository: GarmentRepository
) {
    fun getAllGarments(): List<Garment> = garmentRepository.getAllGarments()

    fun getByCategory(category: GarmentCategory): List<Garment> =
        garmentRepository.getGarmentsByCategory(category)
}
