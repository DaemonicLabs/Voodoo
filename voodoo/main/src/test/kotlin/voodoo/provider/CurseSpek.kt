package voodoo.provider

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import list
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.builder.resolve
import voodoo.data.nested.NestedEntry
import voodoo.script.MainScriptEnv
import voodoo.util.Directories
import java.io.File

object CurseSpek : Spek({
    describe("Flat Entry") {
        val directories by memoized { Directories.get() }
        val cacheDir by memoized { directories.cacheHome }

        val rootFolder by memoized {
            File("run").resolve("test").resolve("curse").absoluteFile.apply {
                deleteRecursively()
                mkdirs()
            }
        }

        val scriptEnv by memoized {
            MainScriptEnv(rootFolder = rootFolder, id = "curse_spek").apply {
                mcVersion = "1.12.2"
                title = "Curse Spek"
                root<NestedEntry.Curse> { builder ->
                    builder.list {
                        +(Mod.matterlink)
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

        context("build pack") {
            val versionsMapping by memoized {
                runBlocking {
                    CurseProvider.reset()
                    resolve(Stopwatch("resolve"), modpack)
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
                                    entry.provider().download(Stopwatch("download"), entry, targetFolder, cacheDir)
                                }
                            }
                        deferredFiles.awaitAll().filterNotNull()
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
