package voodoo.builder

import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.nested.NestedPack
import voodoo.util.toJson
import java.io.File

object Importer : KLogging() {
    suspend fun flatten(
        nestedPack: NestedPack,
        name: String = nestedPack.id.replace("[^\\w-]+".toRegex(), ""),
        targetFileName: String = "$name.pack.hjson"
    ) {
        nestedPack.rootDir.walkTopDown().asSequence()
            .filter {
                it.isFile && it.name.endsWith(".entry.hjson")
            }
            .forEach {
                it.delete()
            }
        val modpack = nestedPack.flatten()
        modpack.entrySet += nestedPack.root.flatten(File("parentFile"))

        modpack.writeEntries()
        val packFile = nestedPack.sourceFolder.resolve(targetFileName)

        packFile.writeText(modpack.toJson(ModPack.serializer()))
    }

    suspend fun flatten(
        nestedPack: NestedPack
    ): ModPack {
        val targetFolder = nestedPack.rootDir
        targetFolder.walkTopDown().asSequence()
            .filter {
                it.isFile && it.name.endsWith(".entry.hjson")
            }
            .forEach {
                it.delete()
            }
        val modpack = nestedPack.flatten()
        modpack.entrySet += nestedPack.root.flatten()
        return modpack
    }
}