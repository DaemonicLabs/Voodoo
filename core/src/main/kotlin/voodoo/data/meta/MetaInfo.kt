package voodoo.data.meta

import kotlinx.serialization.Serializable

@Serializable
data class MetaInfo(
    val name: String,
    val value: String
)