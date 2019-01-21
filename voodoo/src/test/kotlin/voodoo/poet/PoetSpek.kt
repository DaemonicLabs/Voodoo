package voodoo.poet

import Mod
import list
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.PoetPack
import voodoo.data.Side
import voodoo.provider.CurseProvider
import voodoo.script.MainScriptEnv
import java.io.File

object PoetSpek : Spek({
    describe("create nested pack") {
        val rootFolder by memoized {
            File("run").resolve("test").resolve("poet").absoluteFile.apply {
                deleteRecursively()
                mkdirs()
            }
        }
        val scriptEnv by memoized {
            MainScriptEnv(rootDir = rootFolder, id = "new-pack").apply {
                mcVersion = "1.12.2"
                authors = listOf("blarb something", "nikky")
                root(CurseProvider) {
                    validMcVersions = setOf("1.12.1", "1.12")
                    list {
                        +(Mod.wearableBackpacks)
                        +(Mod.neat)

                        group {
                            side = Side.SERVER
                        }.list {
                            +(Mod.btfuContinuousRsyncIncrementalBackup)
                        }
                    }
                }
            }
        }

        val nestedPack by memoized {
            scriptEnv.pack
        }

        it("generate kotlin source") {
            PoetPack.createModpack(
                rootFolder,
                nestedPack
            )
        }

        // TODO: compile generated file
    }
})