package voodoo.fabric.meta

import kotlinx.serialization.Serializable

@Serializable
data class FabricLoader(
    val separator: String,
    val build: Int,
    val maven: String,
    val version: String,
    val stable: Boolean
)