package voodoo.pack

import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import voodoo.config.Autocompletions
import voodoo.data.ModloaderPattern
import voodoo.data.PackOptions
import voodoo.data.curse.ProjectID
import voodoo.data.flat.FlatModPack
import voodoo.util.json
import java.io.File
import java.net.URI

@Serializable
data class VersionPack(
    @Required
    @SerialName("\$schema")
    @JsonSchema.NoDefinition
    val schema: String = "../schema/versionPack.schema.json",
    val title: String? = null,
    val icon: String? = null,
    val authors: List<String> = listOf(),
    val version: String,
    var srcDir: String = "src",
    val mcVersion: String,
    val modloader: Modloader,
    val packageConfiguration: VersionPackageConfig = VersionPackageConfig(),
//    val overrides: Map<String, EntryOverride> = mapOf(),
    val mods: List<FileEntry>,
) {
    companion object {
        const val extension = "voodoo.json"
        const val defaultSchema = "../schema/versionPack.schema.json"

        fun parse(packFile: File) : VersionPack {
            return json.decodeFromString(
                VersionPack.serializer(),
                packFile.readText()
            ).run {
                copy(
                    modloader = modloader.replaceAutoCompletes(),
                    mods = mods.map { entry ->
                        transformFileEntry(entry)
                    }
                )
            }
        }
        fun parseAll(baseDir: File) : List<VersionPack> {
            return baseDir
                .list { dir, name ->
                    name.endsWith(".$extension")
                }!!
                .map { versionPackFilename ->
                    val versionPackFile = baseDir.resolve(versionPackFilename)

                    parse(versionPackFile)
                }
        }

        private fun transformFileEntry(entry: FileEntry): FileEntry = when (entry) {
            is FileEntry.Curse -> {
                if (entry.projectName != null) {
                    val addonid = Autocompletions.curseforge[entry.projectName]?.toIntOrNull()
                    val newName = entry.projectName.substringAfterLast('/')
                    require(addonid != null) { "cannot find replacement for ${entry.projectName} / ${Autocompletions.curseforge[entry.projectName]}" }
                    entry.copy(
                        projectName = null,
                        curse = entry.curse.copy(
                            projectID = ProjectID(addonid)
                        )
                    ).apply {
                        name = entry.name ?: newName
                        id = entry.id ?: newName
                    }
                } else {
                    entry
                }
            }
            else -> entry
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
            entrySet = mods.map { intitalEntry ->
                val entryId = intitalEntry.id
                val entry = intitalEntry.applyOverrides.fold(intitalEntry) { acc, overrideId ->
                    val entryOverride =
                        overrides[overrideId] ?: error("$entryId: override for id $overrideId not found")
                    return@fold acc.applyTag(entryOverride)
                }
                entry.toEntry()
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
