package voodoo.provider

import blue.endless.jankson.JsonObject
import com.sun.org.apache.xpath.internal.operations.Mod
import kotlinx.coroutines.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.Builder
import voodoo.builder.resolve
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
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
            )
        }

        beforeEachTest {
            context("write mod entries") {
                listOf(
                        Entry(
                                provider = Provider.CURSE.name,
                                id = "rftools-dimensions"
                        ) to "mods"
                ).apply {
                    for ((entry, path) in this) {
                        val jankson = Builder.jankson
                        val folder = rootFolder.resolve(modpack.sourceDir).resolve(path)
                        folder.mkdirs()
                        val filename = entry.id
                                .replace('/', '-')
                                .replace("[^\\w-]+".toRegex(), "")
                        val targetFile = folder.resolve("$filename.entry.hjson")
                        val json = jankson.marshaller.serialize(entry)//.toJson(true, true)
                        if (json is JsonObject) {
                            val defaultJson = entry.toDefaultJson(jankson.marshaller)
                            val delta = json.getDelta(defaultJson)
                            targetFile.writeText(delta.toJson(true, true).replace("\t", "  "))
                        }
                    }
                }
            }

        }

        context("build pack") {
            val versionsMapping by memoized {
                runBlocking {
                    modpack.resolve(rootFolder, Builder.jankson, updateAll = true)
                }
                modpack.versionsMapping
            }
            it("validate entries") {
                versionsMapping.forEach { name, (entry, file) ->
                    assert(entry.provider().validate(entry))
                }
            }
        }
    }

//    describe("LockEntry") {
//
//    }
})