package voodoo.data.curse

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Int,
    val name: String,
    val url: String,
    val avatarUrl: String
)