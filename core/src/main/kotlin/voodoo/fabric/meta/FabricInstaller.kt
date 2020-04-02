package voodoo.fabric.meta

import kotlinx.serialization.Serializable

@Serializable
data class FabricInstaller(
    val url: String,
    val maven: String,
    val version: String,
    val stable: Boolean
)