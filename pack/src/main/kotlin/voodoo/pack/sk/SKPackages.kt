package voodoo.pack.sk

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class SKPackages(
    @Optional val minimumVersion: Int = 1,
    @Optional var packages: List<SkPackageFragment> = emptyList()
)

@Serializable
data class SkPackageFragment(
    val title: String,
    val name: String,
    var version: String,
    val location: String,
    @Optional val priority: Int = 0
)