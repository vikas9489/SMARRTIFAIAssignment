package com.vikas.tryon.data.model

data class Avatar(
    val name: String = "My Avatar",
    val heightCm: Int = 170,
    val weightKg: Int = 65,
    val bodyType: BodyType = BodyType.ATHLETIC,
    val skinTone: SkinTone = SkinTone.MEDIUM
)

enum class BodyType(val displayName: String) {
    SLIM("Slim"),
    ATHLETIC("Athletic"),
    AVERAGE("Average"),
    PLUS("Plus Size")
}

enum class SkinTone(val displayName: String) {
    LIGHT("Light"),
    MEDIUM_LIGHT("Medium Light"),
    MEDIUM("Medium"),
    MEDIUM_DARK("Medium Dark"),
    DARK("Dark")
}
