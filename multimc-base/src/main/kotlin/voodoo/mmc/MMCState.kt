package voodoo.mmc

import kotlinx.serialization.Serializable

@Serializable
data class MMCState(
    val bounds: Bounds? = null
)