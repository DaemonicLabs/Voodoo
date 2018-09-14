package voodoo

import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.nested.NestedPack
import voodoo.util.toJson
import java.io.File

object Importer : KLogging() {
    suspend fun flatten(
        nestedPack: NestedPack,
        targetFolder: File,
        name: String = nestedPack.id.replace("[^\\w-]+".toRegex(), ""),
        targetFileName: String = "$name.pack.hjson"
    ) {
        targetFolder.walkTopDown().asSequence()
            .filter {
                it.isFile && it.name.endsWith(".entry.hjson")
            }
            .forEach {
                it.delete()
            }
        val modpack = nestedPack.flatten()
        modpack.entrySet += nestedPack.root.flatten(File("parentFile"))

        modpack.writeEntries(targetFolder)
        val packFile = targetFolder.resolve(targetFileName)

        packFile.writeText(modpack.toJson)
    }

    suspend fun flatten(
        nestedPack: NestedPack,
        targetFolder: File
    ): ModPack {
        val modpack = nestedPack.flatten()
        modpack.entrySet += nestedPack.root.flatten(File("parentFile"))
        val srcDir = targetFolder.resolve(modpack.sourceDir)
        modpack.entrySet.forEach { entry ->
            entry.setDefaultFile(srcDir)
        }
        return modpack
    }
}