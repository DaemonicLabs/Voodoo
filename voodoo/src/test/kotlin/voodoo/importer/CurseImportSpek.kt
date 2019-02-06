package voodoo.importer

import kotlinx.coroutines.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.poet.importer.CurseImporter
import java.io.File

object CurseImportSpek : Spek({
    describe("importer curse pack") {
        val rootFolder by memoized {
            File("run").resolve("test").resolve("importCurse").absoluteFile.apply {
                deleteRecursively()
                mkdirs()
            }
        }

        it("importer") {
            runBlocking {
                CurseImporter.import(
                    "stoneblock",
                    "https://minecraft.curseforge.com/projects/stoneblock/files/2654545/download",
                    rootFolder,
                    rootFolder.resolve("packs")
                )
            }
        }
    }
})