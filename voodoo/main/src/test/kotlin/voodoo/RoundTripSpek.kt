package voodoo

import Forge
import Mod
import com.eyeem.watchadoin.Stopwatch
import job
import kotlinx.coroutines.runBlocking
import list
import optional
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.builder.Builder
import voodoo.data.Side
import voodoo.data.curse.FileType
import voodoo.data.lock.LockPack
import voodoo.data.nested.NestedEntry
import voodoo.script.MainScriptEnv
import voodoo.util.json
import java.io.File
import kotlin.test.assertEquals

object RoundTripSpek : Spek({
    describe("load pack") {
        val rootFolder by memoized {
            File("run").resolve("test").resolve("roundtrip").absoluteFile.apply {
                deleteRecursively()
                mkdirs()
            }
        }

        val scriptEnv by memoized {
            MainScriptEnv(rootFolder = rootFolder, id = "some-id").apply {
                mcVersion = "1.12.2"
                version = "1.0"
                icon = rootFolder.resolve("icon.png")
                authors = listOf("dude", "and", "friends")
                // TODO: type = {recommended, latest} | buildnumber, make sealed class ?
                modloader {
                    forge(Forge.mc1_12_2_recommended)
                }
                root<NestedEntry.Curse> {
                    releaseTypes = setOf(FileType.Release, FileType.Beta)

                    // TODO: use type URL ?
                    it.list {
                        +(Mod.botania)

                        +(Mod.rftools)

//                        withType<NestedEntry.Jenkins> {
//                            jenkinsUrl = "https://ci.elytradev.com/"
//                            side = Side.SERVER
//                        }.list {
//                            +"BTFU" job "elytra/BTFU/master"
//                        }

                        group {
                            side = Side.BOTH
                            optional {
                                selected = false
                            }
                        }.list {
                            +(Mod.laggoggles) {
                                description =
                                    "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                            }

                            +(Mod.sampler) {
                                description =
                                    "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                            }

                            +(Mod.openeye) {
                                description =
                                    "Automatically collects and submits crash reports. Enable if asked or wish to help sort issues with the pack."
                            }
                        }

//                    include("other.kts")
                    }
                }
            }
        }

        val targetFilename = "roundtrip.lock.json"
        val modpack by memoized {
            runBlocking { scriptEnv.pack.flatten() }
        }
        val lockpack by memoized {
            runBlocking {
                Builder.build(Stopwatch("RoundTrip-build"), modpack, "roundtrip", targetFilename)
            }
        }
        it("parse lockpack") {
            val jsonText = json.stringify(LockPack.serializer(), lockpack)
            println(jsonText)
            val parsedLockPack = json.parse(LockPack.serializer(), jsonText)
            parsedLockPack.rootFolder = rootFolder
            println(lockpack)
            println(parsedLockPack)
            assertEquals(lockpack, parsedLockPack)
        }
    }
})
