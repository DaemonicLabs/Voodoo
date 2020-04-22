package voodoo.importer

import kotlinx.coroutines.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.curse.CurseClient
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
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
                val downloadURL = CurseClient.getAddonFile(ProjectID(290913), FileID(2654545))!!.downloadUrl
                CurseImporter.import(
                    "stoneblock",
                    downloadURL, //"https://www.curseforge.com/minecraft/modpacks/stoneblock/download/2654545/file",
                    rootFolder,
                    rootFolder.resolve("packs")
                )
            }
        }
    }
})