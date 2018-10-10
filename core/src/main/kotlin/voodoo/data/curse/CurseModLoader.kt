package voodoo.data.curse

import kotlinx.serialization.Serializable

@Serializable
data class CurseModLoader(
    val id: String,
    val primary: Boolean
)