package voodoo.poet

import group
import id
import list
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import rootEntry
import voodoo.NewModpack
import voodoo.data.Side
import voodoo.data.nested.NestedPack
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
            NestedPack(
                rootFolder,
                id = "new-pack",
                mcVersion = "1.12.2",
                authors = listOf("blarb something", "nikky"),
                root = rootEntry(CurseProvider) {
                    validMcVersions = setOf("1.12.1", "1.12")
                    list {
                        id(Mod.wearableBackpacks)
                        id(Mod.neat)

                        group {
                            side = Side.SERVER
                        }.list {
                            id(Mod.btfuContinuousRsyncIncrementalBackup)
                        }
                    }
                }
            )
        }

        it("generate kotlin source") {
            NewModpack.createModpack(
                rootFolder,
                nestedpack
            )
        }
    }
})