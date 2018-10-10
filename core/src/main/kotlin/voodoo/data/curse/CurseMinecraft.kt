package voodoo.data.curse

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class CurseMinecraft(
    @Optional val version: String = "",
    @Optional val modLoaders: List<CurseModLoader> = emptyList()
)