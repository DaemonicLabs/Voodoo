package moe.nikky.voodoo.format

import kotlinx.serialization.Serializable

@Serializable
data class VersionsList(
    val versions: Map<String, VersionEntry>
)