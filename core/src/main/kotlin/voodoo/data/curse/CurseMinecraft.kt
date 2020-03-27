package voodoo.data.curse

import kotlinx.serialization.Serializable

@Serializable
data class CurseMinecraft(
    val version: String = "",
    val modLoaders: List<CurseModLoader> = emptyList()
)