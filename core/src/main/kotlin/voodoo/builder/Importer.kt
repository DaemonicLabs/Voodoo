package voodoo.builder

import com.eyeem.watchadoin.Stopwatch
import mu.KLogging
import voodoo.data.flat.FlatModPack
import voodoo.data.nested.NestedPack
import java.io.File

object Importer : KLogging() {

    suspend fun flatten(
        stopwatch: Stopwatch,
        nestedPack: NestedPack,
        id: String,
        rootFolder: File
    ): FlatModPack = stopwatch {
        require(rootFolder.isAbsolute) { "rootFolder: '$rootFolder' is not absolute" }
        rootFolder.resolve(id).walkTopDown().asSequence()
            .filter {
                it.isFile && it.name.endsWith(".entry.json")
            }
            .forEach {
                it.delete()
            }
        val modpack = nestedPack.flatten(rootFolder = rootFolder, id = id) //TODO: add stopwatch more levels down
//        modpack.entrySet += nestedPack.root.flatten()
        modpack
    }
}
