package voodoo

import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.json.JSON
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.builder.resolve
import voodoo.data.flat.ModPack
import voodoo.importer.YamlImporter
import voodoo.util.ExceptionHelper
import java.io.*

object BuildSpek : Spek({
    describe("import YAML") {
        val rootFolder by memoized {
            File("run").resolve("test").resolve("build").absoluteFile.apply {
                deleteRecursively()
                mkdirs()
            }
        }

        beforeEachTest {
            fileForResource("/voodoo/buildSpek").copyRecursively(rootFolder)
        }

        val mainFile by memoized {
            rootFolder.resolve("testpack.yaml")
        }
        val includeFiles by memoized {
            listOf("include_features.yaml", "include_server.yaml").map {rootFolder.resolve(it)}
        }

        it("main yaml exists") {
            assert(mainFile.isFile)
        }
        it("include yaml file exists") {
            includeFiles.forEach {
                assert(it.isFile) { "$it does not exist" }
            }
        }

        val packFile by memoized {
            runBlocking(context = ExceptionHelper.context) { YamlImporter.import(source = mainFile.path, target = rootFolder, name = "lockpack") }
            rootFolder.resolve("lockpack.pack.hjson")
        }

        it("packFile exists") {
            assert(packFile.isFile) { "cannot find $packFile" }
        }

        context("loading pack") {
            val modpack by memoized {
                JSON.unquoted.parse<ModPack>(packFile.readText())
            }
            val entries by memoized {
                modpack.loadEntries(rootFolder)
                modpack.entrySet
            }

            it("entries are valid") {
                entries.forEach { entry ->
                    assert(entry.id.isNotBlank()) {"id of $entry is blank"}
                }
            }
            context ("building pack") {
                val lockEntries by memoized {
                    runBlocking(context = ExceptionHelper.context) {
                        modpack.resolve(
                                rootFolder,
                                updateAll = true,
                                updateDependencies = true
                        )
                    }
                    modpack.lockEntrySet
                }

                it("validate lockentries") {
                    println("start test lockentries")
                    lockEntries.forEach { entry ->
                        assert(entry.provider().validate(entry)) { "$entry failed validation" }
                    }
                }
            }
        }

    }
})

