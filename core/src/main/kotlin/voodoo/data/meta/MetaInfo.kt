package voodoo.data.meta

import kotlinx.serialization.Serializable

@Serializable
data class MetaInfo(
    val key: String, // TODO: use some sort of enum ?
    val value: String
)