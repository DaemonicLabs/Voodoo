package voodoo.pack

import blue.endless.jankson.Jankson
import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging
import voodoo.config.Autocompletions
import voodoo.config.Configuration
import voodoo.data.ModloaderPattern
import voodoo.data.PackOptions
import voodoo.data.flat.FlatModPack
import voodoo.util.json
import java.io.File
import java.net.URI

@Serializable
data class VersionPack(
    @Required
    @SerialName("\$schema")
    @JsonSchema.NoDefinition
    val schema: String = defaultSchema,
    val title: String? = null,
    val icon: String? = null,
    val authors: List<String> = listOf(),
    val version: String,
    var srcDir: String = "src",
    val mcVersion: String,
    val modloader: Modloader,
    val packageConfiguration: VersionPackageConfig = VersionPackageConfig(),
    @JsonSchema.Definition("FileEntryList")
    val mods: Map<String, List<JsonElement>>,
//    @JsonSchema.Definition("FileEntryList")
//    val mods: List<JsonElement>,
) {
    @Transient
    lateinit var baseDir: File

    @Transient
    lateinit var modEntries: List<FileEntry>

    companion object {
        private val logger = KotlinLogging.logger {}
        const val extension = "voodoo.json5"
        const val defaultSchema = "../schema/versionPack.schema.json"

        fun parseEntry(jsonElement: JsonElement): FileEntry {
            return when (jsonElement) {
                is JsonPrimitive -> {
                    require(jsonElement.isString) { "element $jsonElement is not a string" }
                    val str = jsonElement.content
                    val overrides =
                        str.substringBefore("=").takeIf { it.contains(":") }?.substringAfter(":")?.split(",")
                            ?: listOf()
                    when {
                        str.startsWith("curse") -> {
                            FileEntry.Curse(
                                applyOverrides = overrides,
                                curse_projectName = str.substringAfter(":").substringAfter("=")
                            )
                        }
                        str.startsWith("jenkins:") -> {
                            FileEntry.Jenkins(
                                applyOverrides = overrides,
                                jenkins_job = str.substringAfter(":").substringAfter("=")
                            )
                        }
                        str.startsWith("direct:") -> {
                            FileEntry.Direct(
                                applyOverrides = overrides,
                                direct_url = str.substringAfter(":").substringAfter("=")
                            )
                        }
                        else -> error("unknown prefix: ${str.substringBefore(":")}")
                    }
                }
                is JsonObject -> {
                    json.decodeFromJsonElement(FileEntry.serializer(), jsonElement)
                }
                else -> {
                    error("element $jsonElement needs to be a object or string")
                }
            }
        }

        fun parse(packFile: File): VersionPack {
            val cleanedString = Jankson
                .builder()
                .build()
                .load(packFile.readText()).let { jsonObject ->
                    jsonObject.toJson(false, true);
                }

            return json.decodeFromString(
                VersionPack.serializer(),
                cleanedString
            ).postParse(packFile.absoluteFile.parentFile)
        }

        fun parseAll(baseDir: File): List<VersionPack> {
            return baseDir
                .list { dir, name ->
                    name.endsWith(".$extension")
                }!!
                .map { versionPackFilename ->
                    val versionPackFile = baseDir.resolve(versionPackFilename)

                    parse(versionPackFile)
                }
        }


    }

    fun postParse(baseDir: File): VersionPack {
        return this.run {
            copy(
                modloader = modloader.replaceAutoCompletes(),
            ).apply {
                modEntries = mods.flatMap { (overrideKey, modsList) ->
                    modsList.map { element ->
                        parseEntry(element)
                    }.map { entry ->
                        entry.postParse(overrideKey)
                    }
                }
            }
        }.apply {
            this.baseDir = baseDir
        }
    }

    fun flatten(rootDir: File, id: String, metaPack: MetaPack, overrides: Map<String, EntryOverride>): FlatModPack {
        return FlatModPack(
            rootFolder = rootDir,
            id = id,
            mcVersion = mcVersion,
            title = title,
            version = version,
            srcDir = srcDir,
            icon = icon ?: metaPack.icon,
            authors = authors,
            modloader = when (modloader) {
                is Modloader.Forge -> ModloaderPattern.Forge(
                    version = modloader.version
                )
                is Modloader.Fabric -> ModloaderPattern.Fabric(
                    intermediateMappingsVersion = modloader.intermediateMappings,
                    loaderVersion = modloader.loader,
                    installerVersion = modloader.installer
                )
                is Modloader.None -> ModloaderPattern.None
            },
            packOptions = PackOptions(
                uploadUrl = URI(metaPack.uploadBaseUrl).resolve(id).toASCIIString(),
                multimcOptions = PackOptions.MultiMC(
                    relativeSelfupdateUrl = packageConfiguration.voodoo.relativeSelfupdateUrl,
                    instanceCfg = packageConfiguration.multimc.instanceCfg
                )
            ),
            entrySet = modEntries.map { intitalEntry ->
                intitalEntry.toEntry(overrides)
            }.toMutableSet()
        ).apply {

            require(rootFolder.isAbsolute) { "rootFolder: '$rootFolder' is not absolute" }
            rootFolder.resolve(id).walkTopDown().asSequence()
                .filter {
                    it.isFile && it.name.endsWith(".entry.json")
                }
                .forEach {
                    it.delete()
                }
        }
    }

}
