package voodoo.provider

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.Builder
import voodoo.builder.resolve
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.util.Directories
import voodoo.util.ExceptionHelper
import java.io.File

object CurseSpek : Spek({
    describe("Flat Entry") {
        val directories by memoized { Directories.get() }
        val cacheDir by memoized { directories.cacheHome }

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
                addOrMerge(entry = Entry(
                        provider = Provider.CURSE.name,
                        id = "matterlink",
                        folder = "mods"
                )) { _, new -> new }
                writeEntries(rootFolder)
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
                    Provider.CURSE.base.reset()
                    modpack.resolve(this, rootFolder, updateAll = true)
                }
                modpack.lockEntrySet
            }
            it("validate entries") {
                versionsMapping.forEach { entry ->
                    assert(entry.provider().validate(entry))
                }
            }
            context("download") {
                val filePairs by memoized {
                    val targetFolder = rootFolder.resolve("install")
                    runBlocking {
                        val deferredFiles =
                                versionsMapping.map { entry ->
                                    async {
                                        entry.provider().download(entry, targetFolder, cacheDir)
                                    }
                                }
                        deferredFiles.map { it.await() }
                    }
                }
                it("files were downloaded") {
                    filePairs.forEach { (url, file) ->
                        assert(file.isFile) { "$url - $file did not exist" }
                    }
                }
            }
        }
    }

//    describe("LockEntry") {
//
//    }
})