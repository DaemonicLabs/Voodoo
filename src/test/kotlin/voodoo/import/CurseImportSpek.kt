package voodoo.import

import kotlinx.coroutines.experimental.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.importer.CurseImporter
import java.io.File

object CurseImportSpek : Spek({
    describe("import curse pack") {
        val rootFolder by memoized {
            File("run").resolve("test").resolve("importCurse").absoluteFile.apply {
                deleteRecursively()
                mkdirs()
            }
        }

        it("import") {
            runBlocking {
                CurseImporter.import(
                    "stoneblock",
                    "https://minecraft.curseforge.com/projects/stoneblock/files/2626239/download",
                    rootFolder,
                    rootFolder.resolve("packs")
                )
            }
        }
    }
})