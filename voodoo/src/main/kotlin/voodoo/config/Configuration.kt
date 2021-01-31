package voodoo.config

import blue.endless.jankson.Jankson
import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import voodoo.data.Side
import voodoo.pack.EntryOverride
import voodoo.poet.generator.CurseSection
import voodoo.util.json
import java.io.File

@Serializable
data class Configuration(
    @Required
    @SerialName("\$schema")
    @JsonSchema.NoDefinition
    val schema: String = defaultSchema,
    @Required
    val curseforgeGenerators: Map<String, Generator.Curse> = mapOf(
        "Mods" to Generator.Curse(
            section = CurseSection.MODS
        ),
        "ResourcePacks" to Generator.Curse(
            section = CurseSection.RESOURCE_PACKS
        )
    ),
    @Required
    val forgeGenerators: Map<String, Generator.Forge> = mapOf(
        "Forge" to Generator.Forge()
    ),
    @Required
    val fabricGenerators: Map<String, Generator.Fabric> = mapOf(
        "Fabric" to Generator.Fabric(
            requireStable = true
        )
    ),
    @Required
    val overrides: Map<String, EntryOverride> = mapOf(
        "side_client" to EntryOverride.Common().apply {
            side = Side.CLIENT
        },
        "side_server" to EntryOverride.Common().apply {
            side = Side.SERVER
        }
    )
) {
    companion object {
        private val logger = KotlinLogging.logger {}
        const val defaultSchema = "./schema/config.schema.json"
        const val CONFIG_PATH = "config.json5"
        fun parse(rootDir: File): Configuration {
            val configFile = rootDir.resolve(CONFIG_PATH)

            val cleanedString = Jankson
                .builder()
                .build()
                .load(configFile.readText()).let { jsonObject ->
                    jsonObject.toJson(false, true);
                }

            return json.decodeFromString(serializer(), cleanedString)
        }
        fun parseOrDefault(rootDir: File, generateDefault: () -> Configuration = { Configuration() }): Configuration {
            val configFile = rootDir.resolve(CONFIG_PATH)
            return if(configFile.exists()) {
                val cleanedString = Jankson
                    .builder()
                    .build()
                    .load(configFile.readText()).let { jsonObject ->
                        jsonObject.toJson(false, true);
                    }

                json.decodeFromString(serializer(), cleanedString)
            } else {
                logger.trace { "generate default configuration" }
                generateDefault()
            }
        }
        fun save(rootDir: File, configuration: Configuration) {
            val configFile = rootDir.resolve(CONFIG_PATH)
            configFile.writeText(json.encodeToString(serializer(), configuration))
        }
    }
}
