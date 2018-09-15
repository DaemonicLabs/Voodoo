package voodoo.pack.sk

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class SKWorkspace(
    @Optional val packs: MutableSet<SKLocation> = mutableSetOf(),
    @Optional var packageListingEntries: List<String> = listOf(),
    @Optional var packageListingType: String = "STATIC"
)