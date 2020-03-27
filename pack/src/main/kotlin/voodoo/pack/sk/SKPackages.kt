package voodoo.pack.sk

import kotlinx.serialization.Serializable

@Serializable
data class SKPackages(
    val minimumVersion: Int = 1,
    var packages: List<SkPackageFragment> = emptyList()
)
