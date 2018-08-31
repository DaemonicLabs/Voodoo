package voodoo

import blue.endless.jankson.JsonObject
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.data.flat.Entry
import voodoo.data.nested.NestedPack
import voodoo.importer.YamlImporter
import voodoo.provider.Provider
import voodoo.util.yamlMapper
import java.io.File
import kotlin.test.assertEquals

object ImportYamlSpek : Spek({
    describe("A YAML Pack") {
        val rootFolder by memoized {
            File("run").resolve("test").resolve("yaml").absoluteFile.apply {
                deleteRecursively()
                mkdirs()
            }
        }

        val id by memoized { "testpack" }
        val title by memoized { "Some Title" }

        val mainFile by memoized {
            rootFolder.resolve("$id.yaml")
        }
        val includeFile by memoized {
            rootFolder.resolve("include.yaml")
        }

        val yaml by memoized {
            """
                title: $title
                id: $id
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
                  - include: ${includeFile.relativeTo(rootFolder)}
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

        val includeYaml by memoized {
            """
                validMcVersions:
                - 1.12.2
                - 1.12.1
                curseOptionalDependencies: false
                curseReleaseTypes: [beta, release]
                provider: CURSE
                side: SERVER
                entries:
                - matterlink
                - swingthroughgrass
                """.trimIndent()
        }


        beforeEachTest {
            mainFile.writeText(yaml)
            includeFile.writeText(includeYaml)
        }

        context("main yaml file") {
            it("exists") {
                assert(mainFile.isFile)
            }
        }

        context("include yaml file") {
            it("exists") {
                assert(includeFile.isFile)
            }
        }

        context("importing yaml") {
            val lockFile by memoized {
                runBlocking { YamlImporter.import(source = mainFile.path, target = rootFolder) }
                rootFolder.resolve("$id.pack.hjson")
            }
            it("lockfile exists") {
                assert(lockFile.isFile) { "cannot find $lockFile" }
            }
        }

        context("parse yaml") {
            val nestedPack by memoized {
                yamlMapper.readValue<NestedPack>(yaml)
            }

            it("id matches") {
                assertEquals(id, nestedPack.id)
            }
            it("title matches") {
                assertEquals(title, nestedPack.title)
            }

            context("flatten") {
                val entries by memoized {
                    runBlocking {
                        nestedPack.root.flatten(rootFolder)
                    }
                }


                it("entries is not empty") {
                    assert(entries.isNotEmpty())
                }

                context("writing imported files") {
                    val srcDir by memoized { rootFolder.resolve(nestedPack.sourceDir) }
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
                            targetFile
                        }
                    }

                    it("file is not empty") {
                        // require to run the code for files
                        assert(files.isNotEmpty())
                    }

                    it("testing isFile") {
                        files.forEach { assert(it.isFile) { "$it isFile ?" } }
                    }
                    context("entry tests") {
                        //TODO: move into tests for parsing/validating flat entrries
                        val entries2 by memoized {
                            files.map { file ->
                                YamlImporter.jankson.fromJson<Entry>(file)
                            }
                        }
                        it("entry id is valid") {
                            entries2.forEach { entry ->
                                assert(entry.id.isNotBlank()) { "'${entry.id}' is not valid" }
                            }
                        }

                        context("provider tests") {
                            val providers by memoized {
                                entries2.map { entry ->
                                    Provider.valueOf(entry.provider).base to entry
                                }
                            }

                            it("entry id is valid") {
                                providers.forEach { (provider, entry) ->
                                    assert(provider.name.isNotBlank()) { "'${provider.name}' is not valid" }
                                }
                            }
                        }
                    }
                }
            }
        }


    }
})
