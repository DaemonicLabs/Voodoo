package voodoo.pack

import kotlinx.serialization.Serializable

@Serializable
data class PackConfig(
    @Deprecated("no multiple versions anymore")
    val versionAlias: Map<String, String> = mapOf()
)