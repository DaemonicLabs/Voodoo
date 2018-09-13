package voodoo.importer


import kotlinx.serialization.SerialContext
import kotlinx.serialization.UpdateMode
import kotlinx.serialization.json.JSON
import voodoo.data.flat.ModPack
import voodoo.data.nested.NestedPack
import voodoo.util.readYaml
import java.io.File

/**
 * Created by nikky on 01/07/18.
 * @author Nikky
 */

object YamlImporter : AbstractImporter() {
    override val label = "Yaml Importer"

    override suspend fun import(source: String, target: File, name: String?) {
        logger.info(source)
        val yamlFile = File(source).absoluteFile
        logger.info("reading: $yamlFile")
        val nestedPack = yamlFile.readYaml<NestedPack>()

        val modpack = nestedPack.flatten()
        modpack.entrySet += nestedPack.root.flatten(yamlFile.parentFile)

        val srcDir = target.resolve(nestedPack.sourceDir)
//        val modsDir = srcDir.resolve("mods")

//        modsDir.mkdirs()
        modpack.writeEntries(target)

        val filename =  name ?: nestedPack.id.replace("[^\\w-]+".toRegex(), "")
        val packFile = target.resolve("$filename.pack.hjson")

        packFile.writeText(JSON.unquoted.stringify(modpack))

        run {
            val json = JSON(indented = true, context = SerialContext().apply {
//                registerSerializer(ModPack::class, ModPack.Companion)
            }, updateMode = UpdateMode.BANNED, nonstrict = true, unquoted = true, indent = "  ")
            val jsonified = json.stringify(modpack)
            println(jsonified)
            val reloaded = json.parse<ModPack>(jsonified)
            println(reloaded)
        }
    }

}