package voodoo.config

import blue.endless.jankson.Jankson
import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import voodoo.pack.EntryOverride
import voodoo.util.json
import java.io.File

@Serializable
data class Configuration(
    @Required
    @SerialName("\$schema")
    @JsonSchema.NoDefinition
    val schema: String = defaultSchema,
    @Required
    val curseforgeGenerators: Map<String, Generator.Curse> = mapOf(),
    @Required
    val forgeGenerators: Map<String, Generator.Forge> = mapOf(),
    @Required
    val fabricGenerators: Map<String, Generator.Fabric> = mapOf(),
    @Required
    val overrides: Map<String, EntryOverride> = mapOf()
) {
    companion object {
        private val logger = KotlinLogging.logger {}
        const val defaultSchema = "./schema/config.schema.json"
        private const val CONFIG_PATH = "config.json5"
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
