package voodoo

import Forge
import Mod
import job
import kotlinx.coroutines.runBlocking
import list
import metaUrl
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import releaseTypes
import voodoo.data.Side
import voodoo.data.curse.FileType
import voodoo.provider.CurseProvider
import voodoo.provider.JenkinsProvider
import voodoo.script.MainScriptEnv
import withProvider
import java.io.File

object DslSpek : Spek({
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
                forge = Forge.recommended
                root(CurseProvider) {
                    releaseTypes = setOf(FileType.RELEASE, FileType.BETA)

                    // TODO: use type URL ?
                    metaUrl = "https://curse.nikky.moe/api"
                    list {
                        +(Mod.botania)
                        +(Mod.rftools)

                        withProvider(JenkinsProvider) {
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
            nestedPack.flatten()
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
