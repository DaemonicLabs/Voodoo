package voodoo.pack.sk

data class SKWorkspace(
        val packs: MutableSet<SKLocation> = mutableSetOf(),
        var packageListingEntries: List<Any> = listOf(),
        var packageListingType: String = "STATIC"
)