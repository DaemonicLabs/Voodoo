package voodoo.data.provider

import kotlinx.serialization.Serializable

@Serializable
data class UpdateJson(
    val homepage: String = "",
    val promos: Map<String, String> = emptyMap()
)