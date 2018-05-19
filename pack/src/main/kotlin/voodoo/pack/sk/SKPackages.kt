package voodoo.pack.sk

import java.util.*

data class SKPackages(
        val minimumVersion: Int = 1,
        var packages: List<SkPackageFragment> = emptyList()
)

data class SkPackageFragment(
        val title: String,
        val name: String,
        var version: String,
        val location: String,
        val priority: Int = 0
)