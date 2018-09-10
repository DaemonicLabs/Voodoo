package voodoo.provider

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.Builder
import voodoo.builder.resolve
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.exceptionHandler
import voodoo.util.Directories
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
                )) { old, new -> new }
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
                runBlocking(context = exceptionHandler) {
                    Provider.CURSE.base.reset()
                    modpack.resolve(rootFolder, Builder.jankson, updateAll = true)
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
                    val deferredFiles =
                            versionsMapping.map { entry ->
                                async(context = exceptionHandler) {
                                    entry.provider().download(entry, targetFolder, cacheDir)
                                }
                            }
                    runBlocking(context = exceptionHandler) { deferredFiles.map { it.await() } }
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