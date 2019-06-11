package voodoo.pack.sk

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class SKWorkspace(
    val packs: MutableSet<SKLocation> = mutableSetOf(),
    var packageListingEntries: List<String> = listOf(),
    var packageListingType: String = "STATIC"
)