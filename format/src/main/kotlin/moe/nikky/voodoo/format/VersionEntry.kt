package moe.nikky.voodoo.format

import kotlinx.serialization.Serializable

@Serializable
data class VersionEntry(
    val version: String,
    val title: String,
    val location: String
)