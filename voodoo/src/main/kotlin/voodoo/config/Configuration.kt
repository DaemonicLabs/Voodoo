package voodoo.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import voodoo.pack.EntryOverride

@Serializable
data class Configuration(
    @SerialName("\$schema")
    val schema: String = "./schema/config.schema.json",
    val generators: Map<String, Generator> = mapOf(),
    val overrides: Map<String, EntryOverride> = mapOf()
)
