package voodoo

import kotlinx.coroutines.experimental.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.data.Side
import voodoo.data.curse.FileType
import voodoo.data.nested.NestedPack
import voodoo.provider.CurseProviderThing
import voodoo.provider.JenkinsProviderThing
import java.io.*

object DslSpek : Spek({
    describe("load pack") {
//        val rootFolder by memoized {
//            File("run").resolve("test").resolve("build").absoluteFile.apply {
//                deleteRecursively()
//                mkdirs()
//            }
//        }

//        beforeEachTest {
//            fileForResource("/voodoo/dslSpek").copyRecursively(rootFolder)
//        }

        val nestedPack by memoized {
            NestedPack(
                    id = "some-id",
                    version = "1.0",
                    //TODO: type = File
                    icon = "icon.png",
                    authors = listOf("dude", "and", "friends"),
                    //TODO: type = {recommended, latest} | buildnumber, make sealed class
                    forge = "recommended",
                    root = rootEntry(CurseProviderThing) {
                        optionals = false
                        releaseTypes = setOf(FileType.RELEASE, FileType.BETA)

                        //TODO: use type URL ?
                        metaUrl = "https://curse.nikky.moe"
                        entriesBlock {
                            id("botania") optionals false

                            id("rf-tools") {
                                optionals = false
                            }

                            entry(JenkinsProviderThing) {
                                side = Side.SERVER
                            }.entriesBlock {
                                id("matterlink") job "elytra/matterlink/master"
                                id("elytra/btfu/master")
                            }

//                    include("other.kts")
                        }
                    }
            )
        }

        val modpack by memoized {
            nestedPack.flatten()
        }
        val entries by memoized {
            runBlocking(context = exceptionHandler) {
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

