package voodoo.provider

import kotlinx.coroutines.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.Builder
import voodoo.builder.resolve
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import java.io.File

object CurseSpek : Spek({
    describe("Flat Entry") {
        val rootFolder by memoized {
            File("run").resolve("test").resolve("curse").apply {
                deleteRecursively()
                mkdirs()
            }
        }

        val modpack by memoized {
            ModPack(
                    id = "curse_spek",
                    title = "Curse Spek",
                    mcVersion = "1.12.2"
            ).apply {
                entriesSet += Entry(
                        provider = Provider.CURSE.name,
                        id = "rftools-dimensions",
                        folder = "mods"
                )
                writeEntries(rootFolder, Builder.jankson)
            }
        }

//        beforeEachTest {
//            context("write mod entries") {
//
//            }
//
//        }

        context("build pack") {
            val versionsMapping by memoized {
                runBlocking {
                    modpack.resolve(rootFolder, Builder.jankson, updateAll = true)
                }
                modpack.lockEntrySet
            }
            it("validate entries") {
                versionsMapping.forEach { entry ->
                    assert(entry.provider().validate(entry))
                }
            }
        }
    }

//    describe("LockEntry") {
//
//    }
})