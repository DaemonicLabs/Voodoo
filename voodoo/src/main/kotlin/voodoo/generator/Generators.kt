package voodoo.generator

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Generators(
    @SerialName("\$schema")
    val schema: String,
    val generators: Map<String, Generator>
) {
}