package moe.nikky.voodoo.format.modpack

import kotlinx.serialization.Serializable

@Serializable
data class Server(
    val serverHost: String,
    val serverPort: Int = 25565
)