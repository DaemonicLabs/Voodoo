package voodoo.pack

import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.FnPatternList
import voodoo.data.ModloaderPattern
import voodoo.data.PackOptions
import voodoo.data.flat.ModPack
import java.io.File

@Serializable
data class ModpackInput(
    @JsonSchema.NoDefinition
    @SerialName("\$schema")
    val schema: String = "./schema/modpack.schema.json",
    val title: String,
    val authors: List<String> = listOf(),
    val version: String,
    val icon: String,
    val mcVersion: String,
    val modloader: Modloader,
    @JsonSchema.Description(["url pointing to \$modpackId.package.json", "you need to upload the packaged modpack there"])
    val selfupdateUrl: String,
    var userFiles: FnPatternList = FnPatternList(),
//    val overrides: Map<String, EntryOverride> = mapOf(),
    val mods: Map<String, EntryInput>
) {
    fun flatten(rootDir: File, id: String, overrides: Map<String, EntryOverride>): ModPack {
        return ModPack(
            rootFolder = rootDir,
            id = id,
            mcVersion = mcVersion,
            title = title,
            version = version,
            icon = rootDir.resolve(icon),
            authors = authors,
            modloader = when(modloader) {
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
                multimcOptions = PackOptions.MultiMC(
                    selfupdateUrl = selfupdateUrl,
//                    instanceCfg =
                ),
                userFiles = userFiles
            ),
            entrySet = mods.map { (entryId, intitalEntry) ->
                val entry = intitalEntry.applyOverrides.fold(intitalEntry) { acc, overrideId ->
                    val entryOverride = overrides[overrideId] ?: error("$entryId: override for id $overrideId not found")
                    return@fold acc.applyTag(entryOverride)
                }
                entry.toEntry(entryId)
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
