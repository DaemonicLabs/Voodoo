package voodoo.data.curse

import kotlinx.serialization.Serializable

@Serializable
data class Author(
        val name: String,
        val url: String
)