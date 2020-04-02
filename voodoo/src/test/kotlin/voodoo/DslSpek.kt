package voodoo

import Forge
import job
import kotlinx.coroutines.runBlocking
import list
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.data.Side
import voodoo.data.curse.FileType
import voodoo.data.nested.NestedEntry
import voodoo.script.MainScriptEnv
import voodoo.util.SharedFolders
import java.io.File

object DslSpek : Spek({
    SharedFolders.RootDir.value = File(".")
    describe("load pack") {
        val rootFolder by memoized {
            File("run").resolve("test").resolve("dslspek").absoluteFile.apply {
                deleteRecursively()
                mkdirs()
            }
        }

//        beforeEachTest {
//            fileForResource("/voodoo/dslSpek").copyRecursively(rootFolder)
//        }

        val scriptEnv by memoized {
            MainScriptEnv(rootDir = rootFolder, id = "some-id").apply {
                mcVersion = "1.12.2"
                version = "1.0"
                // TODO: type = File
                icon = File("icon.png")
                authors = listOf("dude", "and", "friends")
                // TODO: type = {recommended, latest} | buildnumber, make sealed class
                modloader {
                    forge(version = Forge.mc1_12_2_recommended)
                }
                root<NestedEntry.Curse> {
                    releaseTypes = setOf(FileType.Release, FileType.Beta)

                    // TODO: use type URL ?
                    it.list {
                        +(Mod.botania)
                        +(Mod.rftools)

                        withType<NestedEntry.Jenkins> {
                            side = Side.SERVER
                        }.list {
                            +"matterlink" job "elytra/matterlink/master"
                        }

//                    include("other.kts")
                    }
                }
            }
        }

        val nestedPack by memoized {
            scriptEnv.pack
        }
        val modpack by memoized {
            runBlocking {
                nestedPack.flatten()
            }
        }
        val entries by memoized {
            runBlocking {
                nestedPack.root.flatten(File(""))
            }
        }
        it("simple test") {
            println(nestedPack)
            for (entry in entries) {
                println(entry)
            }
            println(modpack)
        }
    }
})
