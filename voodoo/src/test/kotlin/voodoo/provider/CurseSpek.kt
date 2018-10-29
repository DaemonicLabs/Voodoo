package voodoo.provider

import Mod
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import list
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.builder.resolve
import voodoo.script.MainScriptEnv
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

        val nestedpack by memoized {
            MainScriptEnv(rootDir = rootFolder).nestedPack(
                id = "curse_spek",
                mcVersion = "1.12.2"
            ) {
                title = "Curse Spek"
                root = rootEntry(CurseProvider) {
                    list {
                        +(Mod.matterlink)
                    }
                }
            }
        }

        val modpack by memoized {
            nestedpack.flatten()
        }

        context("build pack") {
            val versionsMapping by memoized {
                runBlocking {
                    CurseProvider.reset()
                    resolve(modpack)
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
