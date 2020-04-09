package voodoo.builder

import com.eyeem.watchadoin.Stopwatch
import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.nested.NestedPack

object Importer : KLogging() {

    suspend fun flatten(
        stopwatch: Stopwatch,
        nestedPack: NestedPack
    ): ModPack = stopwatch {
        val targetFolder = nestedPack.rootFolder
        targetFolder.walkTopDown().asSequence()
            .filter {
                it.isFile && it.name.endsWith(".entry.json")
            }
            .forEach {
                it.delete()
            }
        val modpack = nestedPack.flatten() //TODO: add stopwatch more levels down
//        modpack.entrySet += nestedPack.root.flatten()
        modpack
    }
}
//    @Deprecated("looks suspect")
//    suspend fun flatten(
//        nestedPack: NestedPack,
//        name: String = nestedPack.id.replace("[^\\w-]+".toRegex(), ""),
//        targetFileName: String = "$name.pack.json"
//    ) {
//        nestedPack.rootDir.walkTopDown().asSequence()
//            .filter {
//                it.isFile && it.name.endsWith(".entry.json")
//            }
//            .forEach {
//                it.delete()
//            }
//        val modpack = nestedPack.flatten()
//        modpack.entrySet += nestedPack.root.flatten(File("parentFile"))
//
//        // TODO: why write Entry and not LockEntry ?
//        modpack.writeEntries()
//        val packFile = nestedPack.sourceFolder.resolve(targetFileName)
//
//        packFile.writeText(modpack.toJson(ModPack.serializer()))
//    }
