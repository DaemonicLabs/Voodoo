package voodoo.builder

import com.eyeem.watchadoin.Stopwatch
import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.nested.NestedPack
import java.io.File

object Importer : KLogging() {

    suspend fun flatten(
        stopwatch: Stopwatch,
        nestedPack: NestedPack,
        id: String,
        targetFolder: File
    ): ModPack = stopwatch {
        targetFolder.walkTopDown().asSequence()
            .filter {
                it.isFile && it.name.endsWith(".entry.json")
            }
            .forEach {
                it.delete()
            }
        val modpack = nestedPack.flatten(rootFolder = targetFolder, id = id) //TODO: add stopwatch more levels down
//        modpack.entrySet += nestedPack.root.flatten()
        modpack
    }
}
