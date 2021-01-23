package voodoo.pack

import kotlinx.serialization.Serializable

@Serializable
data class PackConfig(
    val versionAlias: Map<String, String> = mapOf()
)