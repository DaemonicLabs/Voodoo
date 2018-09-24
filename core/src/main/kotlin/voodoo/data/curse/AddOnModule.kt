package voodoo.data.curse

import kotlinx.serialization.Serializable

@Serializable
data class AddOnModule(
    val fingerprint: Long,
    val foldername: String
)
