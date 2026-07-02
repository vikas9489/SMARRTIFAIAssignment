package com.vikas.tryon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vikas.tryon.data.model.Avatar
import com.vikas.tryon.data.model.BodyType
import com.vikas.tryon.data.model.SkinTone

@Entity(tableName = "avatar")
data class AvatarEntity(
    @PrimaryKey val id: Int = 1, // single-row table, always id=1
    val name: String,
    val heightCm: Int,
    val weightKg: Int,
    val bodyType: String,
    val skinTone: String
) {
    fun toAvatar() = Avatar(
        name = name,
        heightCm = heightCm,
        weightKg = weightKg,
        bodyType = BodyType.valueOf(bodyType),
        skinTone = SkinTone.valueOf(skinTone)
    )

    companion object {
        fun from(avatar: Avatar) = AvatarEntity(
            name = avatar.name,
            heightCm = avatar.heightCm,
            weightKg = avatar.weightKg,
            bodyType = avatar.bodyType.name,
            skinTone = avatar.skinTone.name
        )
    }
}
