package voodoo

import blue.endless.jankson.JsonObject
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.data.flat.Entry
import voodoo.data.nested.NestedPack
import voodoo.importer.YamlImporter
import voodoo.provider.Provider
import voodoo.util.yamlMapper
import java.io.File
import kotlin.test.assertEquals

object YamlSpek : Spek({
    describe("A YAML Pack") {
        val runDir by memoized {
            File("run").resolve("test").absoluteFile.apply {
                println("init folder")
                deleteRecursively()
                mkdirs()
            }
        }
        //TODO: write yaml to file
        //TODO: add tes for includes
        val yaml by memoized {
            """
                title: Minimal Feature Test
                id: features
                mcVersion: 1.12.2
                forge: 2739
                authors:
                - NikkyAi
                version: 1.0
                sourceDir: src
                root:
                  validMcVersions:
                  - 1.12.2
                  - 1.12.1
                  curseOptionalDependencies: false
                  curseReleaseTypes: [beta, release]
                  provider: CURSE
                  entries:
                  - feature:
                      selected: true
                      recommendation: starred
                    entries:
                    - id: FoamFix
                      provider: DIRECT
                      url: https://asie.pl/files/mods/FoamFix/foamfix-0.9.9.1-1.12.2-anarchy.jar
                      description: "Foamfix (Anarchy)!"
                      name: FoamFix

                  - side: CLIENT
                    entries:
                    - betterfps
                    - feature:
                        recommendation: starred
                        selected: true
                      entries:
                      - id: toast-control
                        description: "Removes Minecraft annoying Toasts."
                        name: Toast Control
                      - id: journeymap
                        description: "Minimap"
                    - feature:
                        selected: false
                      entries:
                      - id: worldeditcui-forge-edition-2
                        description: "If you don't know what this is, I suggest not ticking it."
                """.trimIndent()
        }

        val nestedPack by memoized {
            yamlMapper.readValue<NestedPack>(yaml)
        }

        context("parsed yaml") {
            it("id matches") {
                assertEquals("features", nestedPack.id)
            }
            it("title matches") {
                assertEquals("Minimal Feature Test", nestedPack.title)
            }
        }

        context("flatten") {
            val entries by memoized {
                runBlocking {
                    nestedPack.root.flatten(runDir)
                }
            }

            val srcDir by memoized { runDir.resolve(nestedPack.sourceDir) }

            it("entries is not empty") {
                assert(entries.isNotEmpty())
            }

            context("writing imported files") {
                val files by memoized {
                    entries.map { entry ->
                        entry.validMcVersions += nestedPack.mcVersion
                        val folder = srcDir.resolve(entry.folder)
                        folder.mkdirs()
                        val filename = entry.id
                                .replace('/', '-')
                                .replace("[^\\w-]+".toRegex(), "")
                        val targetFile = folder.resolve("$filename.entry.hjson")
                        val json = YamlImporter.jankson.marshaller.serialize(entry)//.toJson(true, true)
                        if (json is JsonObject) {
                            val defaultJson = entry.toDefaultJson(YamlImporter.jankson.marshaller)
                            val delta = json.getDelta(defaultJson)
                            targetFile.writeText(delta.toJson(true, true).replace("\t", "  "))
                        }
                        println("written to $targetFile")
                        targetFile
                    }
                }

                it("file is not empty") {
                    // require to run the code for files
                    assert(files.isNotEmpty())
                }

                files.forEach {
                    context("testing file $it") {
                        it("$it isFile") {
                            assert(it.isFile)
                        }
                        val entry by memoized { YamlImporter.jankson.fromJson<Entry>(it) }
                        it("entry: ${entry.id} is valid") {
                            assert(entry.id.isNotBlank())
                        }
                        val provider by memoized { Provider.valueOf(entry.provider) }
                        it("entry: '${entry.name}' provider is not blank") {
                            assert(provider.base.name.isNotBlank())
                        }
                    }
                }
            }
        }


    }
})
