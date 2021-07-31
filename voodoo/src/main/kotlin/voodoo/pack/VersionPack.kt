package voodoo.pack

import blue.endless.jankson.Jankson
import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import mu.KotlinLogging
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
    val overrides: Map<String, EntryOverride> = mapOf(),
    @JsonSchema.Definition("FileEntryList")
    val mods: Map<String, List<FileEntry>>,
) {
    @Transient
    lateinit var baseDir: File

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

            val jsonObject = json.decodeFromString(
                JsonObject.serializer(), cleanedString
            )

            val mods: Map<String, List<FileEntry>> = jsonObject["mods"]!!.jsonObject.mapValues { (_, list) ->
                list.jsonArray.map {
                    parseEntry(it)
                }
            }

            val modsObj = json.encodeToJsonElement(
                MapSerializer(String.serializer(), ListSerializer(FileEntry.serializer())),
                mods
            )

            val fixedObject = JsonObject(
                jsonObject.toMap() +
                        ("mods" to modsObj)
            )
            //TODO: process string entries here

            return json.decodeFromJsonElement(
                VersionPack.serializer(),
                fixedObject
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
        return run {
            copy(
//                modloader = modloader.replaceAutoCompletes(),
            )/*.apply {
                modEntries = mods.flatMap { (overrideKey, modsList) ->
                    modsList.map { entry ->
                        entry.postParse(overrideKey)
                    }
                }
            }*/
        }.apply {
            this.baseDir = baseDir
        }
    }

    fun flatten(rootDir: File, id: String, metaPack: MetaPack, configOverrides: Map<String, EntryOverride>): FlatModPack {
        return FlatModPack(
            rootFolder = rootDir,
            id = id,
            mcVersion = mcVersion,
            title = title,
            version = version,
            srcDir = srcDir,
            icon = icon ?: metaPack.icon,
            authors = authors,
            modloader = modloader.replaceAutoCompletes().let { modloader ->
                when (modloader) {
                    is Modloader.Forge -> ModloaderPattern.Forge(
                        version = modloader.version
                    )
                    is Modloader.Fabric -> ModloaderPattern.Fabric(
                        intermediateMappingsVersion = modloader.intermediateMappings,
                        loaderVersion = modloader.loader,
                        installerVersion = modloader.installer
                    )
                    is Modloader.None -> ModloaderPattern.None
                }
            },
            packOptions = PackOptions(
                uploadUrl = URI(metaPack.uploadBaseUrl).resolve(id).toASCIIString(),
                multimcOptions = PackOptions.MultiMC(
                    relativeSelfupdateUrl = packageConfiguration.voodoo.relativeSelfupdateUrl,
                    instanceCfg = packageConfiguration.multimc.instanceCfg
                ),
                userFiles = packageConfiguration.userFiles
            ),
            entrySet = mods.flatMap { (overrideKey, modsList) ->
                modsList.map { entry ->
                    entry.postParse(overrideKey)
                }
            }.map { intitalEntry ->
                intitalEntry.toEntry(configOverrides + overrides)
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
