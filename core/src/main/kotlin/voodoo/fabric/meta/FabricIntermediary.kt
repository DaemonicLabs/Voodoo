package voodoo.fabric.meta

import kotlinx.serialization.Serializable

@Serializable
data class FabricIntermediary(
    val maven: String,
    val version: String,
    val stable: Boolean
)