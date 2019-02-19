package voodoo.data

import kotlinx.serialization.Serializable

@Serializable
data class ForgeVersion(
    val url: String,
    val fileName: String,
    val fullVersion: String,
    val forgeVersion: String
)