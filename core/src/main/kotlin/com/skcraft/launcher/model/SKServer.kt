package com.skcraft.launcher.model

import kotlinx.serialization.Serializable

@Serializable
data class SKServer(
    val serverHost: String,
    val serverPort: Int = 25565
)