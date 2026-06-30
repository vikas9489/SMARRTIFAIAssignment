package com.vikas.tryon.domain.usecase

import com.vikas.tryon.data.model.Avatar
import com.vikas.tryon.data.repository.AvatarRepository
import javax.inject.Inject

class SaveAvatarUseCase @Inject constructor(
    private val avatarRepository: AvatarRepository
) {
    operator fun invoke(avatar: Avatar) = avatarRepository.updateAvatar(avatar)
}
