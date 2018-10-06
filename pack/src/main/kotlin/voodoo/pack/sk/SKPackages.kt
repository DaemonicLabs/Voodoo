package voodoo.pack.sk

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class SKPackages(
    @Optional val minimumVersion: Int = 1,
    @Optional var packages: List<SkPackageFragment> = emptyList()
)
