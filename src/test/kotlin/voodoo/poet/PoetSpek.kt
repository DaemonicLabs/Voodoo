package voodoo.poet

import Mod
import list
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.script.MainScriptEnv
import voodoo.PoetPack
import voodoo.data.Side
import voodoo.provider.CurseProvider
import java.io.File

object PoetSpek : Spek({
    describe("create nested pack") {
        val rootFolder by memoized {
            File("run").resolve("test").resolve("poet").absoluteFile.apply {
                //                deleteRecursively()
                mkdirs()
            }
        }
        val nestedpack by memoized {
            MainScriptEnv(rootDir = rootFolder).nestedPack(
                id = "new-pack",
                mcVersion = "1.12.2"
            ) {
                authors = listOf("blarb something", "nikky")
                root = rootEntry(CurseProvider) {
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

        it("generate kotlin source") {
            PoetPack.createModpack(
                rootFolder,
                nestedpack
            )
        }
    }
})